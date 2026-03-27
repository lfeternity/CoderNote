package com.codernote.platform.service;

import com.codernote.platform.dto.user.CaptchaVO;
import com.codernote.platform.dto.user.ChangePasswordRequest;
import com.codernote.platform.dto.user.LoginRequest;
import com.codernote.platform.dto.user.RegisterRequest;
import com.codernote.platform.dto.user.UpdateProfileRequest;
import com.codernote.platform.dto.user.UserProfileVO;

import javax.servlet.http.HttpServletRequest;

public interface UserService {

    CaptchaVO generateRegisterCaptcha(HttpServletRequest servletRequest);

    void register(RegisterRequest request, HttpServletRequest servletRequest);

    void login(LoginRequest request, HttpServletRequest servletRequest);

    void logout(HttpServletRequest servletRequest);

    UserProfileVO getProfile(Long userId);

    void updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request, HttpServletRequest servletRequest);
}