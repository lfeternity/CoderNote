package com.codernote.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

@Component
public class CsrfTokenService {

    public static final String SESSION_CSRF_TOKEN = "SESSION_CSRF_TOKEN";
    public static final String CSRF_HEADER_NAME = "X-CSRF-Token";
    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";

    @Value("${app.security.csrf.enabled:true}")
    private boolean csrfEnabled;

    @Value("${app.security.csrf.cookie-secure:false}")
    private boolean csrfCookieSecure;

    @Value("${app.security.csrf.cookie-same-site:Lax}")
    private String csrfCookieSameSite;

    public boolean isEnabled() {
        return csrfEnabled;
    }

    public void ensureTokenAndWrite(HttpSession session, HttpServletResponse response) {
        if (!csrfEnabled || session == null || response == null) {
            return;
        }
        String token = ensureToken(session);
        response.setHeader(CSRF_HEADER_NAME, token);
        ResponseCookie cookie = ResponseCookie.from(CSRF_COOKIE_NAME, token)
                .path("/")
                .httpOnly(false)
                .secure(csrfCookieSecure)
                .sameSite(normalizeSameSite(csrfCookieSameSite))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String ensureToken(HttpSession session) {
        Object value = session.getAttribute(SESSION_CSRF_TOKEN);
        if (value instanceof String && StringUtils.hasText((String) value)) {
            return (String) value;
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        session.setAttribute(SESSION_CSRF_TOKEN, token);
        return token;
    }

    public boolean validate(HttpServletRequest request, HttpSession session) {
        if (!csrfEnabled || session == null) {
            return true;
        }
        Object value = session.getAttribute(SESSION_CSRF_TOKEN);
        if (!(value instanceof String) || !StringUtils.hasText((String) value)) {
            return false;
        }
        String expected = (String) value;
        String actual = request.getHeader(CSRF_HEADER_NAME);
        return StringUtils.hasText(actual) && expected.equals(actual.trim());
    }

    private String normalizeSameSite(String sameSite) {
        if (!StringUtils.hasText(sameSite)) {
            return "Lax";
        }
        String value = sameSite.trim();
        if ("None".equalsIgnoreCase(value) || "Strict".equalsIgnoreCase(value)) {
            return value;
        }
        return "Lax";
    }
}
