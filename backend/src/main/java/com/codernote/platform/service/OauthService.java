package com.codernote.platform.service;

import com.codernote.platform.dto.user.OauthAutoRegisterRequest;
import com.codernote.platform.dto.user.OauthBindExistingRequest;
import com.codernote.platform.dto.user.OauthBindingStatusVO;
import com.codernote.platform.dto.user.OauthPendingBindVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface OauthService {

    String buildLoginAuthorizeUrl(String platformCode, String clientBaseUrl, String redirectPath, HttpServletRequest request);

    String buildBindAuthorizeUrl(String platformCode, String clientBaseUrl, Long userId, HttpServletRequest request);

    String handleCallback(String platformCode, String state, String code, String error, HttpServletRequest request);

    OauthPendingBindVO getPendingBindInfo(String bindToken, HttpServletRequest request);

    void bindExistingAccount(OauthBindExistingRequest request, HttpServletRequest servletRequest);

    void autoRegisterAndLogin(OauthAutoRegisterRequest request, HttpServletRequest servletRequest);

    List<OauthBindingStatusVO> listBindings(Long userId);

    void unbind(Long userId, String platformCode);
}
