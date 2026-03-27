package com.codernote.platform.service;

import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.review.ReviewBatchPlanRequest;
import com.codernote.platform.dto.review.ReviewCenterOverviewVO;
import com.codernote.platform.dto.review.ReviewExecuteRequest;
import com.codernote.platform.dto.review.ReviewListItemVO;
import com.codernote.platform.dto.review.ReviewPlanRequest;
import com.codernote.platform.dto.review.ReviewSessionItemVO;
import com.codernote.platform.dto.review.ReviewStatusUpdateRequest;
import com.codernote.platform.dto.review.ReviewSummaryVO;
import com.codernote.platform.enums.ReviewContentType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ReviewService {

    ReviewSummaryVO summary(Long userId);

    ReviewCenterOverviewVO overview(Long userId);

    PageResult<ReviewListItemVO> page(Long userId,
                                      Long pageNo,
                                      Long pageSize,
                                      String category,
                                      String language,
                                      String tag,
                                      String contentType);

    List<ReviewSessionItemVO> sessionItems(Long userId, String category);

    void upsertPlan(Long userId, ReviewPlanRequest request);

    void batchUpsertPlan(Long userId, ReviewBatchPlanRequest request);

    void updateContentStatus(Long userId, ReviewStatusUpdateRequest request);

    void execute(Long userId, ReviewExecuteRequest request);

    void removePlan(Long userId, String contentType, Long contentId);

    void clearAllPlans(Long userId);

    void removePlanByContent(Long userId, ReviewContentType contentType, Long contentId);

    Map<Long, String> noteMasteryStatusMap(Long userId, List<Long> noteIds);

    Set<Long> contentIdsInPlan(Long userId, ReviewContentType contentType, List<Long> contentIds);

    void refreshTodayStatForUser(Long userId);

    void refreshTodayStatsForAllUsers();
}
