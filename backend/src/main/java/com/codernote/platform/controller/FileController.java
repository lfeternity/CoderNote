package com.codernote.platform.controller;

import com.codernote.platform.common.ApiResponse;
import com.codernote.platform.dto.question.QuestionAttachmentVO;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.security.AuthContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/file")
public class FileController {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "bmp",
            "pdf", "doc", "docx", "md", "markdown", "txt", "rtf",
            "ppt", "pptx", "xls", "xlsx"
    );

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp");
    private static final Set<String> BIZ_TYPES = Set.of("question", "material", "note", "tag");

    private final Path uploadBaseDir;

    public FileController(@Value("${app.upload.base-dir:uploads}") String uploadBaseDir) {
        this.uploadBaseDir = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
    }

    @PostMapping("/upload")
    public ApiResponse<QuestionAttachmentVO> upload(@RequestParam("file") MultipartFile file,
                                                     @RequestParam(value = "bizType", defaultValue = "question") String bizType,
                                                     HttpServletRequest request) throws IOException {
        Long userId = AuthContext.getRequiredUserId(request);
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "Empty file");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(400, "File too large (max 20MB)");
        }

        String normalizedBizType = normalizeBizType(bizType);
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        if (!StringUtils.hasText(originalName)) {
            throw new BizException(400, "Invalid file name");
        }

        String ext = fileExtension(originalName);
        if (!StringUtils.hasText(ext) || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new BizException(400, "Unsupported file type");
        }
        String normalizedExt = ext.toLowerCase(Locale.ROOT);

        byte[] fileBytes = file.getBytes();
        validateMagic(normalizedExt, fileBytes);

        String folder = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path userDir = uploadBaseDir.resolve(normalizedBizType).resolve(String.valueOf(userId)).resolve(folder).normalize();
        Files.createDirectories(userDir);

        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + normalizedExt;
        Path target = userDir.resolve(storedName).normalize();
        if (!target.startsWith(uploadBaseDir)) {
            throw new BizException(400, "Invalid target path");
        }

        Files.copy(new java.io.ByteArrayInputStream(fileBytes), target, StandardCopyOption.REPLACE_EXISTING);

        String relativePath = normalizedBizType + "/" + userId + "/" + folder + "/" + storedName;

        QuestionAttachmentVO vo = new QuestionAttachmentVO();
        vo.setFileName(originalName);
        vo.setPath(relativePath);
        vo.setContentType(file.getContentType());
        vo.setSize(file.getSize());
        vo.setImage(isImage(ext, file.getContentType()));

        String encodedPath = URLEncoder.encode(relativePath, StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(originalName, StandardCharsets.UTF_8);
        String previewUrl = "/api/v1/file/download?path=" + encodedPath + "&name=" + encodedName;
        vo.setPreviewUrl(previewUrl);
        vo.setDownloadUrl(previewUrl + "&download=true");

        return ApiResponse.success("Uploaded", vo);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam("path") String relativePath,
                                             @RequestParam(value = "name", required = false) String fileName,
                                             @RequestParam(value = "download", defaultValue = "false") boolean download,
                                             HttpServletRequest request) throws IOException {
        Long userId = AuthContext.getRequiredUserId(request);
        Path target = resolveAndValidatePath(relativePath, userId);
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new BizException(404, "File not found");
        }

        Resource resource = toResource(target);
        String contentType = Files.probeContentType(target);
        if (!StringUtils.hasText(contentType)) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        String finalFileName = StringUtils.hasText(fileName) ? fileName : target.getFileName().toString();
        String ext = fileExtension(finalFileName);
        boolean image = isImage(ext, contentType);
        boolean forceAttachment = download || !image;

        ContentDisposition disposition = forceAttachment
                ? ContentDisposition.attachment().filename(finalFileName, StandardCharsets.UTF_8).build()
                : ContentDisposition.inline().filename(finalFileName, StandardCharsets.UTF_8).build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    private String normalizeBizType(String bizType) {
        String normalized = StringUtils.hasText(bizType) ? bizType.trim().toLowerCase(Locale.ROOT) : "question";
        if (!BIZ_TYPES.contains(normalized)) {
            throw new BizException(400, "Invalid biz type");
        }
        return normalized;
    }

    private Path resolveAndValidatePath(String relativePath, Long userId) {
        String cleaned = StringUtils.cleanPath(relativePath).replace('\\', '/');
        if (!StringUtils.hasText(cleaned) || cleaned.contains("..") || cleaned.startsWith("/")) {
            throw new BizException(400, "Invalid file path");
        }

        String questionPrefix = "question/" + userId + "/";
        String materialPrefix = "material/" + userId + "/";
        String notePrefix = "note/" + userId + "/";
        String tagPrefix = "tag/" + userId + "/";
        if (!cleaned.startsWith(questionPrefix)
                && !cleaned.startsWith(materialPrefix)
                && !cleaned.startsWith(notePrefix)
                && !cleaned.startsWith(tagPrefix)) {
            throw new BizException(403, "No permission for this file");
        }

        Path target = uploadBaseDir.resolve(cleaned).normalize();
        if (!target.startsWith(uploadBaseDir)) {
            throw new BizException(400, "Invalid file path");
        }
        return target;
    }

    private Resource toResource(Path path) {
        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new BizException(500, "Invalid file resource");
        }
    }

    private String fileExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1);
    }

    private boolean isImage(String ext, String contentType) {
        if (StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        return StringUtils.hasText(ext) && IMAGE_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }

    private void validateMagic(String ext, byte[] bytes) {
        if (!StringUtils.hasText(ext) || bytes == null || bytes.length == 0) {
            throw new BizException(400, "Invalid file content");
        }
        String normalizedExt = ext.toLowerCase(Locale.ROOT);
        boolean mismatch = ("png".equals(normalizedExt) && !hasPrefix(bytes, 0x89, 0x50, 0x4E, 0x47))
                || (("jpg".equals(normalizedExt) || "jpeg".equals(normalizedExt)) && !hasPrefix(bytes, 0xFF, 0xD8, 0xFF))
                || ("gif".equals(normalizedExt) && !hasPrefixAscii(bytes, "GIF8"))
                || ("webp".equals(normalizedExt) && !(hasPrefixAscii(bytes, "RIFF") && hasAsciiAt(bytes, 8, "WEBP")))
                || ("bmp".equals(normalizedExt) && !hasPrefix(bytes, 0x42, 0x4D))
                || ("pdf".equals(normalizedExt) && !containsAsciiWithin(bytes, "%PDF-", 1024))
                || ("rtf".equals(normalizedExt) && !containsAsciiWithin(bytes, "{\\rtf", 256))
                || (("doc".equals(normalizedExt) || "xls".equals(normalizedExt) || "ppt".equals(normalizedExt))
                && !(hasPrefix(bytes, 0xD0, 0xCF, 0x11, 0xE0) || hasPrefix(bytes, 0x50, 0x4B)))
                || (("docx".equals(normalizedExt) || "xlsx".equals(normalizedExt) || "pptx".equals(normalizedExt))
                && !hasPrefix(bytes, 0x50, 0x4B));
        if (mismatch) {
            throw new BizException(400, "File content does not match extension");
        }
    }

    private boolean hasPrefix(byte[] bytes, int... magic) {
        if (bytes.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if ((bytes[i] & 0xFF) != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPrefixAscii(byte[] bytes, String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        byte[] expected = text.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (bytes[i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean hasAsciiAt(byte[] bytes, int offset, String text) {
        if (!StringUtils.hasText(text) || offset < 0) {
            return false;
        }
        byte[] expected = text.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (bytes[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean containsAsciiWithin(byte[] bytes, String text, int maxScanLength) {
        if (!StringUtils.hasText(text) || bytes == null || bytes.length == 0 || maxScanLength <= 0) {
            return false;
        }
        byte[] expected = text.getBytes(StandardCharsets.US_ASCII);
        int scanLimit = Math.min(bytes.length, maxScanLength);
        if (scanLimit < expected.length) {
            return false;
        }
        for (int i = 0; i <= scanLimit - expected.length; i++) {
            boolean matched = true;
            for (int j = 0; j < expected.length; j++) {
                if (bytes[i + j] != expected[j]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }
}
