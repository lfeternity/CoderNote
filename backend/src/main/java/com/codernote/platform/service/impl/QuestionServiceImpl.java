package com.codernote.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.question.QuestionAttachmentVO;
import com.codernote.platform.dto.question.QuestionDetailVO;
import com.codernote.platform.dto.question.QuestionListItemVO;
import com.codernote.platform.dto.question.QuestionRichFieldPayload;
import com.codernote.platform.dto.question.QuestionSaveRequest;
import com.codernote.platform.dto.review.ReviewStatusUpdateRequest;
import com.codernote.platform.entity.ErrorQuestion;
import com.codernote.platform.entity.NoteQuestionLink;
import com.codernote.platform.entity.StudyMaterial;
import com.codernote.platform.entity.StudyNote;
import com.codernote.platform.entity.Tag;
import com.codernote.platform.enums.MasteryStatus;
import com.codernote.platform.enums.ReviewContentType;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.mapper.ErrorQuestionMapper;
import com.codernote.platform.mapper.NoteQuestionLinkMapper;
import com.codernote.platform.mapper.StudyMaterialMapper;
import com.codernote.platform.mapper.StudyNoteMapper;
import com.codernote.platform.mapper.TagMapper;
import com.codernote.platform.service.CacheVersionService;
import com.codernote.platform.service.ManualLinkService;
import com.codernote.platform.service.QuestionService;
import com.codernote.platform.service.ReviewService;
import com.codernote.platform.service.TagRelationHelperService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final ErrorQuestionMapper questionMapper;
    private final StudyMaterialMapper materialMapper;
    private final StudyNoteMapper noteMapper;
    private final NoteQuestionLinkMapper noteQuestionLinkMapper;
    private final TagMapper tagMapper;
    private final TagRelationHelperService tagRelationHelperService;
    private final ManualLinkService manualLinkService;
    private final ReviewService reviewService;
    private final CacheVersionService cacheVersionService;
    private final ObjectMapper objectMapper;

    public QuestionServiceImpl(ErrorQuestionMapper questionMapper,
                               StudyMaterialMapper materialMapper,
                               StudyNoteMapper noteMapper,
                               NoteQuestionLinkMapper noteQuestionLinkMapper,
                               TagMapper tagMapper,
                               TagRelationHelperService tagRelationHelperService,
                               ManualLinkService manualLinkService,
                               ReviewService reviewService,
                               CacheVersionService cacheVersionService,
                               ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.materialMapper = materialMapper;
        this.noteMapper = noteMapper;
        this.noteQuestionLinkMapper = noteQuestionLinkMapper;
        this.tagMapper = tagMapper;
        this.tagRelationHelperService = tagRelationHelperService;
        this.manualLinkService = manualLinkService;
        this.reviewService = reviewService;
        this.cacheVersionService = cacheVersionService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, QuestionSaveRequest request) {
        validateRequest(request);
        ErrorQuestion question = new ErrorQuestion();
        question.setUserId(userId);
        question.setTitle(request.getTitle().trim());
        question.setLanguage(request.getLanguage().trim());
        question.setCoverPath(normalizeCoverPath(userId, request.getCoverPath()));
        question.setMasteryStatus(MasteryStatus.NOT_MASTERED.name());
        question.setErrorCode(encodeRichField(userId, request.getErrorCode(), request.getErrorQuestionAttachments()));
        question.setErrorReason(request.getErrorReason());
        question.setCorrectCode(encodeRichField(userId, request.getCorrectCode(), request.getCorrectSolutionAttachments()));
        question.setSolution(request.getSolution());
        question.setSource(request.getSource());
        question.setRemark(request.getRemark());
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());
        questionMapper.insert(question);

        List<Long> tagIds = tagRelationHelperService.resolveTagIds(request.getTagNames(), userId);
        tagRelationHelperService.replaceQuestionTags(question.getId(), tagIds);

        validateManualMaterialIds(userId, request.getManualMaterialIds());
        manualLinkService.replaceQuestionLinks(userId, question.getId(), request.getManualMaterialIds());
        cacheVersionService.bumpSearchAndStatisticsForUser(userId);
        return question.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long questionId, QuestionSaveRequest request) {
        validateRequest(request);
        ErrorQuestion question = getOwnedQuestion(userId, questionId);
        question.setTitle(request.getTitle().trim());
        question.setLanguage(request.getLanguage().trim());
        question.setCoverPath(normalizeCoverPath(userId, request.getCoverPath()));
        question.setErrorCode(encodeRichField(userId, request.getErrorCode(), request.getErrorQuestionAttachments()));
        question.setErrorReason(request.getErrorReason());
        question.setCorrectCode(encodeRichField(userId, request.getCorrectCode(), request.getCorrectSolutionAttachments()));
        question.setSolution(request.getSolution());
        question.setSource(request.getSource());
        question.setRemark(request.getRemark());
        question.setUpdatedAt(LocalDateTime.now());
        questionMapper.updateById(question);

        List<Long> tagIds = tagRelationHelperService.resolveTagIds(request.getTagNames(), userId);
        tagRelationHelperService.replaceQuestionTags(questionId, tagIds);

        validateManualMaterialIds(userId, request.getManualMaterialIds());
        manualLinkService.replaceQuestionLinks(userId, questionId, request.getManualMaterialIds());
        cacheVersionService.bumpSearchAndStatisticsForUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long questionId) {
        ErrorQuestion question = getOwnedQuestion(userId, questionId);
        questionMapper.deleteById(question.getId());
        tagRelationHelperService.replaceQuestionTags(questionId, Collections.emptyList());
        manualLinkService.clearByQuestionId(userId, questionId);
        reviewService.removePlanByContent(userId, ReviewContentType.QUESTION, questionId);
        cacheVersionService.bumpSearchAndStatisticsForUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(Long userId, List<Long> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return;
        }
        for (Long questionId : questionIds) {
            delete(userId, questionId);
        }
    }

    @Override
    public PageResult<QuestionListItemVO> page(Long userId,
                                               Long pageNo,
                                               Long pageSize,
                                               String language,
                                               String tag,
                                               String masteryStatus,
                                               String keyword) {
        long pageNum = pageNo == null || pageNo < 1 ? 1L : pageNo;
        long size = pageSize == null || pageSize < 1 ? 10L : pageSize;

        LambdaQueryWrapper<ErrorQuestion> wrapper = new LambdaQueryWrapper<ErrorQuestion>()
                .eq(ErrorQuestion::getUserId, userId)
                .orderByDesc(ErrorQuestion::getCreatedAt);

        if (StringUtils.hasText(language)) {
            wrapper.eq(ErrorQuestion::getLanguage, language.trim());
        }
        if (StringUtils.hasText(masteryStatus)) {
            wrapper.eq(ErrorQuestion::getMasteryStatus, masteryStatus.trim());
        }

        Set<Long> tagFilteredQuestionIds = null;
        if (StringUtils.hasText(tag)) {
            Tag exactTag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getName, tag.trim())
                    .eq(Tag::getCreatorUserId, userId)
                    .last("limit 1"));
            if (exactTag == null) {
                return PageResult.empty(pageNum, size);
            }
            tagFilteredQuestionIds = tagRelationHelperService.findQuestionIdsByTagIds(List.of(exactTag.getId()));
            if (tagFilteredQuestionIds.isEmpty()) {
                return PageResult.empty(pageNum, size);
            }
        }

        if (StringUtils.hasText(keyword)) {
            String key = keyword.trim();
            List<Tag> keywordTags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getCreatorUserId, userId)
                    .like(Tag::getName, key));
            Set<Long> keywordTagQuestionIds = keywordTags.isEmpty()
                    ? Collections.emptySet()
                    : tagRelationHelperService.findQuestionIdsByTagIds(keywordTags.stream().map(Tag::getId).collect(Collectors.toList()));

            if (keywordTagQuestionIds.isEmpty()) {
                wrapper.like(ErrorQuestion::getTitle, key);
            } else {
                wrapper.and(w -> w.like(ErrorQuestion::getTitle, key)
                        .or()
                        .in(ErrorQuestion::getId, keywordTagQuestionIds));
            }
        }

        if (tagFilteredQuestionIds != null) {
            wrapper.in(ErrorQuestion::getId, tagFilteredQuestionIds);
        }

        Page<ErrorQuestion> page = questionMapper.selectPage(new Page<>(pageNum, size), wrapper);
        List<ErrorQuestion> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(pageNum, size);
        }

        List<Long> questionIds = records.stream().map(ErrorQuestion::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagMap = tagRelationHelperService.questionTagNameMap(questionIds);
        Set<Long> inReviewPlanIds = reviewService.contentIdsInPlan(userId, ReviewContentType.QUESTION, questionIds);

        List<QuestionListItemVO> voList = records.stream().map(item -> {
            QuestionListItemVO vo = new QuestionListItemVO();
            vo.setId(item.getId());
            vo.setTitle(item.getTitle());
            vo.setLanguage(item.getLanguage());
            vo.setCoverPath(item.getCoverPath());
            vo.setMasteryStatus(item.getMasteryStatus());
            vo.setInReviewPlan(inReviewPlanIds.contains(item.getId()));
            vo.setCreatedAt(item.getCreatedAt());
            vo.setTagNames(tagMap.getOrDefault(item.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(page.getTotal(), pageNum, size, voList);
    }

    @Override
    public QuestionDetailVO detail(Long userId, Long questionId) {
        ErrorQuestion question = getOwnedQuestion(userId, questionId);
        QuestionDetailVO vo = new QuestionDetailVO();
        vo.setId(question.getId());
        vo.setTitle(question.getTitle());
        vo.setLanguage(question.getLanguage());
        vo.setCoverPath(question.getCoverPath());
        vo.setMasteryStatus(question.getMasteryStatus());
        vo.setInReviewPlan(reviewService.contentIdsInPlan(userId, ReviewContentType.QUESTION, List.of(questionId))
                .contains(questionId));

        QuestionRichFieldPayload errorQuestionField = decodeRichField(question.getErrorCode());
        QuestionRichFieldPayload correctSolutionField = decodeRichField(question.getCorrectCode());
        fillAttachmentUrls(errorQuestionField.getAttachments());
        fillAttachmentUrls(correctSolutionField.getAttachments());

        vo.setErrorCode(errorQuestionField.getText());
        vo.setErrorQuestionAttachments(errorQuestionField.getAttachments());
        vo.setErrorReason(question.getErrorReason());

        vo.setCorrectCode(correctSolutionField.getText());
        vo.setCorrectSolutionAttachments(correctSolutionField.getAttachments());
        vo.setSolution(question.getSolution());

        vo.setSource(question.getSource());
        vo.setRemark(question.getRemark());
        vo.setCreatedAt(question.getCreatedAt());
        vo.setUpdatedAt(question.getUpdatedAt());

        List<String> tagNames = tagRelationHelperService.listTagNamesByQuestionId(questionId);
        vo.setTagNames(tagNames);

        List<Long> questionTagIds = tagRelationHelperService.listTagIdsByQuestionId(questionId);
        Set<Long> autoMaterialIds = questionTagIds.isEmpty()
                ? Collections.emptySet()
                : tagRelationHelperService.findMaterialIdsByTagIds(questionTagIds);
        List<Long> manualMaterialIds = manualLinkService.listMaterialIdsByQuestionId(userId, questionId);
        vo.setManualMaterialIds(manualMaterialIds);
        LinkedHashSet<Long> relatedIds = new LinkedHashSet<>();
        relatedIds.addAll(manualMaterialIds);
        relatedIds.addAll(autoMaterialIds);

        List<Long> linkedNoteIds = noteQuestionLinkMapper.selectList(new LambdaQueryWrapper<NoteQuestionLink>()
                        .eq(NoteQuestionLink::getUserId, userId)
                        .eq(NoteQuestionLink::getQuestionId, questionId))
                .stream()
                .map(NoteQuestionLink::getNoteId)
                .collect(Collectors.toList());
        if (linkedNoteIds.isEmpty()) {
            vo.setRelatedNotes(Collections.emptyList());
        } else {
            List<StudyNote> linkedNotes = noteMapper.selectList(new LambdaQueryWrapper<StudyNote>()
                    .eq(StudyNote::getUserId, userId)
                    .in(StudyNote::getId, linkedNoteIds)
                    .orderByDesc(StudyNote::getUpdatedAt));
            if (linkedNotes.isEmpty()) {
                vo.setRelatedNotes(Collections.emptyList());
            } else {
                List<Long> linkedNoteIdList = linkedNotes.stream().map(StudyNote::getId).collect(Collectors.toList());
                Map<Long, List<String>> noteTagMap = tagRelationHelperService.noteTagNameMap(linkedNoteIdList);
                List<QuestionDetailVO.LinkedNoteVO> relatedNotes = linkedNotes.stream().map(note -> {
                    QuestionDetailVO.LinkedNoteVO linked = new QuestionDetailVO.LinkedNoteVO();
                    linked.setId(note.getId());
                    linked.setTitle(note.getTitle());
                    linked.setLanguage(note.getLanguage());
                    linked.setTagNames(noteTagMap.getOrDefault(note.getId(), Collections.emptyList()));
                    return linked;
                }).collect(Collectors.toList());
                vo.setRelatedNotes(relatedNotes);
            }
        }

        if (relatedIds.isEmpty()) {
            vo.setRelatedMaterials(Collections.emptyList());
            return vo;
        }

        List<StudyMaterial> materials = materialMapper.selectList(new LambdaQueryWrapper<StudyMaterial>()
                .eq(StudyMaterial::getUserId, userId)
                .in(StudyMaterial::getId, relatedIds));
        if (materials.isEmpty()) {
            vo.setRelatedMaterials(Collections.emptyList());
            return vo;
        }

        Map<Long, List<String>> materialTagMap = tagRelationHelperService.materialTagNameMap(
                materials.stream().map(StudyMaterial::getId).collect(Collectors.toList())
        );

        Set<String> questionTagNameSet = new LinkedHashSet<>(tagNames);
        List<QuestionDetailVO.LinkedMaterialVO> linkedList = materials.stream().map(material -> {
            QuestionDetailVO.LinkedMaterialVO linked = new QuestionDetailVO.LinkedMaterialVO();
            linked.setId(material.getId());
            linked.setTitle(material.getTitle());
            linked.setMaterialType(material.getMaterialType());
            linked.setLanguage(material.getLanguage());
            linked.setTagNames(materialTagMap.getOrDefault(material.getId(), Collections.emptyList()));
            return linked;
        }).collect(Collectors.toList());

        linkedList.sort(Comparator.comparingInt((QuestionDetailVO.LinkedMaterialVO item) ->
                matchCount(questionTagNameSet, item.getTagNames())).reversed());

        vo.setRelatedMaterials(linkedList);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMasteryStatus(Long userId, Long questionId, String masteryStatus) {
        ErrorQuestion question = getOwnedQuestion(userId, questionId);
        ReviewStatusUpdateRequest request = new ReviewStatusUpdateRequest();
        request.setContentType(ReviewContentType.QUESTION.name());
        request.setContentId(question.getId());
        request.setMasteryStatus(masteryStatus);
        reviewService.updateContentStatus(userId, request);
        cacheVersionService.bumpSearchAndStatisticsForUser(userId);
    }

    @Override
    public List<QuestionListItemVO> listByTagId(Long userId, Long tagId) {
        Set<Long> questionIds = tagRelationHelperService.findQuestionIdsByTagIds(List.of(tagId));
        if (questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ErrorQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                .eq(ErrorQuestion::getUserId, userId)
                .in(ErrorQuestion::getId, questionIds)
                .orderByDesc(ErrorQuestion::getCreatedAt));
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<String>> questionTagMap = tagRelationHelperService.questionTagNameMap(
                questions.stream().map(ErrorQuestion::getId).collect(Collectors.toList())
        );
        Set<Long> inReviewPlanIds = reviewService.contentIdsInPlan(
                userId,
                ReviewContentType.QUESTION,
                questions.stream().map(ErrorQuestion::getId).collect(Collectors.toList())
        );
        List<QuestionListItemVO> voList = new ArrayList<>();
        for (ErrorQuestion item : questions) {
            QuestionListItemVO vo = new QuestionListItemVO();
            vo.setId(item.getId());
            vo.setTitle(item.getTitle());
            vo.setLanguage(item.getLanguage());
            vo.setCoverPath(item.getCoverPath());
            vo.setMasteryStatus(item.getMasteryStatus());
            vo.setInReviewPlan(inReviewPlanIds.contains(item.getId()));
            vo.setCreatedAt(item.getCreatedAt());
            vo.setTagNames(questionTagMap.getOrDefault(item.getId(), Collections.emptyList()));
            voList.add(vo);
        }
        return voList;
    }

    private void validateRequest(QuestionSaveRequest request) {
        if (CollectionUtils.isEmpty(request.getTagNames())) {
            throw new BizException(400, "At least one tag is required");
        }

        boolean hasErrorQuestionText = StringUtils.hasText(request.getErrorCode());
        boolean hasErrorQuestionAttachments = !CollectionUtils.isEmpty(request.getErrorQuestionAttachments());
        if (!hasErrorQuestionText && !hasErrorQuestionAttachments) {
            throw new BizException(400, "Error question text or attachments are required");
        }

        boolean hasCorrectSolutionText = StringUtils.hasText(request.getCorrectCode());
        boolean hasCorrectSolutionAttachments = !CollectionUtils.isEmpty(request.getCorrectSolutionAttachments());
        if (!hasCorrectSolutionText && !hasCorrectSolutionAttachments) {
            throw new BizException(400, "Correct solution text or attachments are required");
        }
    }

    private String encodeRichField(Long userId, String text, List<QuestionAttachmentVO> attachments) {
        List<QuestionAttachmentVO> safeAttachments = sanitizeAttachments(userId, attachments);
        String safeText = text == null ? "" : text;

        if (safeAttachments.isEmpty()) {
            return safeText;
        }

        QuestionRichFieldPayload payload = new QuestionRichFieldPayload();
        payload.setText(safeText);
        payload.setAttachments(safeAttachments);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BizException(500, "Failed to encode attachment payload");
        }
    }

    private QuestionRichFieldPayload decodeRichField(String rawValue) {
        QuestionRichFieldPayload fallback = new QuestionRichFieldPayload();
        fallback.setText(rawValue == null ? "" : rawValue);
        fallback.setAttachments(Collections.emptyList());

        if (!StringUtils.hasText(rawValue)) {
            return fallback;
        }

        String trimmed = rawValue.trim();
        if (!(trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            return fallback;
        }

        try {
            QuestionRichFieldPayload payload = objectMapper.readValue(trimmed, QuestionRichFieldPayload.class);
            if (payload == null) {
                return fallback;
            }
            payload.setText(payload.getText() == null ? "" : payload.getText());
            payload.setAttachments(payload.getAttachments() == null ? Collections.emptyList() : payload.getAttachments());
            return payload;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private List<QuestionAttachmentVO> sanitizeAttachments(Long userId, List<QuestionAttachmentVO> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            return Collections.emptyList();
        }

        String expectedPrefix = "question/" + userId + "/";
        LinkedHashSet<String> pathDedup = new LinkedHashSet<>();
        List<QuestionAttachmentVO> safeList = new ArrayList<>();
        for (QuestionAttachmentVO item : attachments) {
            if (item == null || !StringUtils.hasText(item.getPath())) {
                continue;
            }
            String cleanedPath = item.getPath().replace('\\', '/').trim();
            if (cleanedPath.contains("..") || cleanedPath.startsWith("/") || !cleanedPath.startsWith(expectedPrefix)) {
                throw new BizException(400, "Invalid attachment path");
            }
            if (!pathDedup.add(cleanedPath)) {
                continue;
            }

            QuestionAttachmentVO safe = new QuestionAttachmentVO();
            safe.setPath(cleanedPath);
            safe.setFileName(StringUtils.hasText(item.getFileName()) ? item.getFileName().trim() : cleanedPath.substring(cleanedPath.lastIndexOf('/') + 1));
            safe.setContentType(item.getContentType());
            safe.setSize(item.getSize());
            safe.setImage(isImageByType(safe.getFileName(), safe.getContentType()));
            safeList.add(safe);
        }
        return safeList;
    }

    private void fillAttachmentUrls(List<QuestionAttachmentVO> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            return;
        }
        for (QuestionAttachmentVO item : attachments) {
            if (item == null || !StringUtils.hasText(item.getPath())) {
                continue;
            }
            String fileName = StringUtils.hasText(item.getFileName()) ? item.getFileName() : "file";
            String preview = "/api/v1/file/download?path=" + encodeUrl(item.getPath()) + "&name=" + encodeUrl(fileName);
            item.setPreviewUrl(preview);
            item.setDownloadUrl(preview + "&download=true");
        }
    }

    private String encodeUrl(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isImageByType(String fileName, String contentType) {
        if (StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        String ext = fileExtension(fileName);
        return "png".equals(ext)
                || "jpg".equals(ext)
                || "jpeg".equals(ext)
                || "gif".equals(ext)
                || "webp".equals(ext)
                || "bmp".equals(ext);
    }

    private String fileExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private void validateManualMaterialIds(Long userId, List<Long> materialIds) {
        if (CollectionUtils.isEmpty(materialIds)) {
            return;
        }
        List<StudyMaterial> materials = materialMapper.selectList(new LambdaQueryWrapper<StudyMaterial>()
                .in(StudyMaterial::getId, materialIds)
                .eq(StudyMaterial::getUserId, userId));
        if (materials.size() != new LinkedHashSet<>(materialIds).size()) {
            throw new BizException(400, "Invalid manual material links");
        }
    }

    private String normalizeCoverPath(Long userId, String coverPath) {
        if (!StringUtils.hasText(coverPath)) {
            return null;
        }
        String cleaned = coverPath.replace('\\', '/').trim();
        String prefix = "question/" + userId + "/";
        if (cleaned.contains("..") || cleaned.startsWith("/") || !cleaned.startsWith(prefix)) {
            throw new BizException(400, "Invalid cover path");
        }
        return cleaned;
    }

    private ErrorQuestion getOwnedQuestion(Long userId, Long questionId) {
        ErrorQuestion question = questionMapper.selectOne(new LambdaQueryWrapper<ErrorQuestion>()
                .eq(ErrorQuestion::getId, questionId)
                .eq(ErrorQuestion::getUserId, userId)
                .last("limit 1"));
        if (question == null) {
            throw new BizException(404, "Question not found");
        }
        return question;
    }

    private int matchCount(Set<String> sourceTags, List<String> targetTags) {
        if (CollectionUtils.isEmpty(sourceTags) || CollectionUtils.isEmpty(targetTags)) {
            return 0;
        }
        int count = 0;
        for (String tagName : targetTags) {
            if (sourceTags.contains(tagName)) {
                count++;
            }
        }
        return count;
    }
}
