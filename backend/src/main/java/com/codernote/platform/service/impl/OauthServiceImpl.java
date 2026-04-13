package com.codernote.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codernote.platform.common.Constants;
import com.codernote.platform.config.OauthProperties;
import com.codernote.platform.dto.user.OauthAutoRegisterRequest;
import com.codernote.platform.dto.user.OauthBindExistingRequest;
import com.codernote.platform.dto.user.OauthBindingStatusVO;
import com.codernote.platform.dto.user.OauthPendingBindVO;
import com.codernote.platform.entity.User;
import com.codernote.platform.entity.UserOauthBind;
import com.codernote.platform.enums.OauthPlatform;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.mapper.UserMapper;
import com.codernote.platform.mapper.UserOauthBindMapper;
import com.codernote.platform.service.OauthService;
import com.codernote.platform.util.PasswordUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OauthServiceImpl implements OauthService {

    private static final String SESSION_OAUTH_STATE_MAP = "OAUTH_STATE_MAP";
    private static final String SESSION_OAUTH_PENDING_MAP = "OAUTH_PENDING_MAP";
    private static final String DEFAULT_MAJOR = "Computer Science";
    private static final String OAUTH_MESSAGE_QUERY_KEY = "oauthMessage";
    private static final String BIND_TOKEN_QUERY_KEY = "bindToken";

    private static final String GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_INFO_URL = "https://api.github.com/user";

    private static final String QQ_AUTHORIZE_URL = "https://graph.qq.com/oauth2.0/authorize";
    private static final String QQ_TOKEN_URL = "https://graph.qq.com/oauth2.0/token";
    private static final String QQ_OPEN_ID_URL = "https://graph.qq.com/oauth2.0/me";
    private static final String QQ_USER_INFO_URL = "https://graph.qq.com/user/get_user_info";

    private static final String WECHAT_AUTHORIZE_URL = "https://open.weixin.qq.com/connect/qrconnect";
    private static final String WECHAT_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String WECHAT_USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo";

    private final UserMapper userMapper;
    private final UserOauthBindMapper userOauthBindMapper;
    private final OauthProperties oauthProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OauthServiceImpl(UserMapper userMapper,
                            UserOauthBindMapper userOauthBindMapper,
                            OauthProperties oauthProperties,
                            ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.userOauthBindMapper = userOauthBindMapper;
        this.oauthProperties = oauthProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String buildLoginAuthorizeUrl(String platformCode, String clientBaseUrl, String redirectPath, HttpServletRequest request) {
        OauthPlatform platform = requirePlatform(platformCode);
        String frontend = normalizeClientBaseUrl(clientBaseUrl);
        OauthProperties.PlatformConfig config = oauthProperties.findPlatformConfig(platform.getCode());
        if (!isConfigured(config)) {
            return buildLoginPageUrl(frontend, platform.getDisplayName() + " 登录暂未配置");
        }
        clearOauthContext(request.getSession(true));
        String state = randomToken();
        OauthStateContext ctx = new OauthStateContext();
        ctx.scene = OauthScene.LOGIN;
        ctx.platform = platform.getCode();
        ctx.userId = null;
        ctx.clientBaseUrl = frontend;
        ctx.redirectPath = sanitizeRedirectPath(redirectPath);
        ctx.expireAtMillis = nowMillis() + safeExpireMillis(oauthProperties.getStateExpireSeconds(), 300);
        stateMap(request.getSession(true), true).put(state, ctx);
        return buildAuthorizeUrl(platform, config, state, request);
    }

    @Override
    public String buildBindAuthorizeUrl(String platformCode, String clientBaseUrl, Long userId, HttpServletRequest request) {
        OauthPlatform platform = requirePlatform(platformCode);
        String frontend = normalizeClientBaseUrl(clientBaseUrl);
        if (userId == null) {
            return buildLoginPageUrl(frontend, "请先登录后再绑定账号");
        }
        OauthProperties.PlatformConfig config = oauthProperties.findPlatformConfig(platform.getCode());
        if (!isConfigured(config)) {
            return appendQuery(frontend + "/user/center", OAUTH_MESSAGE_QUERY_KEY, platform.getDisplayName() + " 绑定暂未配置");
        }
        clearOauthContext(request.getSession(true));
        String state = randomToken();
        OauthStateContext ctx = new OauthStateContext();
        ctx.scene = OauthScene.BIND;
        ctx.platform = platform.getCode();
        ctx.userId = userId;
        ctx.clientBaseUrl = frontend;
        ctx.redirectPath = "/user/center";
        ctx.expireAtMillis = nowMillis() + safeExpireMillis(oauthProperties.getStateExpireSeconds(), 300);
        stateMap(request.getSession(true), true).put(state, ctx);
        return buildAuthorizeUrl(platform, config, state, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(String platformCode, String state, String code, String error, HttpServletRequest request) {
        OauthPlatform platform = requirePlatform(platformCode);
        OauthStateContext ctx = consumeStateContext(request, state);
        if (ctx == null) {
            return buildLoginPageUrl(normalizeClientBaseUrl(null), "授权状态已失效，请重试");
        }
        if (!platform.getCode().equals(ctx.platform)) {
            return buildFallbackByScene(ctx, "授权平台不匹配，请重新发起授权");
        }
        if ("access_denied".equalsIgnoreCase(trimToEmpty(error))) {
            return buildFallbackByScene(ctx, "授权已取消");
        }
        if (!StringUtils.hasText(code)) {
            return buildFallbackByScene(ctx, "授权信息获取失败，请重试");
        }

        OauthProperties.PlatformConfig config = oauthProperties.findPlatformConfig(platform.getCode());
        if (!isConfigured(config)) {
            return buildFallbackByScene(ctx, platform.getDisplayName() + " 登录暂未配置");
        }

        OauthProfile profile;
        try {
            profile = requestOauthProfile(platform, config, code.trim(), request);
        } catch (BizException ex) {
            return buildFallbackByScene(ctx, ex.getMessage());
        }
        if (profile == null || !StringUtils.hasText(profile.openId)) {
            return buildFallbackByScene(ctx, "授权信息获取失败，请重试");
        }

        try {
            if (ctx.scene == OauthScene.BIND) {
                return handleBindCallback(ctx, profile, request);
            }
            return handleLoginCallback(ctx, platform, profile, request);
        } catch (BizException ex) {
            return buildFallbackByScene(ctx, ex.getMessage());
        }
    }

    @Override
    public OauthPendingBindVO getPendingBindInfo(String bindToken, HttpServletRequest request) {
        PendingBindContext ctx = getPendingContext(request, bindToken, false);
        OauthPlatform platform = requirePlatform(ctx.platform);
        OauthPendingBindVO vo = new OauthPendingBindVO();
        vo.setPlatform(platform.getCode());
        vo.setPlatformName(platform.getDisplayName());
        vo.setNickname(ctx.nickname);
        vo.setAvatarUrl(ctx.avatarUrl);
        vo.setRedirectPath(ctx.redirectPath);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindExistingAccount(OauthBindExistingRequest request, HttpServletRequest servletRequest) {
        PendingBindContext ctx = getPendingContext(servletRequest, request.getBindToken(), false);
        String rawPassword = trimToEmpty(request.getPassword());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getNickname, trimToEmpty(request.getNickname())));
        if (user == null || !PasswordUtil.matchesPassword(rawPassword, user.getPassword())) {
            throw new BizException(400, "账号或密码错误");
        }
        upgradePasswordHashIfNeeded(user, rawPassword);
        upsertBinding(user.getId(), ctx);
        removePendingContext(servletRequest, request.getBindToken());
        setLoginSession(servletRequest, user.getId(), user.getNickname());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoRegisterAndLogin(OauthAutoRegisterRequest request, HttpServletRequest servletRequest) {
        PendingBindContext ctx = getPendingContext(servletRequest, request.getBindToken(), false);
        String preferred = StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : ctx.nickname;
        String registerNickname = nextAvailableNickname(normalizeNickname(preferred));
        User user = new User();
        user.setNickname(registerNickname);
        user.setMajor(DEFAULT_MAJOR);
        user.setPassword(PasswordUtil.hashPassword(randomPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        upsertBinding(user.getId(), ctx);
        removePendingContext(servletRequest, request.getBindToken());
        setLoginSession(servletRequest, user.getId(), user.getNickname());
    }

    @Override
    public List<OauthBindingStatusVO> listBindings(Long userId) {
        List<UserOauthBind> list = userOauthBindMapper.selectList(new LambdaQueryWrapper<UserOauthBind>()
                .eq(UserOauthBind::getUserId, userId));
        Map<String, UserOauthBind> boundMap = list.stream()
                .collect(Collectors.toMap(UserOauthBind::getPlatform, item -> item, (left, right) -> right, LinkedHashMap::new));
        List<OauthBindingStatusVO> result = new ArrayList<>();
        for (OauthPlatform platform : OauthPlatform.values()) {
            UserOauthBind bind = boundMap.get(platform.getCode());
            OauthBindingStatusVO vo = new OauthBindingStatusVO();
            vo.setPlatform(platform.getCode());
            vo.setPlatformName(platform.getDisplayName());
            vo.setBrandColor(platform.getBrandColor());
            vo.setBound(bind != null);
            vo.setPlatformNickname(bind == null ? null : bind.getPlatformNickname());
            vo.setBindTime(bind == null ? null : bind.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long userId, String platformCode) {
        OauthPlatform platform = requirePlatform(platformCode);
        userOauthBindMapper.delete(new LambdaQueryWrapper<UserOauthBind>()
                .eq(UserOauthBind::getUserId, userId)
                .eq(UserOauthBind::getPlatform, platform.getCode()));
    }

    private String handleBindCallback(OauthStateContext ctx, OauthProfile profile, HttpServletRequest request) {
        if (ctx.userId == null) {
            return buildLoginPageUrl(ctx.clientBaseUrl, "登录状态已失效，请先登录后再绑定");
        }
        User user = userMapper.selectById(ctx.userId);
        if (user == null) {
            return buildLoginPageUrl(ctx.clientBaseUrl, "当前用户不存在，请重新登录");
        }
        PendingBindContext pending = toPendingContext(ctx, profile);
        upsertBinding(user.getId(), pending);
        setLoginSession(request, user.getId(), user.getNickname());
        return appendQuery(ctx.clientBaseUrl + "/user/center", OAUTH_MESSAGE_QUERY_KEY, "绑定成功");
    }

    private String handleLoginCallback(OauthStateContext ctx, OauthPlatform platform, OauthProfile profile, HttpServletRequest request) {
        UserOauthBind bind = userOauthBindMapper.selectOne(new LambdaQueryWrapper<UserOauthBind>()
                .eq(UserOauthBind::getPlatform, platform.getCode())
                .eq(UserOauthBind::getOpenId, profile.openId));
        if (bind != null) {
            User user = userMapper.selectById(bind.getUserId());
            if (user == null) {
                throw new BizException(404, "绑定账号不存在，请联系管理员");
            }
            refreshBindingProfile(bind, profile);
            setLoginSession(request, user.getId(), user.getNickname());
            return appendQuery(ctx.clientBaseUrl + sanitizeRedirectPath(ctx.redirectPath), "oauthLoggedIn", "1");
        }
        String bindToken = randomToken();
        PendingBindContext pending = toPendingContext(ctx, profile);
        pending.expireAtMillis = nowMillis() + safeExpireMillis(oauthProperties.getPendingExpireSeconds(), 600);
        pendingMap(request.getSession(true), true).put(bindToken, pending);
        return appendQuery(ctx.clientBaseUrl + "/oauth/bind", BIND_TOKEN_QUERY_KEY, bindToken);
    }

    private void upsertBinding(Long userId, PendingBindContext pending) {
        OauthPlatform platform = requirePlatform(pending.platform);
        UserOauthBind bindByOpenId = userOauthBindMapper.selectOne(new LambdaQueryWrapper<UserOauthBind>()
                .eq(UserOauthBind::getPlatform, platform.getCode())
                .eq(UserOauthBind::getOpenId, pending.openId));
        if (bindByOpenId != null && !Objects.equals(bindByOpenId.getUserId(), userId)) {
            throw new BizException(400, "该账号已绑定其他用户，请更换账号");
        }
        UserOauthBind bindByUserPlatform = userOauthBindMapper.selectOne(new LambdaQueryWrapper<UserOauthBind>()
                .eq(UserOauthBind::getUserId, userId)
                .eq(UserOauthBind::getPlatform, platform.getCode()));
        if (bindByUserPlatform != null && !Objects.equals(bindByUserPlatform.getOpenId(), pending.openId)) {
            throw new BizException(400, "当前账号已绑定其他 " + platform.getDisplayName() + " 账号，请先解绑");
        }
        LocalDateTime now = LocalDateTime.now();
        if (bindByOpenId != null) {
            bindByOpenId.setUnionId(pending.unionId);
            bindByOpenId.setPlatformNickname(pending.nickname);
            bindByOpenId.setPlatformAvatarUrl(pending.avatarUrl);
            bindByOpenId.setUpdatedAt(now);
            userOauthBindMapper.updateById(bindByOpenId);
            return;
        }
        UserOauthBind bind = new UserOauthBind();
        bind.setUserId(userId);
        bind.setPlatform(platform.getCode());
        bind.setOpenId(pending.openId);
        bind.setUnionId(pending.unionId);
        bind.setPlatformNickname(pending.nickname);
        bind.setPlatformAvatarUrl(pending.avatarUrl);
        bind.setCreatedAt(now);
        bind.setUpdatedAt(now);
        userOauthBindMapper.insert(bind);
    }

    private void refreshBindingProfile(UserOauthBind bind, OauthProfile profile) {
        bind.setUnionId(profile.unionId);
        bind.setPlatformNickname(profile.nickname);
        bind.setPlatformAvatarUrl(profile.avatarUrl);
        bind.setUpdatedAt(LocalDateTime.now());
        userOauthBindMapper.updateById(bind);
    }

    private OauthProfile requestOauthProfile(OauthPlatform platform,
                                             OauthProperties.PlatformConfig config,
                                             String code,
                                             HttpServletRequest request) {
        if (platform == OauthPlatform.GITHUB) {
            return requestGithubProfile(config, code, buildCallbackUrl(platform, request));
        }
        if (platform == OauthPlatform.QQ) {
            return requestQqProfile(config, code, buildCallbackUrl(platform, request));
        }
        if (platform == OauthPlatform.WECHAT) {
            return requestWechatProfile(config, code);
        }
        throw new BizException(400, "不支持的登录平台");
    }

    private OauthProfile requestGithubProfile(OauthProperties.PlatformConfig config, String code, String callbackUrl) {
        String tokenBody = httpPostForm(GITHUB_TOKEN_URL, mapOf(
                "client_id", trimToEmpty(config.getClientId()),
                "client_secret", trimToEmpty(config.getClientSecret()),
                "code", code,
                "redirect_uri", callbackUrl
        ), mapOf("Accept", "application/json", "User-Agent", "CoderNote-OAuth"));
        JsonNode tokenJson = parseJson(tokenBody);
        String accessToken = text(tokenJson, "access_token");
        if (!StringUtils.hasText(accessToken)) throw new BizException(400, "GitHub access token 获取失败");

        JsonNode user = parseJson(httpGet(GITHUB_USER_INFO_URL, mapOf(
                "Authorization", "Bearer " + accessToken,
                "Accept", "application/json",
                "User-Agent", "CoderNote-OAuth"
        )));
        String openId = text(user, "id");
        if (!StringUtils.hasText(openId)) throw new BizException(400, "GitHub 用户标识获取失败");
        OauthProfile p = new OauthProfile();
        p.platform = OauthPlatform.GITHUB.getCode();
        p.openId = openId;
        p.nickname = firstNonBlank(text(user, "name"), text(user, "login"), "GitHub用户");
        p.avatarUrl = text(user, "avatar_url");
        return p;
    }

    private OauthProfile requestQqProfile(OauthProperties.PlatformConfig config, String code, String callbackUrl) {
        String tokenUrl = UriComponentsBuilder.fromHttpUrl(QQ_TOKEN_URL)
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", trimToEmpty(config.getClientId()))
                .queryParam("client_secret", trimToEmpty(config.getClientSecret()))
                .queryParam("code", code)
                .queryParam("redirect_uri", callbackUrl)
                .build(true).toUriString();
        Map<String, String> tokenMap = parseQueryString(httpGet(tokenUrl, null));
        String accessToken = tokenMap.get("access_token");
        if (!StringUtils.hasText(accessToken)) throw new BizException(400, "QQ access token 获取失败");

        String meUrl = UriComponentsBuilder.fromHttpUrl(QQ_OPEN_ID_URL).queryParam("access_token", accessToken).build(true).toUriString();
        JsonNode me = parseQqCallbackJson(httpGet(meUrl, null));
        String openId = text(me, "openid");
        if (!StringUtils.hasText(openId)) throw new BizException(400, "QQ openId 获取失败");

        String infoUrl = UriComponentsBuilder.fromHttpUrl(QQ_USER_INFO_URL)
                .queryParam("access_token", accessToken)
                .queryParam("oauth_consumer_key", trimToEmpty(config.getClientId()))
                .queryParam("openid", openId)
                .build(true).toUriString();
        JsonNode user = parseJson(httpGet(infoUrl, null));
        Integer ret = intValue(user, "ret");
        if (ret != null && ret != 0) throw new BizException(400, firstNonBlank(text(user, "msg"), "QQ 用户信息获取失败"));

        OauthProfile p = new OauthProfile();
        p.platform = OauthPlatform.QQ.getCode();
        p.openId = openId;
        p.nickname = firstNonBlank(text(user, "nickname"), "QQ用户");
        p.avatarUrl = firstNonBlank(text(user, "figureurl_qq_2"), text(user, "figureurl_2"), text(user, "figureurl"));
        return p;
    }

    private OauthProfile requestWechatProfile(OauthProperties.PlatformConfig config, String code) {
        String tokenUrl = UriComponentsBuilder.fromHttpUrl(WECHAT_TOKEN_URL)
                .queryParam("appid", trimToEmpty(config.getClientId()))
                .queryParam("secret", trimToEmpty(config.getClientSecret()))
                .queryParam("code", code)
                .queryParam("grant_type", "authorization_code")
                .build(true).toUriString();
        JsonNode token = parseJson(httpGet(tokenUrl, null));
        Integer err = intValue(token, "errcode");
        if (err != null && err != 0) throw new BizException(400, firstNonBlank(text(token, "errmsg"), "微信授权失败"));
        String accessToken = text(token, "access_token");
        String openId = text(token, "openid");
        if (!StringUtils.hasText(accessToken) || !StringUtils.hasText(openId)) throw new BizException(400, "微信授权信息获取失败");

        String infoUrl = UriComponentsBuilder.fromHttpUrl(WECHAT_USER_INFO_URL)
                .queryParam("access_token", accessToken)
                .queryParam("openid", openId)
                .queryParam("lang", "zh_CN")
                .build(true).toUriString();
        JsonNode user = parseJson(httpGet(infoUrl, null));
        Integer uErr = intValue(user, "errcode");
        if (uErr != null && uErr != 0) throw new BizException(400, firstNonBlank(text(user, "errmsg"), "微信用户信息获取失败"));

        OauthProfile p = new OauthProfile();
        p.platform = OauthPlatform.WECHAT.getCode();
        p.openId = openId;
        p.unionId = firstNonBlank(text(user, "unionid"), text(token, "unionid"));
        p.nickname = firstNonBlank(text(user, "nickname"), "微信用户");
        p.avatarUrl = text(user, "headimgurl");
        return p;
    }

    private String buildAuthorizeUrl(OauthPlatform platform, OauthProperties.PlatformConfig config, String state, HttpServletRequest request) {
        String callbackUrl = buildCallbackUrl(platform, request);
        if (platform == OauthPlatform.GITHUB) {
            return UriComponentsBuilder.fromHttpUrl(GITHUB_AUTHORIZE_URL)
                    .queryParam("client_id", trimToEmpty(config.getClientId()))
                    .queryParam("redirect_uri", callbackUrl)
                    .queryParam("scope", "read:user user:email")
                    .queryParam("state", state)
                    .build(true).toUriString();
        }
        if (platform == OauthPlatform.QQ) {
            return UriComponentsBuilder.fromHttpUrl(QQ_AUTHORIZE_URL)
                    .queryParam("response_type", "code")
                    .queryParam("client_id", trimToEmpty(config.getClientId()))
                    .queryParam("redirect_uri", callbackUrl)
                    .queryParam("scope", "get_user_info")
                    .queryParam("state", state)
                    .build(true).toUriString();
        }
        if (platform == OauthPlatform.WECHAT) {
            String base = UriComponentsBuilder.fromHttpUrl(WECHAT_AUTHORIZE_URL)
                    .queryParam("appid", trimToEmpty(config.getClientId()))
                    .queryParam("redirect_uri", callbackUrl)
                    .queryParam("response_type", "code")
                    .queryParam("scope", "snsapi_login")
                    .queryParam("state", state)
                    .build(true).toUriString();
            return base + "#wechat_redirect";
        }
        throw new BizException(400, "不支持的登录平台");
    }

    private String buildCallbackUrl(OauthPlatform platform, HttpServletRequest request) {
        String configured = trimToEmpty(oauthProperties.getBackendBaseUrl());
        String base;
        if (StringUtils.hasText(configured) && (configured.startsWith("http://") || configured.startsWith("https://"))) {
            base = stripTrailingSlash(configured);
        } else {
            String scheme = firstHeader(request.getHeader("X-Forwarded-Proto"));
            if (!StringUtils.hasText(scheme)) scheme = request.getScheme();
            String host = firstHeader(request.getHeader("X-Forwarded-Host"));
            if (!StringUtils.hasText(host)) {
                host = request.getServerName();
                int port = request.getServerPort();
                if (!isDefaultPort(scheme, port)) host = host + ":" + port;
            }
            base = scheme + "://" + host;
        }
        return base + "/api/v1/user/oauth/callback/" + platform.getCode();
    }

    private OauthPlatform requirePlatform(String platformCode) {
        OauthPlatform platform = OauthPlatform.fromCode(platformCode);
        if (platform == null) throw new BizException(400, "不支持的登录平台");
        return platform;
    }

    private OauthStateContext consumeStateContext(HttpServletRequest request, String state) {
        if (!StringUtils.hasText(state)) return null;
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        cleanupStateContext(session);
        Map<String, OauthStateContext> map = stateMap(session, false);
        if (map == null) return null;
        OauthStateContext ctx = map.remove(state.trim());
        return ctx != null && ctx.expireAtMillis > nowMillis() ? ctx : null;
    }

    private PendingBindContext getPendingContext(HttpServletRequest request, String bindToken, boolean consume) {
        if (!StringUtils.hasText(bindToken)) throw new BizException(400, "绑定凭证无效，请重新授权");
        HttpSession session = request.getSession(false);
        if (session == null) throw new BizException(400, "绑定凭证已失效，请重新授权");
        cleanupPendingContext(session);
        Map<String, PendingBindContext> map = pendingMap(session, false);
        if (map == null) throw new BizException(400, "绑定凭证已失效，请重新授权");
        PendingBindContext ctx = consume ? map.remove(bindToken.trim()) : map.get(bindToken.trim());
        if (ctx == null || ctx.expireAtMillis <= nowMillis()) throw new BizException(400, "绑定凭证已失效，请重新授权");
        return ctx;
    }

    private void removePendingContext(HttpServletRequest request, String bindToken) {
        if (!StringUtils.hasText(bindToken)) return;
        HttpSession session = request.getSession(false);
        if (session == null) return;
        Map<String, PendingBindContext> map = pendingMap(session, false);
        if (map != null) map.remove(bindToken.trim());
    }

    private void clearOauthContext(HttpSession session) {
        if (session == null) return;
        session.removeAttribute(SESSION_OAUTH_STATE_MAP);
        session.removeAttribute(SESSION_OAUTH_PENDING_MAP);
    }

    private void cleanupStateContext(HttpSession session) {
        Map<String, OauthStateContext> map = stateMap(session, false);
        if (map == null || map.isEmpty()) return;
        long now = nowMillis();
        map.entrySet().removeIf(e -> e.getValue() == null || e.getValue().expireAtMillis <= now);
    }

    private void cleanupPendingContext(HttpSession session) {
        Map<String, PendingBindContext> map = pendingMap(session, false);
        if (map == null || map.isEmpty()) return;
        long now = nowMillis();
        map.entrySet().removeIf(e -> e.getValue() == null || e.getValue().expireAtMillis <= now);
    }

    @SuppressWarnings("unchecked")
    private Map<String, OauthStateContext> stateMap(HttpSession session, boolean create) {
        Object obj = session.getAttribute(SESSION_OAUTH_STATE_MAP);
        if (obj instanceof Map) return (Map<String, OauthStateContext>) obj;
        if (!create) return null;
        Map<String, OauthStateContext> map = new HashMap<>();
        session.setAttribute(SESSION_OAUTH_STATE_MAP, map);
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, PendingBindContext> pendingMap(HttpSession session, boolean create) {
        Object obj = session.getAttribute(SESSION_OAUTH_PENDING_MAP);
        if (obj instanceof Map) return (Map<String, PendingBindContext>) obj;
        if (!create) return null;
        Map<String, PendingBindContext> map = new HashMap<>();
        session.setAttribute(SESSION_OAUTH_PENDING_MAP, map);
        return map;
    }

    private String buildFallbackByScene(OauthStateContext ctx, String message) {
        if (ctx == null) return buildLoginPageUrl(normalizeClientBaseUrl(null), message);
        if (ctx.scene == OauthScene.BIND) return appendQuery(ctx.clientBaseUrl + "/user/center", OAUTH_MESSAGE_QUERY_KEY, message);
        return buildLoginPageUrl(ctx.clientBaseUrl, message);
    }

    private String buildLoginPageUrl(String clientBaseUrl, String message) {
        return appendQuery(clientBaseUrl + "/login", OAUTH_MESSAGE_QUERY_KEY, message);
    }

    private void setLoginSession(HttpServletRequest request, Long userId, String nickname) {
        HttpSession session = request.getSession(true);
        session.setAttribute(Constants.SESSION_USER_ID, userId);
        session.setAttribute(Constants.SESSION_USER_NICKNAME, nickname);
    }

    private void upgradePasswordHashIfNeeded(User user, String rawPassword) {
        if (user == null || !PasswordUtil.needsUpgrade(user.getPassword())) return;
        user.setPassword(PasswordUtil.hashPassword(rawPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private PendingBindContext toPendingContext(OauthStateContext state, OauthProfile profile) {
        PendingBindContext ctx = new PendingBindContext();
        ctx.platform = profile.platform;
        ctx.openId = profile.openId;
        ctx.unionId = profile.unionId;
        ctx.nickname = normalizeNickname(profile.nickname);
        ctx.avatarUrl = profile.avatarUrl;
        ctx.clientBaseUrl = state.clientBaseUrl;
        ctx.redirectPath = sanitizeRedirectPath(state.redirectPath);
        ctx.expireAtMillis = nowMillis() + safeExpireMillis(oauthProperties.getPendingExpireSeconds(), 600);
        return ctx;
    }

    private String normalizeNickname(String raw) {
        String fallback = "新用户";
        if (!StringUtils.hasText(raw)) return fallback;
        String value = raw.trim();
        if (value.length() > 10) value = value.substring(0, 10);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String nextAvailableNickname(String baseNickname) {
        String base = normalizeNickname(baseNickname);
        if (!existsNickname(base)) return base;
        for (int i = 1; i <= 9999; i++) {
            String suffix = String.valueOf(i);
            int prefixLen = Math.max(1, 10 - suffix.length());
            String candidate = base.substring(0, Math.min(base.length(), prefixLen)) + suffix;
            if (!existsNickname(candidate)) return candidate;
        }
        throw new BizException(500, "系统繁忙，请稍后重试");
    }

    private boolean existsNickname(String nickname) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getNickname, nickname));
        return count != null && count > 0;
    }

    private String randomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(12);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 12; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        sb.append("A1");
        return sb.substring(0, 14);
    }

    private boolean isConfigured(OauthProperties.PlatformConfig config) {
        return config != null
                && Boolean.TRUE.equals(config.getEnabled())
                && StringUtils.hasText(config.getClientId())
                && StringUtils.hasText(config.getClientSecret());
    }

    private String normalizeClientBaseUrl(String value) {
        String fallback = trimToEmpty(oauthProperties.getFrontendBaseUrl());
        if (StringUtils.hasText(value) && (value.startsWith("http://") || value.startsWith("https://"))) return stripTrailingSlash(value.trim());
        if (StringUtils.hasText(fallback) && (fallback.startsWith("http://") || fallback.startsWith("https://"))) return stripTrailingSlash(fallback);
        return "http://127.0.0.1:5173";
    }

    private String sanitizeRedirectPath(String redirectPath) {
        if (!StringUtils.hasText(redirectPath)) return "/error-question/list";
        String path = redirectPath.trim();
        if (!path.startsWith("/") || path.startsWith("//") || path.contains("\r") || path.contains("\n")) return "/error-question/list";
        return path;
    }

    private String appendQuery(String url, String key, String value) {
        if (!StringUtils.hasText(value)) return url;
        return url + (url.contains("?") ? "&" : "?") + key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String stripTrailingSlash(String url) {
        String out = url;
        while (out.endsWith("/")) out = out.substring(0, out.length() - 1);
        return out;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private long safeExpireMillis(Integer seconds, int fallbackSeconds) {
        int s = seconds == null || seconds <= 0 ? fallbackSeconds : seconds;
        return s * 1000L;
    }

    private long nowMillis() {
        return System.currentTimeMillis();
    }

    private String randomToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String firstHeader(String value) {
        if (!StringUtils.hasText(value)) return "";
        int idx = value.indexOf(',');
        return (idx >= 0 ? value.substring(0, idx) : value).trim();
    }

    private boolean isDefaultPort(String scheme, int port) {
        if (port <= 0) return true;
        if ("https".equalsIgnoreCase(scheme)) return port == 443;
        return port == 80;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) if (StringUtils.hasText(value)) return value.trim();
        return "";
    }

    private Map<String, String> mapOf(String... kv) {
        Map<String, String> map = new LinkedHashMap<>();
        if (kv == null) return map;
        for (int i = 0; i + 1 < kv.length; i += 2) if (kv[i] != null && kv[i + 1] != null) map.put(kv[i], kv[i + 1]);
        return map;
    }

    private String httpGet(String url, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(20)).GET();
            if (headers != null) headers.forEach(builder::header);
            HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) throw new BizException(400, "第三方授权请求失败");
            return resp.body();
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(400, "第三方授权请求失败");
        }
    }

    private String httpPostForm(String url, Map<String, String> form, Map<String, String> headers) {
        try {
            StringBuilder body = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> e : form.entrySet()) {
                if (!first) body.append('&');
                first = false;
                body.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .header("Content-Type", "application/x-www-form-urlencoded");
            if (headers != null) headers.forEach(builder::header);
            HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) throw new BizException(400, "第三方授权请求失败");
            return resp.body();
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(400, "第三方授权请求失败");
        }
    }

    private JsonNode parseJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (IOException ex) {
            throw new BizException(400, "第三方返回数据解析失败");
        }
    }

    private JsonNode parseQqCallbackJson(String raw) {
        if (!StringUtils.hasText(raw)) throw new BizException(400, "QQ 返回数据为空");
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) return parseJson(raw.substring(start, end + 1));
        return parseJson(raw);
    }

    private Map<String, String> parseQueryString(String raw) {
        Map<String, String> out = new HashMap<>();
        if (!StringUtils.hasText(raw)) return out;
        for (String part : raw.split("&")) {
            int idx = part.indexOf('=');
            if (idx <= 0) continue;
            String k = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8);
            out.put(k, v);
        }
        return out;
    }

    private String text(JsonNode node, String field) {
        if (node == null || !StringUtils.hasText(field)) return "";
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private Integer intValue(JsonNode node, String field) {
        if (node == null || !StringUtils.hasText(field)) return null;
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        if (value.isInt() || value.isLong()) return value.asInt();
        String text = value.asText("");
        if (!StringUtils.hasText(text)) return null;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private enum OauthScene {
        LOGIN,
        BIND
    }

    private static final class OauthStateContext {
        private OauthScene scene;
        private String platform;
        private Long userId;
        private String clientBaseUrl;
        private String redirectPath;
        private long expireAtMillis;
    }

    private static final class OauthProfile {
        private String platform;
        private String openId;
        private String unionId;
        private String nickname;
        private String avatarUrl;
    }

    private static final class PendingBindContext {
        private String platform;
        private String openId;
        private String unionId;
        private String nickname;
        private String avatarUrl;
        private String clientBaseUrl;
        private String redirectPath;
        private long expireAtMillis;
    }
}
