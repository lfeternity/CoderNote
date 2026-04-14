package com.codernote.platform.config;

import com.codernote.platform.security.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final List<String> allowedOrigins;

    public WebConfig(AuthInterceptor authInterceptor,
                     @Value("${app.security.cors.allowed-origins:http://127.0.0.1:5173,http://localhost:5173}")
                     String allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/user/login",
                        "/api/v1/user/register",
                        "/api/v1/user/captcha",
                        "/api/v1/user/oauth/login/authorize/**",
                        "/api/v1/user/oauth/callback/**",
                        "/api/v1/user/oauth/mock/**",
                        "/api/v1/user/oauth/pending/**",
                        "/api/v1/user/oauth/bind-existing",
                        "/api/v1/user/oauth/auto-register",
                        "/api/v1/public/**",
                        "/doc.html",
                        "/swagger-resources/**",
                        "/v2/api-docs",
                        "/webjars/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.isEmpty()
                ? new String[]{"http://127.0.0.1:5173", "http://localhost:5173"}
                : allowedOrigins.toArray(new String[0]);
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "X-Requested-With", "X-CSRF-Token")
                .exposedHeaders("X-CSRF-Token")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
