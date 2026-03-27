package com.codernote.platform.controller;

import com.codernote.platform.common.ApiResponse;
import com.codernote.platform.dto.user.CaptchaVO;
import com.codernote.platform.dto.user.ChangePasswordRequest;
import com.codernote.platform.dto.user.LoginRequest;
import com.codernote.platform.dto.user.RegisterRequest;
import com.codernote.platform.dto.user.UpdateProfileRequest;
import com.codernote.platform.dto.user.UserProfileVO;
import com.codernote.platform.security.AuthContext;
import com.codernote.platform.service.AvatarService;
import com.codernote.platform.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;
    private final AvatarService avatarService;

    public UserController(UserService userService, AvatarService avatarService) {
        this.userService = userService;
        this.avatarService = avatarService;
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaVO> captcha(HttpServletRequest servletRequest) {
        return ApiResponse.success(userService.generateRegisterCaptcha(servletRequest));
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request,
                                      HttpServletRequest servletRequest) {
        userService.register(request, servletRequest);
        return ApiResponse.success("Register success", null);
    }

    @PostMapping("/login")
    public ApiResponse<Void> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        userService.login(request, servletRequest);
        return ApiResponse.success("Login success", null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest servletRequest) {
        userService.logout(servletRequest);
        return ApiResponse.success("Logout success", null);
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileVO> profile(HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                           HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        userService.updateProfile(userId, request);
        return ApiResponse.success("Profile updated", null);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserProfileVO> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                   HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        avatarService.saveAvatar(userId, file);
        return ApiResponse.success("Avatar updated", userService.getProfile(userId));
    }

    @DeleteMapping("/avatar")
    public ApiResponse<UserProfileVO> resetAvatar(HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        avatarService.resetAvatar(userId);
        return ApiResponse.success("Avatar reset", userService.getProfile(userId));
    }

    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        userService.changePassword(userId, request, servletRequest);
        return ApiResponse.success("Password changed, login again", null);
    }
}