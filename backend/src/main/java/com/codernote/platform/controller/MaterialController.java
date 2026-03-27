package com.codernote.platform.controller;

import com.codernote.platform.common.ApiResponse;
import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.common.IdsRequest;
import com.codernote.platform.dto.material.MaterialDetailVO;
import com.codernote.platform.dto.material.MaterialListItemVO;
import com.codernote.platform.dto.material.MaterialSaveRequest;
import com.codernote.platform.security.AuthContext;
import com.codernote.platform.service.MaterialService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/study-material")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping("/add")
    public ApiResponse<Long> add(@Valid @RequestBody MaterialSaveRequest request,
                                 HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success("Material created", materialService.create(userId, request));
    }

    @PutMapping("/update/{materialId}")
    public ApiResponse<Void> update(@PathVariable Long materialId,
                                    @Valid @RequestBody MaterialSaveRequest request,
                                    HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        materialService.update(userId, materialId, request);
        return ApiResponse.success("Material updated", null);
    }

    @DeleteMapping("/delete/{materialId}")
    public ApiResponse<Void> delete(@PathVariable Long materialId,
                                    HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        materialService.delete(userId, materialId);
        return ApiResponse.success("Deleted", null);
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody IdsRequest request,
                                         HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        materialService.batchDelete(userId, request.getIds());
        return ApiResponse.success("Batch deleted", null);
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<MaterialListItemVO>> list(@RequestParam(defaultValue = "1") Long pageNo,
                                                             @RequestParam(defaultValue = "10") Long pageSize,
                                                             @RequestParam(required = false) String materialType,
                                                             @RequestParam(required = false) String language,
                                                             @RequestParam(required = false) String tag,
                                                             @RequestParam(required = false) String keyword,
                                                             HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(materialService.page(userId, pageNo, pageSize, materialType, language, tag, keyword));
    }

    @GetMapping("/favorite/list")
    public ApiResponse<PageResult<MaterialListItemVO>> favoriteList(@RequestParam(defaultValue = "1") Long pageNo,
                                                                     @RequestParam(defaultValue = "10") Long pageSize,
                                                                     @RequestParam(required = false) String materialType,
                                                                     @RequestParam(required = false) String language,
                                                                     @RequestParam(required = false) String tag,
                                                                     @RequestParam(required = false) String keyword,
                                                                     HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(materialService.favoritePage(userId, pageNo, pageSize, materialType, language, tag, keyword));
    }

    @PostMapping("/favorite/{materialId}")
    public ApiResponse<Void> favorite(@PathVariable Long materialId,
                                      HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        materialService.favorite(userId, materialId);
        return ApiResponse.success("Favorited", null);
    }

    @DeleteMapping("/favorite/{materialId}")
    public ApiResponse<Void> unfavorite(@PathVariable Long materialId,
                                        HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        materialService.unfavorite(userId, materialId);
        return ApiResponse.success("Unfavorited", null);
    }

    @GetMapping("/detail/{materialId}")
    public ApiResponse<MaterialDetailVO> detail(@PathVariable Long materialId,
                                                HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(materialService.detail(userId, materialId));
    }
}