package com.codernote.platform.service.impl;

import com.codernote.platform.exception.BizException;
import com.codernote.platform.service.AvatarService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class AvatarServiceImpl implements AvatarService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String AVATAR_UPLOAD_KEY_PREFIX = "avatar:upload:daily:";
    private static final RedisScript<Long> AVATAR_UPLOAD_CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('GET', KEYS[1]);"
                    + "local currentNum = tonumber(current);"
                    + "if current and not currentNum then redis.call('DEL', KEYS[1]); currentNum = nil end;"
                    + "if currentNum and currentNum >= tonumber(ARGV[1]) then "
                    + "if redis.call('PTTL', KEYS[1]) < 0 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end; "
                    + "return -1 end;"
                    + "local next = redis.call('INCR', KEYS[1]);"
                    + "if redis.call('PTTL', KEYS[1]) < 0 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end;"
                    + "return next;",
            Long.class
    );

    private final Path avatarBaseDir;
    private final long maxFileSize;
    private final int dailyLimit;
    private final int outputSize;
    private final StringRedisTemplate stringRedisTemplate;

    public AvatarServiceImpl(@Value("${app.avatar.base-dir:uploads/avatar}") String avatarBaseDir,
                             @Value("${app.avatar.max-size-bytes:2097152}") long maxFileSize,
                             @Value("${app.avatar.daily-limit:10}") int dailyLimit,
                             @Value("${app.avatar.output-size:512}") int outputSize,
                             StringRedisTemplate stringRedisTemplate) {
        this.avatarBaseDir = Paths.get(avatarBaseDir).toAbsolutePath().normalize();
        this.maxFileSize = Math.max(1L, maxFileSize);
        this.dailyLimit = Math.max(1, dailyLimit);
        this.outputSize = Math.max(64, outputSize);
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void saveAvatar(Long userId, MultipartFile file) {
        if (userId == null) {
            throw new BizException(400, "Invalid user");
        }
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "Avatar file is required");
        }
        if (file.getSize() > maxFileSize) {
            throw new BizException(400, "Avatar file too large (max 2MB)");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        if (!StringUtils.hasText(originalName)) {
            throw new BizException(400, "Invalid avatar file name");
        }

        String ext = fileExtension(originalName).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BizException(400, "Only JPG/PNG/WEBP is supported");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BizException(500, "Failed to read avatar file");
        }

        MagicType magicType = detectMagicType(bytes);
        if (magicType == MagicType.UNKNOWN || !matchesExtension(magicType, ext)) {
            throw new BizException(400, "Invalid avatar file type");
        }

        BufferedImage sourceImage;
        try {
            sourceImage = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new BizException(400, "Failed to load image, please retry");
        }
        if (sourceImage == null || sourceImage.getWidth() <= 0 || sourceImage.getHeight() <= 0) {
            throw new BizException(400, "Failed to load image, please retry");
        }

        checkDailyUploadLimit(userId);

        Path target = resolveAvatarFile(userId);
        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            throw new BizException(500, "Failed to prepare avatar directory");
        }

        writeCompressedAvatar(sourceImage, target);
    }

    @Override
    public void resetAvatar(Long userId) {
        if (userId == null) {
            throw new BizException(400, "Invalid user");
        }

        Path target = resolveAvatarFile(userId);
        try {
            Files.deleteIfExists(target);
            Path userDir = target.getParent();
            if (userDir != null && Files.isDirectory(userDir)) {
                try (var stream = Files.list(userDir)) {
                    if (!stream.findAny().isPresent()) {
                        Files.deleteIfExists(userDir);
                    }
                }
            }
        } catch (IOException e) {
            throw new BizException(500, "Failed to reset avatar");
        }
    }

    @Override
    public String buildAvatarUrl(Long userId) {
        if (userId == null) {
            return null;
        }
        Path target = resolveAvatarFile(userId);
        if (!Files.isRegularFile(target)) {
            return null;
        }
        try {
            long version = Files.getLastModifiedTime(target).toMillis();
            return "/api/v1/public/avatar/" + userId + "?v=" + version;
        } catch (IOException e) {
            return "/api/v1/public/avatar/" + userId;
        }
    }

    @Override
    public ResponseEntity<Resource> readAvatar(Long userId) {
        if (userId == null) {
            throw new BizException(400, "Invalid user");
        }

        Path target = resolveAvatarFile(userId);
        if (!Files.isRegularFile(target)) {
            throw new BizException(404, "Avatar not found");
        }

        Resource resource = toResource(target);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }

    private void writeCompressedAvatar(BufferedImage sourceImage, Path target) {
        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();
        int side = Math.min(sourceWidth, sourceHeight);
        int x = (sourceWidth - side) / 2;
        int y = (sourceHeight - side) / 2;

        BufferedImage outputImage = new BufferedImage(outputSize, outputSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = outputImage.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, outputSize, outputSize);
            g.drawImage(sourceImage,
                    0, 0, outputSize, outputSize,
                    x, y, x + side, y + side,
                    null);
        } finally {
            g.dispose();
        }

        Path temp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        ImageWriter writer = null;
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                throw new BizException(500, "JPEG writer unavailable");
            }
            writer = writers.next();

            try (OutputStream out = Files.newOutputStream(temp,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
                 ImageOutputStream imageOut = ImageIO.createImageOutputStream(out)) {

                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                if (writeParam.canWriteCompressed()) {
                    writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    writeParam.setCompressionQuality(0.86f);
                }

                writer.setOutput(imageOut);
                writer.write(null, new IIOImage(outputImage, null, null), writeParam);
            }

            try {
                Files.move(temp, target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignore) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BizException(500, "Failed to save avatar");
        } finally {
            if (writer != null) {
                writer.dispose();
            }
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignore) {
                // no-op
            }
        }
    }

    private void checkDailyUploadLimit(Long userId) {
        ZonedDateTime now = ZonedDateTime.now(SHANGHAI_ZONE);
        ZonedDateTime nextDayStart = now.plusDays(1).truncatedTo(ChronoUnit.DAYS);
        long ttlMillis = Math.max(1000L, ChronoUnit.MILLIS.between(now, nextDayStart));
        String key = AVATAR_UPLOAD_KEY_PREFIX + now.format(DAY_KEY_FORMATTER) + ":" + userId;

        Long usedCount = stringRedisTemplate.execute(
                AVATAR_UPLOAD_CONSUME_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(dailyLimit),
                String.valueOf(ttlMillis)
        );
        if (usedCount == null) {
            throw new BizException(500, "Avatar upload service unavailable, please retry later");
        }
        if (usedCount < 0) {
            throw new BizException(429, "Avatar upload limit reached today");
        }
    }

    private Path resolveAvatarFile(Long userId) {
        Path file = avatarBaseDir
                .resolve(String.valueOf(userId))
                .resolve("avatar.jpg")
                .normalize();
        if (!file.startsWith(avatarBaseDir)) {
            throw new BizException(400, "Invalid avatar path");
        }
        return file;
    }

    private Resource toResource(Path target) {
        try {
            return new UrlResource(target.toUri());
        } catch (MalformedURLException e) {
            throw new BizException(500, "Invalid avatar resource");
        }
    }

    private String fileExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1);
    }

    private boolean matchesExtension(MagicType magicType, String ext) {
        if (magicType == MagicType.JPEG) {
            return "jpg".equals(ext) || "jpeg".equals(ext);
        }
        if (magicType == MagicType.PNG) {
            return "png".equals(ext);
        }
        if (magicType == MagicType.WEBP) {
            return "webp".equals(ext);
        }
        return false;
    }

    private MagicType detectMagicType(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return MagicType.UNKNOWN;
        }

        if ((bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return MagicType.JPEG;
        }

        if ((bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return MagicType.PNG;
        }

        if (bytes[0] == 0x52
                && bytes[1] == 0x49
                && bytes[2] == 0x46
                && bytes[3] == 0x46
                && bytes[8] == 0x57
                && bytes[9] == 0x45
                && bytes[10] == 0x42
                && bytes[11] == 0x50) {
            return MagicType.WEBP;
        }

        return MagicType.UNKNOWN;
    }

    private enum MagicType {
        JPEG,
        PNG,
        WEBP,
        UNKNOWN
    }
}
