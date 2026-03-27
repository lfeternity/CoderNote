package com.codernote.platform.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface AvatarService {

    void saveAvatar(Long userId, MultipartFile file);

    void resetAvatar(Long userId);

    String buildAvatarUrl(Long userId);

    ResponseEntity<Resource> readAvatar(Long userId);
}