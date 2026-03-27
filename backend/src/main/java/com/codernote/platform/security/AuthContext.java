package com.codernote.platform.security;

import com.codernote.platform.common.Constants;
import com.codernote.platform.exception.BizException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class AuthContext {

    private AuthContext() {
    }

    public static Long getRequiredUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BizException(401, "Not logged in");
        }
        Object userId = session.getAttribute(Constants.SESSION_USER_ID);
        if (!(userId instanceof Long)) {
            throw new BizException(401, "Not logged in");
        }
        return (Long) userId;
    }
}
