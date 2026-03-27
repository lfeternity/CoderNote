package com.codernote.platform.config;

import com.codernote.platform.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
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
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
