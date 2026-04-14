package com.codernote.platform.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(MDC_REQUEST_ID_KEY, requestId);

        long startMillis = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = StringUtils.hasText(query) ? path + "?" + query : path;
        try {
            filterChain.doFilter(request, response);
        } finally {
            long costMillis = Math.max(0L, System.currentTimeMillis() - startMillis);
            int status = response.getStatus();
            if (status >= 500) {
                log.error("request_id={} method={} path={} status={} cost_ms={}",
                        requestId, method, fullPath, status, costMillis);
            } else if (costMillis >= 1500L) {
                log.warn("request_id={} method={} path={} status={} cost_ms={}",
                        requestId, method, fullPath, status, costMillis);
            } else {
                log.info("request_id={} method={} path={} status={} cost_ms={}",
                        requestId, method, fullPath, status, costMillis);
            }
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }

    private String resolveRequestId(String incoming) {
        if (!StringUtils.hasText(incoming)) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        String trimmed = incoming.trim();
        if (trimmed.length() > 64) {
            return trimmed.substring(0, 64);
        }
        return trimmed;
    }
}
