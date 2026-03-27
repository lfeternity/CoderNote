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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OauthServiceImpl implements OauthService {

    private static final String SESSION_OAUTH_STATE_MAP = "OAUTH_STATE_MAP";
    private static final String SESSION_OAUTH_MOCK_CODE_MAP = "OAUTH_MOCK_CODE_MAP";
    private static final String SESSION_OAUTH_PENDING_MAP = "OAUTH_PENDING_MAP";
    private static final String DEFAULT_MAJOR = "Computer Science";
    private static final String OAUTH_MESSAGE_QUERY_KEY = "oauthMessage";
    private static final String BIND_TOKEN_QUERY_KEY = "bindToken";

    private final UserMapper userMapper;
    private final UserOauthBindMapper userOauthBindMapper;
    private final OauthProperties oauthProperties;
    private final Map<String, OauthStateContext> sharedStateContextMap = new ConcurrentHashMap<>();
    private final Map<String, OauthProfile> sharedCodeContextMap = new ConcurrentHashMap<>();
    private final Map<String, PendingBindContext> sharedPendingContextMap = new ConcurrentHashMap<>();

    public OauthServiceImpl(UserMapper userMapper,
                            UserOauthBindMapper userOauthBindMapper,
                            OauthProperties oauthProperties) {
        this.userMapper = userMapper;
        this.userOauthBindMapper = userOauthBindMapper;
        this.oauthProperties = oauthProperties;
    }

    @Override
    public String buildLoginAuthorizeUrl(String platformCode, String clientBaseUrl, String redirectPath, HttpServletRequest request) {
        OauthPlatform platform = requirePlatform(platformCode);
        HttpSession session = request.getSession(true);
        cleanupAllSessionCaches(session);

        String state = randomToken();
        OauthStateContext context = new OauthStateContext();
        context.scene = OauthScene.LOGIN;
        context.platform = platform.getCode();
        context.userId = null;
        context.clientBaseUrl = normalizeClientBaseUrl(clientBaseUrl);
        context.redirectPath = sanitizeRedirectPath(redirectPath);
        context.expireAtMillis = nowMillis() + safeExpireMillis(oauthProperties.getStateExpireSeconds(), 300);
        stateContextMap(session, true).put(state, context);
        sharedStateContextMap.put(state, context);

        return "/api/v1/user/oauth/mock/authorize/" + platform.getCode()
                + "?state=" + encodeQueryValue(state);
    }

    @Override
    public String buildBindAuthorizeUrl(String platformCode, String clientBaseUrl, Long userId, HttpServletRequest request) {
        OauthPlatform platform = requirePlatform(platformCode);
        if (userId == null) {
            throw new BizException(401, "请先登录后再绑定账号");
        }

        HttpSession session = request.getSession(true);
        cleanupAllSessionCaches(session);

        String state = randomToken();
        OauthStateContext context = new OauthStateContext();
        context.scene = OauthScene.BIND;
        context.platform = platform.getCode();
        context.userId = userId;
        context.clientBaseUrl = normalizeClientBaseUrl(clientBaseUrl);
        context.redirectPath = "/user/center";
        context.expireAtMillis = nowMillis() + safeExpireMillis(oauthProperties.getStateExpireSeconds(), 300);
        stateContextMap(session, true).put(state, context);
        sharedStateContextMap.put(state, context);

        return "/api/v1/user/oauth/mock/authorize/" + platform.getCode()
                + "?state=" + encodeQueryValue(state);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(String platformCode, String state, String code, String error, HttpServletRequest request) {
        OauthPlatform callbackPlatform = requirePlatform(platformCode);
        OauthStateContext stateContext = consumeStateContext(request, state);
        if (stateContext == null) {
            return buildLoginPageUrl(normalizeClientBaseUrl(null), "授权状态已失效，请重试");
        }
        if (!callbackPlatform.getCode().equals(stateContext.platform)) {
            return buildFallbackByScene(stateContext, "授权平台不匹配，请重新发起授权");
        }
        if ("access_denied".equalsIgnoreCase(trimToEmpty(error))) {
            return buildFallbackByScene(stateContext, "授权已取消");
        }
        if (!StringUtils.hasText(code)) {
            return buildFallbackByScene(stateContext, "授权信息获取失败，请重试");
        }

        OauthProfile oauthProfile = consumeMockCodeContext(request, code);
        if (oauthProfile == null || !StringUtils.hasText(oauthProfile.openId)) {
            return buildFallbackByScene(stateContext, "授权信息获取失败，请重试");
        }
        if (!callbackPlatform.getCode().equals(oauthProfile.platform)) {
            return buildFallbackByScene(stateContext, "授权信息获取失败，请重试");
        }

        try {
            if (stateContext.scene == OauthScene.BIND) {
                return handleBindCallback(stateContext, callbackPlatform, oauthProfile, request);
            }
            return handleLoginCallback(stateContext, callbackPlatform, oauthProfile, request);
        } catch (BizException e) {
            return buildFallbackByScene(stateContext, e.getMessage());
        }
    }

    @Override
    public String renderMockAuthorizePage(String platformCode, String state, HttpServletRequest request) {
        OauthPlatform platform = requirePlatform(platformCode);
        OauthStateContext context = peekStateContext(request, state);
        if (context == null || !platform.getCode().equals(context.platform)) {
            return "<!doctype html><html><body>授权状态已失效，请关闭后重试。</body></html>";
        }

        String platformName = escapeHtml(platform.getDisplayName());
        String actionText = context.scene == OauthScene.BIND ? "绑定账号" : "登录 / 注册";
        String brandColor = platform.getBrandColor();
        String encodedState = escapeHtml(state);
        String grantUrl = "/api/v1/user/oauth/mock/grant/" + platform.getCode();
        String cancelUrl = "/api/v1/user/oauth/mock/cancel/" + platform.getCode() + "?state=" + encodeQueryValue(state);

        return "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + platformName + " 授权</title>"
                + "<style>"
                + "body{margin:0;font-family:'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;"
                + "background:linear-gradient(145deg,#eef4ff,#f8fbff);color:#1f2d3d;}"
                + ".wrap{max-width:480px;margin:56px auto;padding:0 14px;}"
                + ".card{background:#fff;border:1px solid #d6e1f6;border-radius:18px;padding:24px;box-shadow:0 14px 32px rgba(30,64,175,.08);}"
                + ".tag{display:inline-flex;align-items:center;padding:5px 10px;border-radius:999px;font-size:12px;"
                + "background:" + brandColor + "1F;color:" + brandColor + ";font-weight:700;}"
                + "h1{margin:14px 0 8px;font-size:22px;color:#1e3a8a;}p{margin:0 0 16px;color:#4b5b74;line-height:1.7;}"
                + ".row{margin:0 0 12px;}.row label{display:block;margin:0 0 6px;color:#425b8f;font-size:13px;}"
                + ".row input{width:100%;height:40px;border:1px solid #c8d5ef;border-radius:10px;padding:0 12px;font-size:14px;outline:none;}"
                + ".row input:focus{border-color:#5b86e5;box-shadow:0 0 0 3px rgba(91,134,229,.16);}"
                + ".actions{display:flex;gap:10px;margin-top:16px;}"
                + "button,a.btn{height:40px;border-radius:10px;border:0;padding:0 16px;cursor:pointer;font-weight:600;font-size:14px;}"
                + "button{background:" + brandColor + ";color:#fff;flex:1;}"
                + "a.btn{display:inline-flex;align-items:center;justify-content:center;flex:1;text-decoration:none;background:#f2f6ff;color:#2f4c8c;border:1px solid #cddaf4;}"
                + ".tip{margin-top:14px;font-size:12px;color:#6a7b98;}"
                + "</style></head><body><div class=\"wrap\"><div class=\"card\">"
                + "<span class=\"tag\">" + platformName + " 授权页（模拟）</span>"
                + "<h1>继续 " + actionText + "</h1>"
                + "<p>用于本地演示 OAuth 流程，可自定义一个“平台账号标识”，同一标识会映射为同一第三方账号。</p>"
                + "<form action=\"" + grantUrl + "\" method=\"post\">"
                + "<input type=\"hidden\" name=\"state\" value=\"" + encodedState + "\">"
                + "<div class=\"row\"><label>平台账号标识（用于生成 OpenID）</label>"
                + "<input name=\"accountId\" maxlength=\"64\" value=\"demo_user\"></div>"
                + "<div class=\"row\"><label>平台昵称（可留空）</label>"
                + "<input name=\"nickname\" maxlength=\"30\" placeholder=\"例如：我的" + platformName + "昵称\"></div>"
                + "<div class=\"actions\"><button type=\"submit\">同意授权</button>"
                + "<a class=\"btn\" href=\"" + cancelUrl + "\">取消授权</a></div></form>"
                + "<div class=\"tip\">说明：授权码一次性使用，state 会在回调时校验并失效。</div>"
                + "</div></div></body></html>";
    }

    @Override
    public String buildMockGrantCallbackUrl(String platformCode,
                                            String state,
                                            String accountId,
                                            String nickname,
                                            HttpServletRequest request) {
        OauthPlatform platform = requirePlatform(platformCode);
        OauthStateContext context = peekStateContext(request, state);
        if (context == null || !platform.getCode().equals(context.platform)) {
            throw new BizException(400, "授权状态已失效，请重新发起授权");
        }

        String normalizedAccount = StringUtils.hasText(accountId)
                ? accountId.trim()
                : "demo_user";
        String nicknameBase = StringUtils.hasText(nickname)
                ? nickname.trim()
                : platform.getDisplayName() + "用户";
        String digest = PasswordUtil.md5(platform.getCode() + "|" + normalizedAccount.toLowerCase(Locale.ROOT));

        OauthProfile profile = new OauthProfile();
        profile.platform = platform.getCode();
        profile.openId = platform.getCode() + "_" + digest.substring(0, 24);
        if (platform == OauthPlatform.WECHAT) {
            profile.unionId = "union_" + digest.substring(0, 24);
        }
        profile.nickname = limitText(nicknameBase, 30);
        profile.avatarUrl = buildSvgAvatarDataUrl(profile.nickname, platform.getBrandColor());
        profile.expireAtMillis = nowMillis() + safeExpireMillis(oauthProperties.getCodeExpireSeconds(), 300);

        String code = randomToken();
        HttpSession session = request.getSession(true);
        mockCodeContextMap(session, true).put(code, profile);
        sharedCodeContextMap.put(code, profile);
        cleanupMockCodeContext(session);

        return "/api/v1/user/oauth/callback/" + platform.getCode()
                + "?code=" + encodeQueryValue(code)
                + "&state=" + encodeQueryValue(state);
    }

    @Override
    public String buildMockCancelCallbackUrl(String platformCode, String state, HttpServletRequest request) {
        OauthPlatform platform = requirePlatform(platformCode);
        OauthStateContext context = peekStateContext(request, state);
        if (context == null || !platform.getCode().equals(context.platform)) {
            throw new BizException(400, "授权状态已失效，请重新发起授权");
        }
        return "/api/v1/user/oauth/callback/" + platform.getCode()
                + "?error=access_denied&state=" + encodeQueryValue(state);
    }

    @Override
    public OauthPendingBindVO getPendingBindInfo(String bindToken, HttpServletRequest request) {
        PendingBindContext context = getPendingContext(request, bindToken, false);
        OauthPlatform platform = requirePlatform(context.platform);

        OauthPendingBindVO vo = new OauthPendingBindVO();
        vo.setPlatform(platform.getCode());
        vo.setPlatformName(platform.getDisplayName());
        vo.setNickname(context.nickname);
        vo.setAvatarUrl(context.avatarUrl);
        vo.setRedirectPath(context.redirectPath);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindExistingAccount(OauthBindExistingRequest request, HttpServletRequest servletRequest) {
        PendingBindContext context = getPendingContext(servletRequest, request.getBindToken(), false);
        String rawPassword = trimToEmpty(request.getPassword());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getNickname, trimToEmpty(request.getNickname())));
        if (user == null || !PasswordUtil.matchesPassword(rawPassword, user.getPassword())) {
            throw new BizException(400, "账号或密码错误");
        }

        upgradePasswordHashIfNeeded(user, rawPassword);
        upsertBinding(user.getId(), context);
        removePendingContext(servletRequest, request.getBindToken());
        setLoginSession(servletRequest, user.getId(), user.getNickname());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoRegisterAndLogin(OauthAutoRegisterRequest request, HttpServletRequest servletRequest) {
        PendingBindContext context = getPendingContext(servletRequest, request.getBindToken(), false);
        String preferred = StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : context.nickname;
        String registerNickname = nextAvailableNickname(normalizeNickname(preferred));

        User user = new User();
        user.setNickname(registerNickname);
        user.setMajor(DEFAULT_MAJOR);
        user.setPassword(PasswordUtil.hashPassword(randomPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        upsertBinding(user.getId(), context);
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

    private String handleBindCallback(OauthStateContext stateContext,
                                      OauthPlatform platform,
                                      OauthProfile oauthProfile,
                                      HttpServletRequest request) {
        if (stateContext.userId == null) {
            return buildLoginPageUrl(stateContext.clientBaseUrl, "登录态已失效，请先登录后再绑定");
        }
        User user = userMapper.selectById(stateContext.userId);
        if (user == null) {
            return buildLoginPageUrl(stateContext.clientBaseUrl, "当前用户不存在，请重新登录");
        }

        PendingBindContext context = toPendingContext(stateContext, oauthProfile);
        upsertBinding(user.getId(), context);
        setLoginSession(request, user.getId(), user.getNickname());
        return appendQuery(stateContext.clientBaseUrl + "/user/center", OAUTH_MESSAGE_QUERY_KEY, "绑定成功");
    }

    private String handleLoginCallback(OauthStateContext stateContext,
                                       OauthPlatform platform,
                                       OauthProfile oauthProfile,
                                       HttpServletRequest request) {
        UserOauthBind bind = userOauthBindMapper.selectOne(new LambdaQueryWrapper<UserOauthBind>()
                .eq(UserOauthBind::getPlatform, platform.getCode())
                .eq(UserOauthBind::getOpenId, oauthProfile.openId));

        if (bind != null) {
            User user = userMapper.selectById(bind.getUserId());
            if (user == null) {
                throw new BizException(404, "绑定账号不存在，请联系管理员");
            }
            refreshBindingProfile(bind, oauthProfile);
            setLoginSession(request, user.getId(), user.getNickname());
            return appendQuery(
                    stateContext.clientBaseUrl + sanitizeRedirectPath(stateContext.redirectPath),
                    "oauthLoggedIn",
                    "1"
            );
        }

        String bindToken = randomToken();
        PendingBindContext context = toPendingContext(stateContext, oauthProfile);
        context.expireAtMillis = nowMillis() + safeExpireMillis(oauthProperties.getPendingExpireSeconds(), 600);
        pendingContextMap(request.getSession(true), true).put(bindToken, context);
        sharedPendingContextMap.put(bindToken, context);
        cleanupPendingContext(request.getSession(false));

        return appendQuery(stateContext.clientBaseUrl + "/oauth/bind", BIND_TOKEN_QUERY_KEY, bindToken);
    }

    private void upsertBinding(Long userId, PendingBindContext context) {
        OauthPlatform platform = requirePlatform(context.platform);
        UserOauthBind bindByOpenId = userOauthBindMapper.selectOne(new LambdaQueryWrapper<UserOauthBind>()
                .eq(UserOauthBind::getPlatform, platform.getCode())
                .eq(UserOauthBind::getOpenId, context.openId));

        if (bindByOpenId != null && !Objects.equals(bindByOpenId.getUserId(), userId)) {
            throw new BizException(400, "该账号已绑定其他用户，请更换账号或联系客服");
        }

        UserOauthBind bindByUserPlatform = userOauthBindMapper.selectOne(new LambdaQueryWrapper<UserOauthBind>()
                .eq(UserOauthBind::getUserId, userId)
                .eq(UserOauthBind::getPlatform, platform.getCode()));

        if (bindByUserPlatform != null && !Objects.equals(bindByUserPlatform.getOpenId(), context.openId)) {
            throw new BizException(400, "当前账号已绑定其他" + platform.getDisplayName() + "账号，请先解绑");
        }

        LocalDateTime now = LocalDateTime.now();
        if (bindByOpenId != null) {
            bindByOpenId.setUnionId(context.unionId);
            bindByOpenId.setPlatformNickname(context.nickname);
            bindByOpenId.setPlatformAvatarUrl(context.avatarUrl);
            bindByOpenId.setUpdatedAt(now);
            userOauthBindMapper.updateById(bindByOpenId);
            return;
        }

        UserOauthBind bind = new UserOauthBind();
        bind.setUserId(userId);
        bind.setPlatform(platform.getCode());
        bind.setOpenId(context.openId);
        bind.setUnionId(context.unionId);
        bind.setPlatformNickname(context.nickname);
        bind.setPlatformAvatarUrl(context.avatarUrl);
        bind.setCreatedAt(now);
        bind.setUpdatedAt(now);
        userOauthBindMapper.insert(bind);
    }

    private void refreshBindingProfile(UserOauthBind bind, OauthProfile oauthProfile) {
        bind.setUnionId(oauthProfile.unionId);
        bind.setPlatformNickname(oauthProfile.nickname);
        bind.setPlatformAvatarUrl(oauthProfile.avatarUrl);
        bind.setUpdatedAt(LocalDateTime.now());
        userOauthBindMapper.updateById(bind);
    }

    private OauthPlatform requirePlatform(String platformCode) {
        OauthPlatform platform = OauthPlatform.fromCode(platformCode);
        if (platform == null) {
            throw new BizException(400, "不支持的登录平台");
        }
        return platform;
    }

    private OauthStateContext consumeStateContext(HttpServletRequest request, String state) {
        if (!StringUtils.hasText(state)) {
            return null;
        }
        String key = state.trim();
        OauthStateContext context = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            Map<String, OauthStateContext> map = stateContextMap(session, false);
            if (map != null) {
                cleanupStateContext(session);
                context = map.remove(key);
            }
        }
        if (context == null) {
            cleanupSharedCaches();
            context = sharedStateContextMap.remove(key);
        } else {
            sharedStateContextMap.remove(key);
        }
        if (context == null || context.expireAtMillis <= nowMillis()) {
            return null;
        }
        return context;
    }

    private OauthStateContext peekStateContext(HttpServletRequest request, String state) {
        if (!StringUtils.hasText(state)) {
            return null;
        }
        String key = state.trim();
        OauthStateContext context = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            Map<String, OauthStateContext> map = stateContextMap(session, false);
            if (map != null) {
                cleanupStateContext(session);
                context = map.get(key);
            }
        }
        if (context == null) {
            cleanupSharedCaches();
            context = sharedStateContextMap.get(key);
        }
        if (context == null || context.expireAtMillis <= nowMillis()) {
            return null;
        }
        return context;
    }

    private OauthProfile consumeMockCodeContext(HttpServletRequest request, String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String key = code.trim();
        OauthProfile context = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            Map<String, OauthProfile> map = mockCodeContextMap(session, false);
            if (map != null) {
                cleanupMockCodeContext(session);
                context = map.remove(key);
            }
        }
        if (context == null) {
            cleanupSharedCaches();
            context = sharedCodeContextMap.remove(key);
        } else {
            sharedCodeContextMap.remove(key);
        }
        if (context == null || context.expireAtMillis <= nowMillis()) {
            return null;
        }
        return context;
    }

    private PendingBindContext getPendingContext(HttpServletRequest request, String bindToken, boolean consume) {
        if (!StringUtils.hasText(bindToken)) {
            throw new BizException(400, "绑定凭证无效，请重新授权");
        }
        String key = bindToken.trim();
        PendingBindContext context = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            Map<String, PendingBindContext> map = pendingContextMap(session, false);
            if (map != null) {
                cleanupPendingContext(session);
                context = consume ? map.remove(key) : map.get(key);
            }
        }
        if (consume && context != null) {
            sharedPendingContextMap.remove(key);
        }
        if (context == null) {
            cleanupSharedCaches();
            context = consume ? sharedPendingContextMap.remove(key) : sharedPendingContextMap.get(key);
        }
        if (context == null || context.expireAtMillis <= nowMillis()) {
            throw new BizException(400, "绑定凭证已失效，请重新授权");
        }
        return context;
    }

    private void removePendingContext(HttpServletRequest request, String bindToken) {
        if (!StringUtils.hasText(bindToken)) {
            return;
        }
        String key = bindToken.trim();
        HttpSession session = request.getSession(false);
        if (session != null) {
            Map<String, PendingBindContext> map = pendingContextMap(session, false);
            if (map != null) {
                map.remove(key);
            }
        }
        sharedPendingContextMap.remove(key);
    }

    private String buildFallbackByScene(OauthStateContext context, String message) {
        if (context == null) {
            return buildLoginPageUrl(normalizeClientBaseUrl(null), message);
        }
        if (context.scene == OauthScene.BIND) {
            return appendQuery(context.clientBaseUrl + "/user/center", OAUTH_MESSAGE_QUERY_KEY, message);
        }
        return buildLoginPageUrl(context.clientBaseUrl, message);
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
        if (user == null || !PasswordUtil.needsUpgrade(user.getPassword())) {
            return;
        }
        user.setPassword(PasswordUtil.hashPassword(rawPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private PendingBindContext toPendingContext(OauthStateContext stateContext, OauthProfile oauthProfile) {
        PendingBindContext context = new PendingBindContext();
        context.platform = oauthProfile.platform;
        context.openId = oauthProfile.openId;
        context.unionId = oauthProfile.unionId;
        context.nickname = normalizeNickname(oauthProfile.nickname);
        context.avatarUrl = oauthProfile.avatarUrl;
        context.clientBaseUrl = stateContext.clientBaseUrl;
        context.redirectPath = sanitizeRedirectPath(stateContext.redirectPath);
        context.expireAtMillis = nowMillis() + safeExpireMillis(oauthProperties.getPendingExpireSeconds(), 600);
        return context;
    }

    private String normalizeNickname(String raw) {
        String fallback = "新用户";
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        String trimmed = raw.trim();
        String limited = limitText(trimmed, 10);
        return StringUtils.hasText(limited) ? limited : fallback;
    }

    private String nextAvailableNickname(String baseNickname) {
        String base = normalizeNickname(baseNickname);
        if (!existsNickname(base)) {
            return base;
        }
        for (int i = 1; i <= 9999; i++) {
            String suffix = String.valueOf(i);
            int prefixLen = Math.max(1, 10 - suffix.length());
            String candidate = limitText(base, prefixLen) + suffix;
            if (!existsNickname(candidate)) {
                return candidate;
            }
        }
        throw new BizException(500, "系统繁忙，请稍后重试");
    }

    private boolean existsNickname(String nickname) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getNickname, nickname));
        return count != null && count > 0;
    }

    private String randomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(12);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        sb.append("A1");
        return sb.substring(0, 14);
    }

    private void cleanupAllSessionCaches(HttpSession session) {
        cleanupStateContext(session);
        cleanupMockCodeContext(session);
        cleanupPendingContext(session);
        cleanupSharedCaches();
    }

    private void cleanupStateContext(HttpSession session) {
        if (session == null) {
            return;
        }
        Map<String, OauthStateContext> map = stateContextMap(session, false);
        if (map == null || map.isEmpty()) {
            return;
        }
        long now = nowMillis();
        map.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().expireAtMillis <= now);
    }

    private void cleanupMockCodeContext(HttpSession session) {
        if (session == null) {
            return;
        }
        Map<String, OauthProfile> map = mockCodeContextMap(session, false);
        if (map == null || map.isEmpty()) {
            return;
        }
        long now = nowMillis();
        map.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().expireAtMillis <= now);
    }

    private void cleanupPendingContext(HttpSession session) {
        if (session == null) {
            return;
        }
        Map<String, PendingBindContext> map = pendingContextMap(session, false);
        if (map == null || map.isEmpty()) {
            return;
        }
        long now = nowMillis();
        map.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().expireAtMillis <= now);
    }

    private void cleanupSharedCaches() {
        long now = nowMillis();
        sharedStateContextMap.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().expireAtMillis <= now);
        sharedCodeContextMap.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().expireAtMillis <= now);
        sharedPendingContextMap.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().expireAtMillis <= now);
    }

    @SuppressWarnings("unchecked")
    private Map<String, OauthStateContext> stateContextMap(HttpSession session, boolean create) {
        Object obj = session.getAttribute(SESSION_OAUTH_STATE_MAP);
        if (obj instanceof Map) {
            return (Map<String, OauthStateContext>) obj;
        }
        if (!create) {
            return null;
        }
        Map<String, OauthStateContext> map = new HashMap<>();
        session.setAttribute(SESSION_OAUTH_STATE_MAP, map);
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, OauthProfile> mockCodeContextMap(HttpSession session, boolean create) {
        Object obj = session.getAttribute(SESSION_OAUTH_MOCK_CODE_MAP);
        if (obj instanceof Map) {
            return (Map<String, OauthProfile>) obj;
        }
        if (!create) {
            return null;
        }
        Map<String, OauthProfile> map = new HashMap<>();
        session.setAttribute(SESSION_OAUTH_MOCK_CODE_MAP, map);
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, PendingBindContext> pendingContextMap(HttpSession session, boolean create) {
        Object obj = session.getAttribute(SESSION_OAUTH_PENDING_MAP);
        if (obj instanceof Map) {
            return (Map<String, PendingBindContext>) obj;
        }
        if (!create) {
            return null;
        }
        Map<String, PendingBindContext> map = new HashMap<>();
        session.setAttribute(SESSION_OAUTH_PENDING_MAP, map);
        return map;
    }

    private String normalizeClientBaseUrl(String clientBaseUrl) {
        String fallback = trimToEmpty(oauthProperties.getFrontendBaseUrl());
        if (StringUtils.hasText(clientBaseUrl)) {
            String normalized = clientBaseUrl.trim();
            if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
                return stripTrailingSlash(normalized);
            }
        }
        if (StringUtils.hasText(fallback) && (fallback.startsWith("http://") || fallback.startsWith("https://"))) {
            return stripTrailingSlash(fallback);
        }
        return "http://127.0.0.1:5173";
    }

    private String stripTrailingSlash(String url) {
        String result = url;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String sanitizeRedirectPath(String redirectPath) {
        if (!StringUtils.hasText(redirectPath)) {
            return "/error-question/list";
        }
        String path = redirectPath.trim();
        if (!path.startsWith("/") || path.startsWith("//")) {
            return "/error-question/list";
        }
        if (path.contains("\r") || path.contains("\n")) {
            return "/error-question/list";
        }
        return path;
    }

    private String appendQuery(String url, String key, String value) {
        if (!StringUtils.hasText(value)) {
            return url;
        }
        char separator = url.contains("?") ? '&' : '?';
        return url + separator + key + "=" + encodeQueryValue(value);
    }

    private String encodeQueryValue(String value) {
        return URLEncoder.encode(trimToEmpty(value), StandardCharsets.UTF_8);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private long nowMillis() {
        return System.currentTimeMillis();
    }

    private long safeExpireMillis(Integer seconds, int defaultSeconds) {
        int safeSeconds = seconds == null || seconds <= 0 ? defaultSeconds : seconds;
        return safeSeconds * 1000L;
    }

    private String randomToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String limitText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (maxLength <= 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String buildSvgAvatarDataUrl(String nickname, String brandColor) {
        String initial = StringUtils.hasText(nickname) ? nickname.substring(0, 1) : "U";
        String color = StringUtils.hasText(brandColor) ? brandColor : "#3153A8";
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='128' height='128'>"
                + "<rect width='128' height='128' rx='20' fill='" + color + "'/>"
                + "<text x='64' y='76' text-anchor='middle' font-size='54' fill='#FFFFFF' font-family='Arial,sans-serif'>"
                + escapeHtml(initial.toUpperCase(Locale.ROOT))
                + "</text></svg>";
        return "data:image/svg+xml;base64," + java.util.Base64.getEncoder()
                .encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (char c : raw.toCharArray()) {
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else if (c == '\'') {
                sb.append("&#39;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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
        private long expireAtMillis;
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
