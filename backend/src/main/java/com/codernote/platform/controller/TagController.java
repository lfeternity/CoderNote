package com.codernote.platform.controller;

import com.codernote.platform.common.ApiResponse;
import com.codernote.platform.dto.tag.TagListItemVO;
import com.codernote.platform.dto.tag.TagRelationVO;
import com.codernote.platform.dto.tag.TagSaveRequest;
import com.codernote.platform.security.AuthContext;
import com.codernote.platform.service.TagService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/tag")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/list")
    public ApiResponse<List<TagListItemVO>> list(HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(tagService.list(userId));
    }

    @PostMapping("/add")
    public ApiResponse<Long> add(@Valid @RequestBody TagSaveRequest request,
                                 HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success("Tag created", tagService.create(userId, request.getName(), request.getCoverPath()));
    }

    @PutMapping("/update/{tagId}")
    public ApiResponse<Void> update(@PathVariable Long tagId,
                                    @Valid @RequestBody TagSaveRequest request,
                                    HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        tagService.update(userId, tagId, request.getName(), request.getCoverPath());
        return ApiResponse.success("Tag updated", null);
    }

    @DeleteMapping("/delete/{tagId}")
    public ApiResponse<Void> delete(@PathVariable Long tagId,
                                    HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        tagService.delete(userId, tagId);
        return ApiResponse.success("Tag deleted", null);
    }

    @GetMapping("/relation/{tagId}")
    public ApiResponse<TagRelationVO> relation(@PathVariable Long tagId,
                                               HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(tagService.relation(userId, tagId));
    }
}
