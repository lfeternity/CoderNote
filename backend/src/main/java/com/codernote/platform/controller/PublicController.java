package com.codernote.platform.controller;

import com.codernote.platform.common.ApiResponse;
import com.codernote.platform.service.AvatarService;
import com.codernote.platform.service.PublicService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    private final PublicService publicService;
    private final AvatarService avatarService;

    public PublicController(PublicService publicService, AvatarService avatarService) {
        this.publicService = publicService;
        this.avatarService = avatarService;
    }

    @GetMapping("/options")
    public ApiResponse<Map<String, Object>> options() {
        return ApiResponse.success(publicService.options());
    }

    @GetMapping("/avatar/{userId}")
    public ResponseEntity<Resource> avatar(@PathVariable Long userId) {
        return avatarService.readAvatar(userId);
    }
}