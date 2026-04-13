package com.codernote.platform.controller;

import com.codernote.platform.common.ApiResponse;
import com.codernote.platform.dto.user.OauthAutoRegisterRequest;
import com.codernote.platform.dto.user.OauthBindExistingRequest;
import com.codernote.platform.dto.user.OauthBindingStatusVO;
import com.codernote.platform.dto.user.OauthPendingBindVO;
import com.codernote.platform.security.AuthContext;
import com.codernote.platform.service.OauthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/user/oauth")
public class UserOauthController {

    private final OauthService oauthService;

    public UserOauthController(OauthService oauthService) {
        this.oauthService = oauthService;
    }

    @GetMapping("/login/authorize/{platform}")
    public void loginAuthorize(@PathVariable String platform,
                               @RequestParam(value = "clientBaseUrl", required = false) String clientBaseUrl,
                               @RequestParam(value = "redirect", required = false) String redirect,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        String targetUrl = oauthService.buildLoginAuthorizeUrl(platform, clientBaseUrl, redirect, request);
        response.sendRedirect(targetUrl);
    }

    @GetMapping("/bind/authorize/{platform}")
    public void bindAuthorize(@PathVariable String platform,
                              @RequestParam(value = "clientBaseUrl", required = false) String clientBaseUrl,
                              HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
        Long userId = AuthContext.getRequiredUserId(request);
        String targetUrl = oauthService.buildBindAuthorizeUrl(platform, clientBaseUrl, userId, request);
        response.sendRedirect(targetUrl);
    }

    @GetMapping("/callback/{platform}")
    public void callback(@PathVariable String platform,
                         @RequestParam(value = "state", required = false) String state,
                         @RequestParam(value = "code", required = false) String code,
                         @RequestParam(value = "error", required = false) String error,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        String redirectUrl = oauthService.handleCallback(platform, state, code, error, request);
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/pending/{bindToken}")
    public ApiResponse<OauthPendingBindVO> pendingBindInfo(@PathVariable String bindToken,
                                                           HttpServletRequest request) {
        return ApiResponse.success(oauthService.getPendingBindInfo(bindToken, request));
    }

    @PostMapping("/bind-existing")
    public ApiResponse<Void> bindExisting(@Valid @RequestBody OauthBindExistingRequest request,
                                          HttpServletRequest servletRequest) {
        oauthService.bindExistingAccount(request, servletRequest);
        return ApiResponse.success("Bind success", null);
    }

    @PostMapping("/auto-register")
    public ApiResponse<Void> autoRegister(@Valid @RequestBody OauthAutoRegisterRequest request,
                                          HttpServletRequest servletRequest) {
        oauthService.autoRegisterAndLogin(request, servletRequest);
        return ApiResponse.success("Register success", null);
    }

    @GetMapping("/bindings")
    public ApiResponse<List<OauthBindingStatusVO>> bindings(HttpServletRequest request) {
        Long userId = AuthContext.getRequiredUserId(request);
        return ApiResponse.success(oauthService.listBindings(userId));
    }

    @DeleteMapping("/bindings/{platform}")
    public ApiResponse<Void> unbind(@PathVariable String platform, HttpServletRequest request) {
        Long userId = AuthContext.getRequiredUserId(request);
        oauthService.unbind(userId, platform);
        return ApiResponse.success("Unbind success", null);
    }
}
