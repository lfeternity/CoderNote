package com.codernote.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.question.QuestionRichFieldPayload;
import com.codernote.platform.dto.review.ReviewBatchPlanRequest;
import com.codernote.platform.dto.review.ReviewCenterOverviewVO;
import com.codernote.platform.dto.review.ReviewExecuteRequest;
import com.codernote.platform.dto.review.ReviewListItemVO;
import com.codernote.platform.dto.review.ReviewPlanRequest;
import com.codernote.platform.dto.review.ReviewSessionItemVO;
import com.codernote.platform.dto.review.ReviewStatusUpdateRequest;
import com.codernote.platform.dto.review.ReviewSummaryVO;
import com.codernote.platform.entity.ErrorQuestion;
import com.codernote.platform.entity.NoteTag;
import com.codernote.platform.entity.QuestionTag;
import com.codernote.platform.entity.ReviewDailyStat;
import com.codernote.platform.entity.ReviewLog;
import com.codernote.platform.entity.ReviewPlan;
import com.codernote.platform.entity.StudyNote;
import com.codernote.platform.entity.Tag;
import com.codernote.platform.enums.MasteryStatus;
import com.codernote.platform.enums.ReviewAction;
import com.codernote.platform.enums.ReviewContentType;
import com.codernote.platform.enums.ReviewPlanMode;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.mapper.ErrorQuestionMapper;
import com.codernote.platform.mapper.NoteTagMapper;
import com.codernote.platform.mapper.QuestionTagMapper;
import com.codernote.platform.mapper.ReviewDailyStatMapper;
import com.codernote.platform.mapper.ReviewLogMapper;
import com.codernote.platform.mapper.ReviewPlanMapper;
import com.codernote.platform.mapper.StudyNoteMapper;
import com.codernote.platform.mapper.TagMapper;
import com.codernote.platform.service.ReviewService;
import com.codernote.platform.service.TagRelationHelperService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final List<Integer> AUTO_REVIEW_DAYS = Arrays.asList(1, 3, 7, 15, 30);
    private static final DateTimeFormatter POINT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TREND_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final ReviewPlanMapper reviewPlanMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final ReviewDailyStatMapper reviewDailyStatMapper;
    private final ErrorQuestionMapper questionMapper;
    private final StudyNoteMapper noteMapper;
    private final QuestionTagMapper questionTagMapper;
    private final NoteTagMapper noteTagMapper;
    private final TagMapper tagMapper;
    private final TagRelationHelperService tagRelationHelperService;
    private final ObjectMapper objectMapper;

    public ReviewServiceImpl(ReviewPlanMapper reviewPlanMapper,
                             ReviewLogMapper reviewLogMapper,
                             ReviewDailyStatMapper reviewDailyStatMapper,
                             ErrorQuestionMapper questionMapper,
                             StudyNoteMapper noteMapper,
                             QuestionTagMapper questionTagMapper,
                             NoteTagMapper noteTagMapper,
                             TagMapper tagMapper,
                             TagRelationHelperService tagRelationHelperService,
                             ObjectMapper objectMapper) {
        this.reviewPlanMapper = reviewPlanMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.reviewDailyStatMapper = reviewDailyStatMapper;
        this.questionMapper = questionMapper;
        this.noteMapper = noteMapper;
        this.questionTagMapper = questionTagMapper;
        this.noteTagMapper = noteTagMapper;
        this.tagMapper = tagMapper;
        this.tagRelationHelperService = tagRelationHelperService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReviewSummaryVO summary(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime todayStart = today.atStartOfDay();

        long questionDue = reviewPlanMapper.selectCount(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .eq(ReviewPlan::getContentType, ReviewContentType.QUESTION.name())
                .ne(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name())
                .lt(ReviewPlan::getNextReviewAt, tomorrowStart));
        long noteDue = reviewPlanMapper.selectCount(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .eq(ReviewPlan::getContentType, ReviewContentType.NOTE.name())
                .ne(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name())
                .lt(ReviewPlan::getNextReviewAt, tomorrowStart));
        long overdue = reviewPlanMapper.selectCount(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .ne(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name())
                .lt(ReviewPlan::getNextReviewAt, todayStart));
        long upcoming = reviewPlanMapper.selectCount(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .ne(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name())
                .ge(ReviewPlan::getNextReviewAt, tomorrowStart));
        long completed = reviewPlanMapper.selectCount(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .eq(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name()));

        ReviewSummaryVO vo = new ReviewSummaryVO();
        vo.setTodayQuestionCount(questionDue);
        vo.setTodayNoteCount(noteDue);
        vo.setTodayTotalCount(questionDue + noteDue);
        vo.setOverdueCount(overdue);
        vo.setUpcomingCount(upcoming);
        vo.setCompletedCount(completed);
        return vo;
    }

    @Override
    public ReviewCenterOverviewVO overview(Long userId) {
        refreshTodayStatForUser(userId);
        LocalDate today = LocalDate.now();
        ReviewDailyStat todayStat = getOrCreateTodayStat(userId, today, false);

        long totalReviewCount = reviewLogMapper.selectCount(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getUserId, userId));
        long totalPlanCount = reviewPlanMapper.selectCount(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId));
        long masteredPlanCount = reviewPlanMapper.selectCount(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .eq(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name()));

        ReviewCenterOverviewVO vo = new ReviewCenterOverviewVO();
        vo.setTodayDueTotal(todayStat == null ? 0L : Long.valueOf(resolveDailyDueTotal(todayStat)));
        vo.setTodayCompletedCount(todayStat == null ? 0L : Long.valueOf(nvl(todayStat.getCompletedCount())));
        vo.setTotalReviewCount(totalReviewCount);
        vo.setMasteryRate(calculateRate(masteredPlanCount, totalPlanCount));
        vo.setWeakTags(buildWeakTagList(userId));
        vo.setTrend(buildTrend(userId, today));
        return vo;
    }

    @Override
    public PageResult<ReviewListItemVO> page(Long userId,
                                             Long pageNo,
                                             Long pageSize,
                                             String category,
                                             String language,
                                             String tag,
                                             String contentType) {
        long safePageNo = pageNo == null || pageNo < 1 ? 1L : pageNo;
        long safePageSize = pageSize == null || pageSize < 1 ? 10L : pageSize;

        List<ReviewPlan> planList = loadPlanListByCategory(userId, category, contentType);
        if (planList.isEmpty()) {
            return PageResult.empty(safePageNo, safePageSize);
        }

        Map<Long, ErrorQuestion> questionMap = questionMapByPlan(planList, userId);
        Map<Long, StudyNote> noteMap = noteMapByPlan(planList, userId);

        List<Long> questionIds = new ArrayList<>(questionMap.keySet());
        List<Long> noteIds = new ArrayList<>(noteMap.keySet());
        Map<Long, List<String>> questionTagNameMap = questionIds.isEmpty()
                ? Collections.emptyMap()
                : tagRelationHelperService.questionTagNameMap(questionIds);
        Map<Long, List<String>> noteTagNameMap = noteIds.isEmpty()
                ? Collections.emptyMap()
                : tagRelationHelperService.noteTagNameMap(noteIds);

        String normalizedLanguage = StringUtils.hasText(language) ? language.trim() : "";
        String normalizedTag = StringUtils.hasText(tag) ? tag.trim() : "";
        List<ReviewListItemVO> allItems = new ArrayList<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        for (ReviewPlan plan : planList) {
            ReviewListItemVO item = new ReviewListItemVO();
            item.setPlanId(plan.getId());
            item.setContentType(plan.getContentType());
            item.setContentId(plan.getContentId());
            item.setMasteryStatus(plan.getMasteryStatus());
            item.setNextReviewAt(plan.getNextReviewAt());
            item.setLastReviewAt(plan.getLastReviewAt());
            item.setReviewCount(nvl(plan.getReviewCount()));
            item.setOverdue(plan.getNextReviewAt() != null
                    && plan.getNextReviewAt().isBefore(todayStart)
                    && !MasteryStatus.MASTERED.name().equals(plan.getMasteryStatus()));

            if (ReviewContentType.QUESTION.name().equals(plan.getContentType())) {
                ErrorQuestion question = questionMap.get(plan.getContentId());
                if (question == null) {
                    continue;
                }
                item.setTitle(question.getTitle());
                item.setLanguage(question.getLanguage());
                item.setTagNames(questionTagNameMap.getOrDefault(plan.getContentId(), Collections.emptyList()));
            } else {
                StudyNote noteEntity = noteMap.get(plan.getContentId());
                if (noteEntity == null) {
                    continue;
                }
                item.setTitle(noteEntity.getTitle());
                item.setLanguage(noteEntity.getLanguage());
                item.setTagNames(noteTagNameMap.getOrDefault(plan.getContentId(), Collections.emptyList()));
            }

            if (StringUtils.hasText(normalizedLanguage)
                    && !normalizedLanguage.equalsIgnoreCase(String.valueOf(item.getLanguage()))) {
                continue;
            }
            if (StringUtils.hasText(normalizedTag) && !containsIgnoreCase(item.getTagNames(), normalizedTag)) {
                continue;
            }
            allItems.add(item);
        }

        if (allItems.isEmpty()) {
            return PageResult.empty(safePageNo, safePageSize);
        }

        sortListItems(allItems, category);
        long total = allItems.size();
        int start = (int) ((safePageNo - 1) * safePageSize);
        if (start >= allItems.size()) {
            return new PageResult<>(total, safePageNo, safePageSize, Collections.emptyList());
        }
        int end = Math.min(allItems.size(), start + (int) safePageSize);
        return new PageResult<>(total, safePageNo, safePageSize, allItems.subList(start, end));
    }

    @Override
    public List<ReviewSessionItemVO> sessionItems(Long userId, String category) {
        String normalizedCategory = StringUtils.hasText(category) ? category.trim().toUpperCase() : "TODAY";
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<ReviewPlan> wrapper = new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .ne(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name());
        if ("OVERDUE".equals(normalizedCategory)) {
            wrapper.lt(ReviewPlan::getNextReviewAt, start);
        } else if ("UPCOMING".equals(normalizedCategory)) {
            wrapper.ge(ReviewPlan::getNextReviewAt, tomorrowStart);
        } else {
            wrapper.lt(ReviewPlan::getNextReviewAt, tomorrowStart);
        }
        wrapper.orderByAsc(ReviewPlan::getNextReviewAt).orderByAsc(ReviewPlan::getId);

        List<ReviewPlan> plans = reviewPlanMapper.selectList(wrapper);
        if (plans.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ErrorQuestion> questionMap = questionMapByPlan(plans, userId);
        Map<Long, StudyNote> noteMap = noteMapByPlan(plans, userId);
        Map<Long, List<String>> questionTagNameMap = questionMap.isEmpty()
                ? Collections.emptyMap()
                : tagRelationHelperService.questionTagNameMap(questionMap.keySet());
        Map<Long, List<String>> noteTagNameMap = noteMap.isEmpty()
                ? Collections.emptyMap()
                : tagRelationHelperService.noteTagNameMap(noteMap.keySet());

        List<ReviewSessionItemVO> items = new ArrayList<>();
        for (ReviewPlan plan : plans) {
            ReviewSessionItemVO item = new ReviewSessionItemVO();
            item.setPlanId(plan.getId());
            item.setContentType(plan.getContentType());
            item.setContentId(plan.getContentId());
            item.setMasteryStatus(plan.getMasteryStatus());
            item.setNextReviewAt(plan.getNextReviewAt());
            item.setReviewCount(nvl(plan.getReviewCount()));

            if (ReviewContentType.QUESTION.name().equals(plan.getContentType())) {
                ErrorQuestion question = questionMap.get(plan.getContentId());
                if (question == null) {
                    continue;
                }
                item.setTitle(question.getTitle());
                item.setLanguage(question.getLanguage());
                item.setTagNames(questionTagNameMap.getOrDefault(question.getId(), Collections.emptyList()));
                item.setQuestionBody(extractRichText(question.getErrorCode()));
                item.setAnswerBody(extractRichText(question.getCorrectCode()));
                StringBuilder explain = new StringBuilder();
                if (StringUtils.hasText(question.getErrorReason())) {
                    explain.append(question.getErrorReason().trim());
                }
                if (StringUtils.hasText(question.getSolution())) {
                    if (explain.length() > 0) {
                        explain.append("\n\n");
                    }
                    explain.append(question.getSolution().trim());
                }
                item.setAnswerExplain(explain.toString());
            } else {
                StudyNote note = noteMap.get(plan.getContentId());
                if (note == null) {
                    continue;
                }
                item.setTitle(note.getTitle());
                item.setLanguage(note.getLanguage());
                item.setTagNames(noteTagNameMap.getOrDefault(note.getId(), Collections.emptyList()));
                item.setNoteContent(note.getContent());
            }

            items.add(item);
        }
        return items;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsertPlan(Long userId, ReviewPlanRequest request) {
        if (!ReviewContentType.isValid(request.getContentType())) {
            throw new BizException(400, "Invalid content type");
        }
        ReviewContentType contentType = ReviewContentType.valueOf(request.getContentType().trim());
        upsertPlanInternal(userId, contentType, request.getContentId(), request.getPlanMode(), request.getManualReviewDates());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpsertPlan(Long userId, ReviewBatchPlanRequest request) {
        if (!ReviewContentType.isValid(request.getContentType())) {
            throw new BizException(400, "Invalid content type");
        }
        ReviewContentType contentType = ReviewContentType.valueOf(request.getContentType().trim());
        if (CollectionUtils.isEmpty(request.getContentIds())) {
            return;
        }
        LinkedHashSet<Long> dedupIds = request.getContentIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long contentId : dedupIds) {
            upsertPlanInternal(userId, contentType, contentId, request.getPlanMode(), request.getManualReviewDates());
        }
        refreshTodayStatForUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContentStatus(Long userId, ReviewStatusUpdateRequest request) {
        if (!ReviewContentType.isValid(request.getContentType())) {
            throw new BizException(400, "Invalid content type");
        }
        String masteryStatus = normalizeMasteryStatus(request.getMasteryStatus());
        if (!MasteryStatus.isValid(masteryStatus)) {
            throw new BizException(400, "Invalid mastery status");
        }

        ReviewContentType contentType = ReviewContentType.valueOf(request.getContentType().trim());
        ensureOwnedContent(userId, contentType, request.getContentId());

        ReviewPlan plan = getPlanByContent(userId, contentType.name(), request.getContentId());
        LocalDateTime now = LocalDateTime.now();
        if (MasteryStatus.REVIEWING.name().equals(masteryStatus)) {
            if (plan == null) {
                upsertPlanInternal(userId, contentType, request.getContentId(), ReviewPlanMode.AUTO.name(), Collections.emptyList());
                return;
            }
            plan.setMasteryStatus(MasteryStatus.REVIEWING.name());
            plan.setMasteredAt(null);
            if (plan.getNextReviewAt() == null) {
                List<LocalDateTime> points = parseReviewPoints(plan.getReviewPoints());
                if (points.isEmpty()) {
                    points = buildAutoReviewPoints(now);
                    plan.setReviewPoints(formatReviewPoints(points));
                    plan.setNextPointIndex(0);
                }
                plan.setNextReviewAt(points.get(Math.max(0, nvl(plan.getNextPointIndex()))));
            }
            plan.setUpdatedAt(now);
            reviewPlanMapper.updateById(plan);
        } else if (MasteryStatus.MASTERED.name().equals(masteryStatus)) {
            if (plan == null) {
                plan = new ReviewPlan();
                plan.setUserId(userId);
                plan.setContentType(contentType.name());
                plan.setContentId(request.getContentId());
                plan.setPlanMode(ReviewPlanMode.AUTO.name());
                plan.setReviewPoints("");
                plan.setNextPointIndex(0);
                plan.setReviewCount(0);
                plan.setCreatedAt(now);
            }
            markPlanMastered(plan, now, false);
            plan.setUpdatedAt(now);
            if (plan.getId() == null) {
                reviewPlanMapper.insert(plan);
            } else {
                reviewPlanMapper.updateById(plan);
            }
        } else {
            if (plan != null) {
                plan.setMasteryStatus(MasteryStatus.NOT_MASTERED.name());
                plan.setMasteredAt(null);
                if (plan.getNextReviewAt() == null) {
                    List<LocalDateTime> points = parseReviewPoints(plan.getReviewPoints());
                    if (points.isEmpty()) {
                        points = buildAutoReviewPoints(now);
                        plan.setReviewPoints(formatReviewPoints(points));
                        plan.setNextPointIndex(0);
                    }
                    plan.setNextReviewAt(points.get(Math.max(0, nvl(plan.getNextPointIndex()))));
                }
                plan.setUpdatedAt(now);
                reviewPlanMapper.updateById(plan);
            }
        }

        syncQuestionMastery(userId, contentType, request.getContentId(), masteryStatus);
        refreshTodayStatForUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(Long userId, ReviewExecuteRequest request) {
        String action = StringUtils.hasText(request.getAction())
                ? request.getAction().trim().toUpperCase()
                : "";
        if (!ReviewAction.isValid(action)) {
            throw new BizException(400, "Invalid review action");
        }

        ReviewPlan plan = reviewPlanMapper.selectOne(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getId, request.getPlanId())
                .eq(ReviewPlan::getUserId, userId)
                .last("limit 1"));
        if (plan == null) {
            throw new BizException(404, "Review plan not found");
        }

        String beforeStatus = plan.getMasteryStatus();
        LocalDateTime beforeNext = plan.getNextReviewAt();
        LocalDateTime now = LocalDateTime.now();

        if (ReviewAction.MASTERED.name().equals(action)) {
            markPlanMastered(plan, now, true);
        } else if (ReviewAction.CONTINUE.name().equals(action)) {
            continuePlan(plan, now);
        } else {
            postponePlan(plan, now);
        }
        plan.setUpdatedAt(now);
        reviewPlanMapper.updateById(plan);
        syncQuestionMastery(userId, ReviewContentType.valueOf(plan.getContentType()), plan.getContentId(), plan.getMasteryStatus());

        ReviewLog log = new ReviewLog();
        log.setUserId(userId);
        log.setPlanId(plan.getId());
        log.setContentType(plan.getContentType());
        log.setContentId(plan.getContentId());
        log.setAction(action);
        log.setMasteryStatusBefore(beforeStatus);
        log.setMasteryStatusAfter(plan.getMasteryStatus());
        log.setNextReviewBefore(beforeNext);
        log.setNextReviewAfter(plan.getNextReviewAt());
        log.setReviewedAt(now);
        log.setCreatedAt(now);
        reviewLogMapper.insert(log);

        increaseTodayCompletedCount(userId);
        refreshTodayStatForUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePlan(Long userId, String contentType, Long contentId) {
        if (!ReviewContentType.isValid(contentType)) {
            throw new BizException(400, "Invalid content type");
        }
        ReviewContentType type = ReviewContentType.valueOf(contentType.trim());
        removePlanByContent(userId, type, contentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllPlans(Long userId) {
        List<ReviewPlan> plans = reviewPlanMapper.selectList(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .eq(ReviewPlan::getContentType, ReviewContentType.QUESTION.name())
                .ne(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name()));
        for (ReviewPlan plan : plans) {
            syncQuestionMastery(userId, ReviewContentType.QUESTION, plan.getContentId(), MasteryStatus.NOT_MASTERED.name());
        }
        reviewPlanMapper.delete(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId));
        refreshTodayStatForUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePlanByContent(Long userId, ReviewContentType contentType, Long contentId) {
        if (contentType == ReviewContentType.QUESTION) {
            syncQuestionMastery(userId, contentType, contentId, MasteryStatus.NOT_MASTERED.name());
        }
        reviewPlanMapper.delete(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .eq(ReviewPlan::getContentType, contentType.name())
                .eq(ReviewPlan::getContentId, contentId));
        refreshTodayStatForUser(userId);
    }

    @Override
    public Map<Long, String> noteMasteryStatusMap(Long userId, List<Long> noteIds) {
        if (CollectionUtils.isEmpty(noteIds)) {
            return Collections.emptyMap();
        }
        List<ReviewPlan> plans = reviewPlanMapper.selectList(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .eq(ReviewPlan::getContentType, ReviewContentType.NOTE.name())
                .in(ReviewPlan::getContentId, noteIds));
        Map<Long, String> map = new HashMap<>();
        for (ReviewPlan plan : plans) {
            map.put(plan.getContentId(), plan.getMasteryStatus());
        }
        return map;
    }

    @Override
    public Set<Long> contentIdsInPlan(Long userId, ReviewContentType contentType, List<Long> contentIds) {
        if (CollectionUtils.isEmpty(contentIds)) {
            return Collections.emptySet();
        }
        List<ReviewPlan> plans = reviewPlanMapper.selectList(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .eq(ReviewPlan::getContentType, contentType.name())
                .in(ReviewPlan::getContentId, contentIds));
        if (plans.isEmpty()) {
            return Collections.emptySet();
        }
        return plans.stream()
                .map(ReviewPlan::getContentId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshTodayStatForUser(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        int questionDue = reviewPlanMapper.selectCount(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .eq(ReviewPlan::getContentType, ReviewContentType.QUESTION.name())
                .ne(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name())
                .lt(ReviewPlan::getNextReviewAt, tomorrowStart)).intValue();
        int noteDue = reviewPlanMapper.selectCount(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .eq(ReviewPlan::getContentType, ReviewContentType.NOTE.name())
                .ne(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name())
                .lt(ReviewPlan::getNextReviewAt, tomorrowStart)).intValue();

        ReviewDailyStat todayStat = getOrCreateTodayStat(userId, today, true);
        int completedCount = nvl(todayStat.getCompletedCount());
        todayStat.setDueQuestionCount(questionDue);
        todayStat.setDueNoteCount(noteDue);
        // Keep today's denominator stable while reviews are completed during the day.
        // `due_total` is treated as "today total workload", not "remaining due count".
        todayStat.setDueTotal(questionDue + noteDue + completedCount);
        todayStat.setUpdatedAt(LocalDateTime.now());
        reviewDailyStatMapper.updateById(todayStat);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshTodayStatsForAllUsers() {
        List<Object> userIds = reviewPlanMapper.selectObjs(new QueryWrapper<ReviewPlan>().select("distinct user_id"));
        for (Object value : userIds) {
            if (value == null) {
                continue;
            }
            Long userId;
            if (value instanceof Number) {
                userId = ((Number) value).longValue();
            } else {
                userId = Long.valueOf(String.valueOf(value));
            }
            refreshTodayStatForUser(userId);
        }
    }

    private void upsertPlanInternal(Long userId,
                                    ReviewContentType contentType,
                                    Long contentId,
                                    String planMode,
                                    List<String> manualReviewDates) {
        ensureOwnedContent(userId, contentType, contentId);

        String normalizedPlanMode = normalizePlanMode(planMode);
        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime> reviewPoints = ReviewPlanMode.MANUAL.name().equals(normalizedPlanMode)
                ? buildManualReviewPoints(manualReviewDates, now)
                : buildAutoReviewPoints(now);
        if (reviewPoints.isEmpty()) {
            throw new BizException(400, "Review points are required");
        }

        ReviewPlan plan = getPlanByContent(userId, contentType.name(), contentId);
        if (plan == null) {
            plan = new ReviewPlan();
            plan.setUserId(userId);
            plan.setContentType(contentType.name());
            plan.setContentId(contentId);
            plan.setReviewCount(0);
            plan.setCreatedAt(now);
            plan.setLastReviewAt(null);
        }
        plan.setMasteryStatus(MasteryStatus.REVIEWING.name());
        plan.setPlanMode(normalizedPlanMode);
        plan.setReviewPoints(formatReviewPoints(reviewPoints));
        plan.setNextPointIndex(0);
        plan.setNextReviewAt(reviewPoints.get(0));
        plan.setMasteredAt(null);
        plan.setUpdatedAt(now);

        if (plan.getId() == null) {
            reviewPlanMapper.insert(plan);
        } else {
            reviewPlanMapper.updateById(plan);
        }

        syncQuestionMastery(userId, contentType, contentId, MasteryStatus.REVIEWING.name());
        refreshTodayStatForUser(userId);
    }

    private String normalizePlanMode(String planMode) {
        String value = StringUtils.hasText(planMode) ? planMode.trim().toUpperCase() : ReviewPlanMode.AUTO.name();
        if (!ReviewPlanMode.isValid(value)) {
            throw new BizException(400, "Invalid plan mode");
        }
        return value;
    }

    private String normalizeMasteryStatus(String masteryStatus) {
        if (!StringUtils.hasText(masteryStatus)) {
            return "";
        }
        return masteryStatus.trim().toUpperCase();
    }

    private void ensureOwnedContent(Long userId, ReviewContentType contentType, Long contentId) {
        if (contentId == null) {
            throw new BizException(400, "contentId is required");
        }
        if (contentType == ReviewContentType.QUESTION) {
            ErrorQuestion question = questionMapper.selectOne(new LambdaQueryWrapper<ErrorQuestion>()
                    .eq(ErrorQuestion::getId, contentId)
                    .eq(ErrorQuestion::getUserId, userId)
                    .last("limit 1"));
            if (question == null) {
                throw new BizException(404, "Question not found");
            }
            return;
        }

        StudyNote note = noteMapper.selectOne(new LambdaQueryWrapper<StudyNote>()
                .eq(StudyNote::getId, contentId)
                .eq(StudyNote::getUserId, userId)
                .last("limit 1"));
        if (note == null) {
            throw new BizException(404, "Note not found");
        }
    }

    private List<ReviewPlan> loadPlanListByCategory(Long userId, String category, String contentType) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<ReviewPlan> wrapper = new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId);
        if (StringUtils.hasText(contentType)) {
            String type = contentType.trim().toUpperCase();
            if (!ReviewContentType.isValid(type)) {
                throw new BizException(400, "Invalid content type");
            }
            wrapper.eq(ReviewPlan::getContentType, type);
        }

        String normalizedCategory = StringUtils.hasText(category) ? category.trim().toUpperCase() : "TODAY";
        if ("COMPLETED".equals(normalizedCategory)) {
            wrapper.eq(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name())
                    .orderByDesc(ReviewPlan::getMasteredAt)
                    .orderByDesc(ReviewPlan::getUpdatedAt);
        } else if ("OVERDUE".equals(normalizedCategory)) {
            wrapper.ne(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name())
                    .lt(ReviewPlan::getNextReviewAt, start)
                    .orderByAsc(ReviewPlan::getNextReviewAt);
        } else if ("UPCOMING".equals(normalizedCategory)) {
            wrapper.ne(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name())
                    .ge(ReviewPlan::getNextReviewAt, tomorrowStart)
                    .orderByAsc(ReviewPlan::getNextReviewAt);
        } else {
            wrapper.ne(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name())
                    .lt(ReviewPlan::getNextReviewAt, tomorrowStart)
                    .orderByAsc(ReviewPlan::getNextReviewAt);
        }

        return reviewPlanMapper.selectList(wrapper);
    }

    private Map<Long, ErrorQuestion> questionMapByPlan(List<ReviewPlan> planList, Long userId) {
        List<Long> questionIds = planList.stream()
                .filter(plan -> ReviewContentType.QUESTION.name().equals(plan.getContentType()))
                .map(ReviewPlan::getContentId)
                .collect(Collectors.toList());
        if (questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return questionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                        .eq(ErrorQuestion::getUserId, userId)
                        .in(ErrorQuestion::getId, questionIds))
                .stream()
                .collect(Collectors.toMap(ErrorQuestion::getId, item -> item));
    }

    private Map<Long, StudyNote> noteMapByPlan(List<ReviewPlan> planList, Long userId) {
        List<Long> noteIds = planList.stream()
                .filter(plan -> ReviewContentType.NOTE.name().equals(plan.getContentType()))
                .map(ReviewPlan::getContentId)
                .collect(Collectors.toList());
        if (noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return noteMapper.selectList(new LambdaQueryWrapper<StudyNote>()
                        .eq(StudyNote::getUserId, userId)
                        .in(StudyNote::getId, noteIds))
                .stream()
                .collect(Collectors.toMap(StudyNote::getId, item -> item));
    }

    private void sortListItems(List<ReviewListItemVO> list, String category) {
        String normalized = StringUtils.hasText(category) ? category.trim().toUpperCase() : "TODAY";
        if ("COMPLETED".equals(normalized)) {
            list.sort(Comparator.comparing(ReviewListItemVO::getLastReviewAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return;
        }
        if ("UPCOMING".equals(normalized)) {
            list.sort(Comparator.comparing(ReviewListItemVO::getNextReviewAt,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            return;
        }

        list.sort((a, b) -> {
            int overdueCompare = Boolean.compare(Boolean.TRUE.equals(b.getOverdue()), Boolean.TRUE.equals(a.getOverdue()));
            if (overdueCompare != 0) {
                return overdueCompare;
            }
            LocalDateTime left = a.getNextReviewAt();
            LocalDateTime right = b.getNextReviewAt();
            if (left == null && right == null) {
                return 0;
            }
            if (left == null) {
                return 1;
            }
            if (right == null) {
                return -1;
            }
            return left.compareTo(right);
        });
    }

    private boolean containsIgnoreCase(List<String> values, String expected) {
        if (CollectionUtils.isEmpty(values) || !StringUtils.hasText(expected)) {
            return false;
        }
        String normalized = expected.trim().toLowerCase();
        for (String value : values) {
            if (value != null && value.trim().toLowerCase().contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private List<LocalDateTime> buildAutoReviewPoints(LocalDateTime base) {
        LocalDateTime normalizedBase = base == null ? LocalDateTime.now() : base.withNano(0);
        List<LocalDateTime> points = new ArrayList<>();
        for (Integer day : AUTO_REVIEW_DAYS) {
            points.add(normalizedBase.plusDays(day));
        }
        return points;
    }

    private List<LocalDateTime> buildManualReviewPoints(List<String> manualReviewDates, LocalDateTime now) {
        if (CollectionUtils.isEmpty(manualReviewDates)) {
            throw new BizException(400, "Manual plan requires dates");
        }
        Set<LocalDateTime> dedup = new LinkedHashSet<>();
        for (String value : manualReviewDates) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            try {
                LocalDate date = LocalDate.parse(value.trim());
                LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.of(9, 0));
                if (dateTime.isBefore(now.minusMinutes(1))) {
                    continue;
                }
                dedup.add(dateTime);
            } catch (DateTimeParseException ex) {
                throw new BizException(400, "Invalid manual date format");
            }
        }
        if (dedup.isEmpty()) {
            throw new BizException(400, "Manual dates must be in the future");
        }
        return dedup.stream().sorted().collect(Collectors.toList());
    }

    private String formatReviewPoints(List<LocalDateTime> points) {
        if (CollectionUtils.isEmpty(points)) {
            return "";
        }
        return points.stream()
                .map(point -> POINT_FORMATTER.format(point.withNano(0)))
                .collect(Collectors.joining(","));
    }

    private List<LocalDateTime> parseReviewPoints(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        String[] parts = raw.split(",");
        List<LocalDateTime> list = new ArrayList<>();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            try {
                list.add(LocalDateTime.parse(part.trim(), POINT_FORMATTER));
            } catch (DateTimeParseException ignored) {
                // ignore malformed points
            }
        }
        list.sort(Comparator.naturalOrder());
        return list;
    }

    private void continuePlan(ReviewPlan plan, LocalDateTime now) {
        plan.setReviewCount(nvl(plan.getReviewCount()) + 1);
        plan.setLastReviewAt(now);

        List<LocalDateTime> points = parseReviewPoints(plan.getReviewPoints());
        if (points.isEmpty()) {
            points = buildAutoReviewPoints(now);
        }
        int nextIndex = nvl(plan.getNextPointIndex()) + 1;
        if (nextIndex >= points.size()) {
            plan.setReviewPoints(formatReviewPoints(points));
            markPlanMastered(plan, now, false);
            return;
        }

        LocalDateTime next = points.get(nextIndex);
        if (!next.isAfter(now)) {
            next = now.plusDays(1);
            points.set(nextIndex, next);
        }
        plan.setMasteryStatus(MasteryStatus.REVIEWING.name());
        plan.setMasteredAt(null);
        plan.setNextPointIndex(nextIndex);
        plan.setNextReviewAt(next);
        plan.setReviewPoints(formatReviewPoints(points));
    }

    private void postponePlan(ReviewPlan plan, LocalDateTime now) {
        plan.setReviewCount(nvl(plan.getReviewCount()) + 1);
        plan.setLastReviewAt(now);
        plan.setMasteryStatus(MasteryStatus.REVIEWING.name());
        plan.setMasteredAt(null);

        List<LocalDateTime> points = parseReviewPoints(plan.getReviewPoints());
        if (points.isEmpty()) {
            points = buildAutoReviewPoints(now);
            plan.setNextPointIndex(0);
        }

        int index = Math.max(0, Math.min(nvl(plan.getNextPointIndex()), points.size() - 1));
        LocalDateTime base = plan.getNextReviewAt();
        if (base == null || !base.isAfter(now)) {
            base = now;
        }
        LocalDateTime next = base.plusDays(1);
        points.set(index, next);
        plan.setNextPointIndex(index);
        plan.setNextReviewAt(next);
        plan.setReviewPoints(formatReviewPoints(points));
    }

    private void markPlanMastered(ReviewPlan plan, LocalDateTime now, boolean increaseCount) {
        if (increaseCount) {
            plan.setReviewCount(nvl(plan.getReviewCount()) + 1);
        }
        plan.setMasteryStatus(MasteryStatus.MASTERED.name());
        plan.setNextReviewAt(null);
        plan.setMasteredAt(now);
        plan.setLastReviewAt(now);
        List<LocalDateTime> points = parseReviewPoints(plan.getReviewPoints());
        if (!points.isEmpty()) {
            plan.setNextPointIndex(points.size());
            plan.setReviewPoints(formatReviewPoints(points));
        } else {
            plan.setNextPointIndex(0);
            plan.setReviewPoints("");
        }
    }

    private void syncQuestionMastery(Long userId, ReviewContentType contentType, Long contentId, String masteryStatus) {
        if (contentType != ReviewContentType.QUESTION) {
            return;
        }
        ErrorQuestion question = questionMapper.selectOne(new LambdaQueryWrapper<ErrorQuestion>()
                .eq(ErrorQuestion::getId, contentId)
                .eq(ErrorQuestion::getUserId, userId)
                .last("limit 1"));
        if (question == null) {
            return;
        }
        question.setMasteryStatus(masteryStatus);
        question.setUpdatedAt(LocalDateTime.now());
        questionMapper.updateById(question);
    }

    private ReviewPlan getPlanByContent(Long userId, String contentType, Long contentId) {
        return reviewPlanMapper.selectOne(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .eq(ReviewPlan::getContentType, contentType)
                .eq(ReviewPlan::getContentId, contentId)
                .last("limit 1"));
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private ReviewDailyStat getOrCreateTodayStat(Long userId, LocalDate statDate, boolean autoCreate) {
        ReviewDailyStat stat = reviewDailyStatMapper.selectOne(new LambdaQueryWrapper<ReviewDailyStat>()
                .eq(ReviewDailyStat::getUserId, userId)
                .eq(ReviewDailyStat::getStatDate, statDate)
                .last("limit 1"));
        if (stat != null || !autoCreate) {
            return stat;
        }

        stat = new ReviewDailyStat();
        stat.setUserId(userId);
        stat.setStatDate(statDate);
        stat.setDueTotal(0);
        stat.setDueQuestionCount(0);
        stat.setDueNoteCount(0);
        stat.setCompletedCount(0);
        stat.setCreatedAt(LocalDateTime.now());
        stat.setUpdatedAt(LocalDateTime.now());
        reviewDailyStatMapper.insert(stat);
        return stat;
    }

    private void increaseTodayCompletedCount(Long userId) {
        ReviewDailyStat todayStat = getOrCreateTodayStat(userId, LocalDate.now(), true);
        todayStat.setCompletedCount(nvl(todayStat.getCompletedCount()) + 1);
        todayStat.setUpdatedAt(LocalDateTime.now());
        reviewDailyStatMapper.updateById(todayStat);
    }

    private List<ReviewCenterOverviewVO.WeakTagItem> buildWeakTagList(Long userId) {
        List<ReviewPlan> pendingPlans = reviewPlanMapper.selectList(new LambdaQueryWrapper<ReviewPlan>()
                .eq(ReviewPlan::getUserId, userId)
                .ne(ReviewPlan::getMasteryStatus, MasteryStatus.MASTERED.name()));
        if (pendingPlans.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> questionIds = pendingPlans.stream()
                .filter(item -> ReviewContentType.QUESTION.name().equals(item.getContentType()))
                .map(ReviewPlan::getContentId)
                .collect(Collectors.toList());
        List<Long> noteIds = pendingPlans.stream()
                .filter(item -> ReviewContentType.NOTE.name().equals(item.getContentType()))
                .map(ReviewPlan::getContentId)
                .collect(Collectors.toList());

        Map<Long, Long> counter = new HashMap<>();
        if (!questionIds.isEmpty()) {
            List<QuestionTag> questionTags = questionTagMapper.selectList(new LambdaQueryWrapper<QuestionTag>()
                    .in(QuestionTag::getQuestionId, questionIds));
            for (QuestionTag relation : questionTags) {
                counter.put(relation.getTagId(), counter.getOrDefault(relation.getTagId(), 0L) + 1);
            }
        }
        if (!noteIds.isEmpty()) {
            List<NoteTag> noteTags = noteTagMapper.selectList(new LambdaQueryWrapper<NoteTag>()
                    .in(NoteTag::getNoteId, noteIds));
            for (NoteTag relation : noteTags) {
                counter.put(relation.getTagId(), counter.getOrDefault(relation.getTagId(), 0L) + 1);
            }
        }
        if (counter.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> tagNameMap = tagMapper.selectBatchIds(counter.keySet())
                .stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));

        return counter.entrySet().stream()
                .map(entry -> {
                    ReviewCenterOverviewVO.WeakTagItem item = new ReviewCenterOverviewVO.WeakTagItem();
                    item.setTagId(entry.getKey());
                    item.setTagName(tagNameMap.getOrDefault(entry.getKey(), "未知标签"));
                    item.setPendingCount(entry.getValue());
                    return item;
                })
                .sorted((a, b) -> Long.compare(b.getPendingCount(), a.getPendingCount()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<ReviewCenterOverviewVO.TrendItem> buildTrend(Long userId, LocalDate today) {
        LocalDate start = today.minusDays(6);
        List<ReviewDailyStat> stats = reviewDailyStatMapper.selectList(new LambdaQueryWrapper<ReviewDailyStat>()
                .eq(ReviewDailyStat::getUserId, userId)
                .between(ReviewDailyStat::getStatDate, start, today));
        Map<LocalDate, ReviewDailyStat> statMap = new LinkedHashMap<>();
        for (ReviewDailyStat stat : stats) {
            statMap.put(stat.getStatDate(), stat);
        }

        List<ReviewCenterOverviewVO.TrendItem> list = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            ReviewDailyStat stat = statMap.get(date);
            ReviewCenterOverviewVO.TrendItem item = new ReviewCenterOverviewVO.TrendItem();
            item.setDate(TREND_DATE_FORMATTER.format(date));
            item.setDueTotal(stat == null ? 0L : Long.valueOf(resolveDailyDueTotal(stat)));
            item.setCompletedCount(stat == null ? 0L : Long.valueOf(nvl(stat.getCompletedCount())));
            list.add(item);
        }
        return list;
    }

    private int resolveDailyDueTotal(ReviewDailyStat stat) {
        if (stat == null) {
            return 0;
        }
        // Historical rows may contain remaining due count, while completed_count is cumulative.
        // Guard against invalid "1/0" style combinations.
        return Math.max(nvl(stat.getDueTotal()), nvl(stat.getCompletedCount()));
    }

    private Double calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        BigDecimal rate = BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
        return rate.doubleValue();
    }

    private String extractRichText(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return "";
        }
        String trimmed = rawValue.trim();
        if (!(trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            return rawValue;
        }
        try {
            QuestionRichFieldPayload payload = objectMapper.readValue(trimmed, QuestionRichFieldPayload.class);
            if (payload == null || !StringUtils.hasText(payload.getText())) {
                return "";
            }
            return payload.getText();
        } catch (Exception ex) {
            return rawValue;
        }
    }
}
