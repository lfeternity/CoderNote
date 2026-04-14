package com.codernote.platform.security;

import com.codernote.platform.common.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;
    private final CsrfTokenService csrfTokenService;

    public AuthInterceptor(ObjectMapper objectMapper, CsrfTokenService csrfTokenService) {
        this.objectMapper = objectMapper;
        this.csrfTokenService = csrfTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(Constants.SESSION_USER_ID) instanceof Long) {
            csrfTokenService.ensureTokenAndWrite(session, response);
            if (isMutatingMethod(request.getMethod()) && !csrfTokenService.validate(request, session)) {
                writeError(response, 403, "Invalid CSRF token");
                return false;
            }
            return true;
        }
        writeError(response, 401, "Not logged in");
        return false;
    }

    private boolean isMutatingMethod(String method) {
        if (!StringUtils.hasText(method)) {
            return false;
        }
        String normalized = method.trim().toUpperCase();
        return !"GET".equals(normalized)
                && !"HEAD".equals(normalized)
                && !"OPTIONS".equals(normalized)
                && !"TRACE".equals(normalized);
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(200);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
