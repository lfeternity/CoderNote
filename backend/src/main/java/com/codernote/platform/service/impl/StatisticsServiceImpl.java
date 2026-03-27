package com.codernote.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codernote.platform.dto.review.ReviewCenterOverviewVO;
import com.codernote.platform.dto.statistics.StatisticsOverviewVO;
import com.codernote.platform.entity.ErrorQuestion;
import com.codernote.platform.entity.QuestionTag;
import com.codernote.platform.entity.StudyMaterial;
import com.codernote.platform.entity.Tag;
import com.codernote.platform.mapper.ErrorQuestionMapper;
import com.codernote.platform.mapper.QuestionTagMapper;
import com.codernote.platform.mapper.StudyMaterialMapper;
import com.codernote.platform.mapper.TagMapper;
import com.codernote.platform.service.ReviewService;
import com.codernote.platform.service.StatisticsService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

        private final ErrorQuestionMapper questionMapper;
        private final StudyMaterialMapper materialMapper;
        private final QuestionTagMapper questionTagMapper;
        private final TagMapper tagMapper;
        private final ReviewService reviewService;

        public StatisticsServiceImpl(ErrorQuestionMapper questionMapper,
                        StudyMaterialMapper materialMapper,
                        QuestionTagMapper questionTagMapper,
                        TagMapper tagMapper,
                        ReviewService reviewService) {
                this.questionMapper = questionMapper;
                this.materialMapper = materialMapper;
                this.questionTagMapper = questionTagMapper;
                this.tagMapper = tagMapper;
                this.reviewService = reviewService;
        }

        @Override
        public StatisticsOverviewVO overview(Long userId) {
                StatisticsOverviewVO vo = new StatisticsOverviewVO();

                long questionTotal = questionMapper.selectCount(new LambdaQueryWrapper<ErrorQuestion>()
                                .eq(ErrorQuestion::getUserId, userId));
                long masteredCount = questionMapper.selectCount(new LambdaQueryWrapper<ErrorQuestion>()
                                .eq(ErrorQuestion::getUserId, userId)
                                .eq(ErrorQuestion::getMasteryStatus, "MASTERED"));
                long reviewingCount = questionMapper.selectCount(new LambdaQueryWrapper<ErrorQuestion>()
                                .eq(ErrorQuestion::getUserId, userId)
                                .eq(ErrorQuestion::getMasteryStatus, "REVIEWING"));
                long notMasteredCount = questionMapper.selectCount(new LambdaQueryWrapper<ErrorQuestion>()
                                .eq(ErrorQuestion::getUserId, userId)
                                .eq(ErrorQuestion::getMasteryStatus, "NOT_MASTERED"));
                long materialTotal = materialMapper.selectCount(new LambdaQueryWrapper<StudyMaterial>()
                                .eq(StudyMaterial::getUserId, userId));

                vo.setQuestionTotal(questionTotal);
                vo.setMasteredQuestionCount(masteredCount);
                vo.setReviewingQuestionCount(reviewingCount);
                vo.setNotMasteredQuestionCount(notMasteredCount);
                vo.setMaterialTotal(materialTotal);

                List<ErrorQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                                .eq(ErrorQuestion::getUserId, userId)
                                .select(ErrorQuestion::getId));
                if (questions.isEmpty()) {
                        vo.setTopKnowledgePoints(Collections.emptyList());
                        fillReviewStats(vo, userId);
                        return vo;
                }

                List<Long> questionIds = questions.stream().map(ErrorQuestion::getId).collect(Collectors.toList());
                List<QuestionTag> relations = questionTagMapper.selectList(new LambdaQueryWrapper<QuestionTag>()
                                .in(QuestionTag::getQuestionId, questionIds));

                if (relations.isEmpty()) {
                        vo.setTopKnowledgePoints(Collections.emptyList());
                        fillReviewStats(vo, userId);
                        return vo;
                }

                Map<Long, Long> counter = new HashMap<>();
                for (QuestionTag relation : relations) {
                        counter.put(relation.getTagId(), counter.getOrDefault(relation.getTagId(), 0L) + 1);
                }

                Map<Long, String> tagNameMap = tagMapper.selectBatchIds(counter.keySet()).stream()
                                .collect(Collectors.toMap(Tag::getId, Tag::getName));

                List<StatisticsOverviewVO.TagRankItem> topList = counter.entrySet().stream()
                                .map(entry -> {
                                        StatisticsOverviewVO.TagRankItem item = new StatisticsOverviewVO.TagRankItem();
                                        item.setTagId(entry.getKey());
                                        item.setTagName(tagNameMap.getOrDefault(entry.getKey(), "Unknown Tag"));
                                        item.setCount(entry.getValue());
                                        return item;
                                })
                                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                                .limit(10)
                                .collect(Collectors.toList());

                vo.setTopKnowledgePoints(topList);
                fillReviewStats(vo, userId);
                return vo;
        }

        private void fillReviewStats(StatisticsOverviewVO vo, Long userId) {
                ReviewCenterOverviewVO reviewOverview = reviewService.overview(userId);
                vo.setReviewTodayCompletedCount(reviewOverview.getTodayCompletedCount());
                vo.setReviewTodayDueTotal(reviewOverview.getTodayDueTotal());
                vo.setReviewTotalCount(reviewOverview.getTotalReviewCount());
                vo.setReviewMasteryRate(reviewOverview.getMasteryRate());

                List<StatisticsOverviewVO.ReviewWeakTagItem> weakTags = reviewOverview.getWeakTags() == null
                        ? Collections.emptyList()
                        : reviewOverview.getWeakTags().stream()
                        .map(item -> {
                                StatisticsOverviewVO.ReviewWeakTagItem weakTag = new StatisticsOverviewVO.ReviewWeakTagItem();
                                weakTag.setTagId(item.getTagId());
                                weakTag.setTagName(item.getTagName());
                                weakTag.setPendingCount(item.getPendingCount());
                                return weakTag;
                        })
                        .collect(Collectors.toList());
                vo.setReviewWeakTags(weakTags);

                List<StatisticsOverviewVO.ReviewTrendItem> trend = reviewOverview.getTrend() == null
                        ? Collections.emptyList()
                        : reviewOverview.getTrend().stream()
                        .map(item -> {
                                StatisticsOverviewVO.ReviewTrendItem trendItem = new StatisticsOverviewVO.ReviewTrendItem();
                                trendItem.setDate(item.getDate());
                                trendItem.setDueTotal(item.getDueTotal());
                                trendItem.setCompletedCount(item.getCompletedCount());
                                return trendItem;
                        })
                        .collect(Collectors.toList());
                vo.setReviewTrend(trend);
        }
}
