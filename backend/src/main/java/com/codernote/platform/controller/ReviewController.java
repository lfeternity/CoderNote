package com.codernote.platform.controller;

import com.codernote.platform.common.ApiResponse;
import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.review.ReviewBatchPlanRequest;
import com.codernote.platform.dto.review.ReviewCenterOverviewVO;
import com.codernote.platform.dto.review.ReviewExecuteRequest;
import com.codernote.platform.dto.review.ReviewListItemVO;
import com.codernote.platform.dto.review.ReviewPlanRequest;
import com.codernote.platform.dto.review.ReviewSessionItemVO;
import com.codernote.platform.dto.review.ReviewStatusUpdateRequest;
import com.codernote.platform.dto.review.ReviewSummaryVO;
import com.codernote.platform.security.AuthContext;
import com.codernote.platform.service.ReviewService;
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
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/summary")
    public ApiResponse<ReviewSummaryVO> summary(HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(reviewService.summary(userId));
    }

    @GetMapping("/overview")
    public ApiResponse<ReviewCenterOverviewVO> overview(HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(reviewService.overview(userId));
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<ReviewListItemVO>> list(@RequestParam(defaultValue = "1") Long pageNo,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) String category,
                                                           @RequestParam(required = false) String language,
                                                           @RequestParam(required = false) String tag,
                                                           @RequestParam(required = false) String contentType,
                                                           HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(reviewService.page(userId, pageNo, pageSize, category, language, tag, contentType));
    }

    @GetMapping("/session-items")
    public ApiResponse<List<ReviewSessionItemVO>> sessionItems(@RequestParam(required = false) String category,
                                                               HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(reviewService.sessionItems(userId, category));
    }

    @PostMapping("/plan")
    public ApiResponse<Void> upsertPlan(@Valid @RequestBody ReviewPlanRequest request,
                                        HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        reviewService.upsertPlan(userId, request);
        return ApiResponse.success("Review plan updated", null);
    }

    @PostMapping("/plan/batch")
    public ApiResponse<Void> batchPlan(@Valid @RequestBody ReviewBatchPlanRequest request,
                                       HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        reviewService.batchUpsertPlan(userId, request);
        return ApiResponse.success("Batch review plan updated", null);
    }

    @PutMapping("/status")
    public ApiResponse<Void> updateStatus(@Valid @RequestBody ReviewStatusUpdateRequest request,
                                          HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        reviewService.updateContentStatus(userId, request);
        return ApiResponse.success("Review status updated", null);
    }

    @PostMapping("/execute")
    public ApiResponse<Void> execute(@Valid @RequestBody ReviewExecuteRequest request,
                                     HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        reviewService.execute(userId, request);
        return ApiResponse.success("Review result recorded", null);
    }

    @DeleteMapping("/plan/{contentType}/{contentId}")
    public ApiResponse<Void> removePlan(@PathVariable String contentType,
                                        @PathVariable Long contentId,
                                        HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        reviewService.removePlan(userId, contentType, contentId);
        return ApiResponse.success("Review plan removed", null);
    }

    @DeleteMapping("/plan/clear")
    public ApiResponse<Void> clearAll(HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        reviewService.clearAllPlans(userId);
        return ApiResponse.success("All review plans cleared", null);
    }
}
