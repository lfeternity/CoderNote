package com.codernote.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codernote.platform.common.Constants;
import com.codernote.platform.dto.user.CaptchaVO;
import com.codernote.platform.dto.user.ChangePasswordRequest;
import com.codernote.platform.dto.user.LoginRequest;
import com.codernote.platform.dto.user.RegisterRequest;
import com.codernote.platform.dto.user.UpdateProfileRequest;
import com.codernote.platform.dto.user.UserProfileVO;
import com.codernote.platform.entity.User;
import com.codernote.platform.entity.UserOauthBind;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.mapper.UserMapper;
import com.codernote.platform.mapper.UserOauthBindMapper;
import com.codernote.platform.service.AvatarService;
import com.codernote.platform.service.UserService;
import com.codernote.platform.util.PasswordUtil;
import org.springframework.dao.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    private static final String SESSION_CAPTCHA_CODE = "AUTH_CAPTCHA_CODE";
    private static final String SESSION_CAPTCHA_EXPIRE_AT = "AUTH_CAPTCHA_EXPIRE_AT";
    private static final long CAPTCHA_EXPIRE_MILLIS = 5 * 60 * 1000L;
    private static final int CAPTCHA_WIDTH = 120;
    private static final int CAPTCHA_HEIGHT = 40;
    private static final int CAPTCHA_LENGTH = 4;
    private static final char[] CAPTCHA_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private static final int LOGIN_FAIL_LIMIT_COUNT = 5;
    private static final long LOGIN_FAIL_WINDOW_MILLIS = 10 * 60 * 1000L;
    private static final long LOGIN_BLOCK_MILLIS = 10 * 60 * 1000L;
    private static final String LOGIN_FAIL_KEY_PREFIX = "auth:login:fail:";
    private static final String LOGIN_BLOCK_KEY_PREFIX = "auth:login:block:";
    private static final long REDIS_KEY_NO_EXPIRE_TTL = -1L;

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final UserOauthBindMapper userOauthBindMapper;
    private final AvatarService avatarService;
    private final StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl(UserMapper userMapper,
                           UserOauthBindMapper userOauthBindMapper,
                           AvatarService avatarService,
                           StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.userOauthBindMapper = userOauthBindMapper;
        this.avatarService = avatarService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public CaptchaVO generateRegisterCaptcha(HttpServletRequest servletRequest) {
        String code = randomCaptchaCode();
        BufferedImage image = drawCaptchaImage(code);

        String base64;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            base64 = Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            throw new BizException(500, "Failed to generate captcha");
        }

        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(SESSION_CAPTCHA_CODE, code);
        session.setAttribute(SESSION_CAPTCHA_EXPIRE_AT, System.currentTimeMillis() + CAPTCHA_EXPIRE_MILLIS);

        CaptchaVO vo = new CaptchaVO();
        vo.setImageBase64(base64);
        vo.setExpireSeconds((int) (CAPTCHA_EXPIRE_MILLIS / 1000));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request, HttpServletRequest servletRequest) {
        verifyCaptchaRequired(request.getCaptchaCode(), servletRequest);

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BizException(400, "Passwords do not match");
        }
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getNickname, request.getNickname()));
        if (exists != null) {
            throw new BizException(400, "Nickname already exists");
        }
        User user = new User();
        user.setNickname(request.getNickname().trim());
        user.setStudentNo(StringUtils.hasText(request.getStudentNo()) ? request.getStudentNo().trim() : null);
        user.setMajor(request.getMajor().trim());
        user.setPassword(PasswordUtil.hashPassword(request.getPassword()));
        user.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BizException(400, "Nickname already exists");
        }
    }

    @Override
    public void login(LoginRequest request, HttpServletRequest servletRequest) {
        String throttleKey = buildLoginThrottleKey(request.getNickname(), servletRequest);
        long blockedRemainingMillis = getBlockedRemainingMillis(throttleKey);
        if (blockedRemainingMillis > 0) {
            throw buildLoginRateLimitException(blockedRemainingMillis);
        }

        verifyCaptchaRequired(request.getCaptchaCode(), servletRequest);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getNickname, request.getNickname()));
        if (user == null || !PasswordUtil.matchesPassword(request.getPassword(), user.getPassword())) {
            boolean blockedNow = recordLoginFailed(throttleKey);
            if (blockedNow) {
                throw buildLoginRateLimitException(getBlockedRemainingMillis(throttleKey));
            }
            throw new BizException(400, "Invalid nickname or password");
        }

        clearLoginFailRecord(throttleKey);
        upgradePasswordHashIfNeeded(user, request.getPassword());
        HttpSession session = rotateLoginSession(servletRequest);
        session.setAttribute(Constants.SESSION_USER_ID, user.getId());
        session.setAttribute(Constants.SESSION_USER_NICKNAME, user.getNickname());
    }

    @Override
    public void logout(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "User not found");
        }
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setStudentNo(user.getStudentNo());
        vo.setMajor(user.getMajor());
        vo.setRemark(user.getRemark());
        vo.setRegisterTime(user.getCreatedAt());
        String avatarUrl = avatarService.buildAvatarUrl(user.getId());
        if (!StringUtils.hasText(avatarUrl)) {
            UserOauthBind latestBind = userOauthBindMapper.selectOne(new LambdaQueryWrapper<UserOauthBind>()
                    .eq(UserOauthBind::getUserId, user.getId())
                    .isNotNull(UserOauthBind::getPlatformAvatarUrl)
                    .orderByDesc(UserOauthBind::getUpdatedAt)
                    .last("LIMIT 1"));
            if (latestBind != null && StringUtils.hasText(latestBind.getPlatformAvatarUrl())) {
                avatarUrl = latestBind.getPlatformAvatarUrl();
            }
        }
        vo.setAvatarUrl(avatarUrl);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "User not found");
        }
        user.setStudentNo(request.getStudentNo());
        user.setMajor(request.getMajor().trim());
        user.setRemark(request.getRemark());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request, HttpServletRequest servletRequest) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BizException(400, "New passwords do not match");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "User not found");
        }
        if (!PasswordUtil.matchesPassword(request.getOldPassword(), user.getPassword())) {
            throw new BizException(400, "Old password incorrect");
        }
        user.setPassword(PasswordUtil.hashPassword(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private void verifyCaptchaRequired(String captchaInput, HttpServletRequest servletRequest) {
        if (!StringUtils.hasText(captchaInput)) {
            throw new BizException(400, "Captcha is required");
        }

        HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            throw new BizException(400, "Captcha expired, please refresh");
        }

        Object codeObj = session.getAttribute(SESSION_CAPTCHA_CODE);
        Object expireAtObj = session.getAttribute(SESSION_CAPTCHA_EXPIRE_AT);
        session.removeAttribute(SESSION_CAPTCHA_CODE);
        session.removeAttribute(SESSION_CAPTCHA_EXPIRE_AT);

        if (!(codeObj instanceof String) || !(expireAtObj instanceof Long)) {
            throw new BizException(400, "Captcha expired, please refresh");
        }

        long expireAt = (Long) expireAtObj;
        if (System.currentTimeMillis() > expireAt) {
            throw new BizException(400, "Captcha expired, please refresh");
        }

        if (!((String) codeObj).equalsIgnoreCase(captchaInput.trim())) {
            throw new BizException(400, "Captcha code incorrect");
        }
    }

    private String randomCaptchaCode() {
        StringBuilder code = new StringBuilder(CAPTCHA_LENGTH);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            code.append(CAPTCHA_CHARS[random.nextInt(CAPTCHA_CHARS.length)]);
        }
        return code.toString();
    }

    private BufferedImage drawCaptchaImage(String code) {
        BufferedImage image = new BufferedImage(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(244, 248, 255));
            g.fillRect(0, 0, CAPTCHA_WIDTH, CAPTCHA_HEIGHT);

            for (int i = 0; i < 10; i++) {
                g.setColor(new Color(180 + randomInt(50), 200 + randomInt(40), 220 + randomInt(30)));
                int x1 = randomInt(CAPTCHA_WIDTH);
                int y1 = randomInt(CAPTCHA_HEIGHT);
                int x2 = randomInt(CAPTCHA_WIDTH);
                int y2 = randomInt(CAPTCHA_HEIGHT);
                g.drawLine(x1, y1, x2, y2);
            }

            g.setFont(new Font("Arial", Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(30 + randomInt(90), 64 + randomInt(70), 130 + randomInt(70)));
                int x = 12 + i * 26 + randomInt(4);
                int y = 28 + randomInt(6);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
            }

            for (int i = 0; i < 20; i++) {
                g.setColor(new Color(120 + randomInt(110), 150 + randomInt(90), 180 + randomInt(70)));
                g.fillOval(randomInt(CAPTCHA_WIDTH), randomInt(CAPTCHA_HEIGHT), 2, 2);
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    private int randomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    private String buildLoginThrottleKey(String nickname, HttpServletRequest servletRequest) {
        String normalizedNickname = StringUtils.hasText(nickname)
                ? nickname.trim().toLowerCase(Locale.ROOT)
                : "";
        return normalizedNickname + "|" + resolveClientIp(servletRequest);
    }

    private String resolveClientIp(HttpServletRequest servletRequest) {
        String xForwardedFor = servletRequest.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            int commaIndex = xForwardedFor.indexOf(',');
            return commaIndex > 0 ? xForwardedFor.substring(0, commaIndex).trim() : xForwardedFor.trim();
        }
        String realIp = servletRequest.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        String remoteAddr = servletRequest.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr.trim() : "unknown";
    }

    private long getBlockedRemainingMillis(String throttleKey) {
        String blockKey = loginBlockRedisKey(throttleKey);
        try {
            Long ttl = stringRedisTemplate.getExpire(blockKey, TimeUnit.MILLISECONDS);
            if (ttl == null || ttl <= 0) {
                return 0L;
            }
            return ttl;
        } catch (Exception ex) {
            log.warn("Failed to query login block ttl key={}", blockKey, ex);
            return 0L;
        }
    }

    private boolean recordLoginFailed(String throttleKey) {
        String blockKey = loginBlockRedisKey(throttleKey);
        String failKey = loginFailRedisKey(throttleKey);
        try {
            Long blockedTtl = stringRedisTemplate.getExpire(blockKey, TimeUnit.MILLISECONDS);
            if (blockedTtl != null && blockedTtl > 0) {
                return true;
            }
            // TTL = -1 means key exists but has no expiry, repair it in-place.
            if (Long.valueOf(REDIS_KEY_NO_EXPIRE_TTL).equals(blockedTtl)) {
                stringRedisTemplate.expire(blockKey, LOGIN_BLOCK_MILLIS, TimeUnit.MILLISECONDS);
                return true;
            }

            Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
            if (failCount == null) {
                return false;
            }
            Long failTtl = stringRedisTemplate.getExpire(failKey, TimeUnit.MILLISECONDS);
            if (failCount == 1L || failTtl == null || failTtl < 0) {
                stringRedisTemplate.expire(failKey, LOGIN_FAIL_WINDOW_MILLIS, TimeUnit.MILLISECONDS);
            }
            if (failCount >= LOGIN_FAIL_LIMIT_COUNT) {
                stringRedisTemplate.opsForValue().set(blockKey, "1", LOGIN_BLOCK_MILLIS, TimeUnit.MILLISECONDS);
                stringRedisTemplate.delete(failKey);
                return true;
            }
            return false;
        } catch (Exception ex) {
            // Degrade gracefully when Redis is unavailable.
            log.warn("Failed to record login throttle key={} blockKey={} failKey={}",
                    hashThrottleKey(throttleKey), blockKey, failKey, ex);
            return false;
        }
    }

    private void clearLoginFailRecord(String throttleKey) {
        try {
            stringRedisTemplate.delete(Arrays.asList(
                    loginFailRedisKey(throttleKey),
                    loginBlockRedisKey(throttleKey)
            ));
        } catch (Exception ex) {
            log.warn("Failed to clear login throttle key={}", hashThrottleKey(throttleKey), ex);
        }
    }

    private void upgradePasswordHashIfNeeded(User user, String rawPassword) {
        if (user == null || !PasswordUtil.needsUpgrade(user.getPassword())) {
            return;
        }
        user.setPassword(PasswordUtil.hashPassword(rawPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private String loginFailRedisKey(String throttleKey) {
        return LOGIN_FAIL_KEY_PREFIX + hashThrottleKey(throttleKey);
    }

    private String loginBlockRedisKey(String throttleKey) {
        return LOGIN_BLOCK_KEY_PREFIX + hashThrottleKey(throttleKey);
    }

    private String hashThrottleKey(String rawKey) {
        String normalized = rawKey == null ? "" : rawKey;
        return DigestUtils.md5DigestAsHex(normalized.getBytes(StandardCharsets.UTF_8));
    }

    private BizException buildLoginRateLimitException(long blockedRemainingMillis) {
        long safeRemaining = Math.max(1L, blockedRemainingMillis);
        long seconds = (safeRemaining + 999L) / 1000L;
        return new BizException(429, "Too many failed login attempts, please retry after " + seconds + " seconds");
    }

    private HttpSession rotateLoginSession(HttpServletRequest servletRequest) {
        HttpSession current = servletRequest.getSession(false);
        if (current == null) {
            return servletRequest.getSession(true);
        }
        try {
            servletRequest.changeSessionId();
            HttpSession rotated = servletRequest.getSession(false);
            if (rotated != null) {
                return rotated;
            }
        } catch (IllegalStateException ignore) {
            // fall through to invalidate + recreate.
        }
        current.invalidate();
        return servletRequest.getSession(true);
    }

}
