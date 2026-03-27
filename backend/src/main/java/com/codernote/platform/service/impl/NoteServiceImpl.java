package com.codernote.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codernote.platform.common.PageResult;
import com.codernote.platform.config.NoteVersionProperties;
import com.codernote.platform.dto.note.NoteDetailVO;
import com.codernote.platform.dto.note.NoteListItemVO;
import com.codernote.platform.dto.note.NoteSaveRequest;
import com.codernote.platform.dto.note.NoteVersionDetailVO;
import com.codernote.platform.dto.note.NoteVersionListItemVO;
import com.codernote.platform.dto.review.ReviewStatusUpdateRequest;
import com.codernote.platform.entity.ErrorQuestion;
import com.codernote.platform.entity.NoteFavorite;
import com.codernote.platform.entity.NoteMaterialLink;
import com.codernote.platform.entity.NoteQuestionLink;
import com.codernote.platform.entity.NoteVersion;
import com.codernote.platform.entity.NoteVersionOperationLog;
import com.codernote.platform.entity.StudyMaterial;
import com.codernote.platform.entity.StudyNote;
import com.codernote.platform.entity.Tag;
import com.codernote.platform.enums.MasteryStatus;
import com.codernote.platform.enums.ReviewContentType;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.mapper.ErrorQuestionMapper;
import com.codernote.platform.mapper.NoteFavoriteMapper;
import com.codernote.platform.mapper.NoteMaterialLinkMapper;
import com.codernote.platform.mapper.NoteQuestionLinkMapper;
import com.codernote.platform.mapper.NoteVersionMapper;
import com.codernote.platform.mapper.NoteVersionOperationLogMapper;
import com.codernote.platform.mapper.StudyMaterialMapper;
import com.codernote.platform.mapper.StudyNoteMapper;
import com.codernote.platform.mapper.TagMapper;
import com.codernote.platform.service.NoteService;
import com.codernote.platform.service.ReviewService;
import com.codernote.platform.service.TagRelationHelperService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NoteServiceImpl implements NoteService {

    private static final int DEFAULT_MAX_VERSIONS_PER_NOTE = 50;
    private static final int DEFAULT_VERSION_CONTENT_LIMIT_BYTES = 5 * 1024 * 1024;
    private static final int AUTO_SUMMARY_LENGTH = 50;
    private static final String VERSION_EDITOR_NAME_SELF = "本人";
    private static final TypeReference<List<String>> TAG_NAME_LIST_TYPE = new TypeReference<>() {
    };

    private final StudyNoteMapper noteMapper;
    private final NoteQuestionLinkMapper noteQuestionLinkMapper;
    private final NoteMaterialLinkMapper noteMaterialLinkMapper;
    private final NoteFavoriteMapper noteFavoriteMapper;
    private final NoteVersionMapper noteVersionMapper;
    private final NoteVersionOperationLogMapper noteVersionOperationLogMapper;
    private final ErrorQuestionMapper questionMapper;
    private final StudyMaterialMapper materialMapper;
    private final TagMapper tagMapper;
    private final TagRelationHelperService tagRelationHelperService;
    private final ReviewService reviewService;
    private final NoteVersionProperties noteVersionProperties;
    private final ObjectMapper objectMapper;

    public NoteServiceImpl(StudyNoteMapper noteMapper,
                           NoteQuestionLinkMapper noteQuestionLinkMapper,
                           NoteMaterialLinkMapper noteMaterialLinkMapper,
                           NoteFavoriteMapper noteFavoriteMapper,
                           NoteVersionMapper noteVersionMapper,
                           NoteVersionOperationLogMapper noteVersionOperationLogMapper,
                           ErrorQuestionMapper questionMapper,
                           StudyMaterialMapper materialMapper,
                           TagMapper tagMapper,
                           TagRelationHelperService tagRelationHelperService,
                           ReviewService reviewService,
                           NoteVersionProperties noteVersionProperties,
                           ObjectMapper objectMapper) {
        this.noteMapper = noteMapper;
        this.noteQuestionLinkMapper = noteQuestionLinkMapper;
        this.noteMaterialLinkMapper = noteMaterialLinkMapper;
        this.noteFavoriteMapper = noteFavoriteMapper;
        this.noteVersionMapper = noteVersionMapper;
        this.noteVersionOperationLogMapper = noteVersionOperationLogMapper;
        this.questionMapper = questionMapper;
        this.materialMapper = materialMapper;
        this.tagMapper = tagMapper;
        this.tagRelationHelperService = tagRelationHelperService;
        this.reviewService = reviewService;
        this.noteVersionProperties = noteVersionProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, NoteSaveRequest request) {
        validateRequest(request);
        String title = normalizeTitle(request.getTitle());
        String content = sanitizeContent(request.getContent());
        String language = normalizeLanguage(request.getLanguage());
        List<String> tagNames = normalizeTagNames(request.getTagNames());
        if (tagNames.isEmpty()) {
            throw new BizException(400, "At least one tag is required");
        }
        ensureVersionContentSize(content);

        StudyNote note = new StudyNote();
        note.setUserId(userId);
        note.setTitle(title);
        note.setContent(content);
        note.setLanguage(language);
        note.setCoverPath(normalizeCoverPath(userId, request.getCoverPath()));
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        noteMapper.insert(note);

        List<Long> tagIds = tagRelationHelperService.resolveTagIds(tagNames, userId);
        tagRelationHelperService.replaceNoteTags(note.getId(), tagIds);

        replaceQuestionLinks(userId, note.getId(), request.getManualQuestionIds());
        replaceMaterialLinks(userId, note.getId(), request.getManualMaterialIds());
        createNoteVersion(userId, note.getId(), title, content, language, tagNames,
                resolveVersionSummary(request.getVersionSummary(), content));
        return note.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long noteId, NoteSaveRequest request) {
        validateRequest(request);

        StudyNote note = getOwnedNote(userId, noteId);
        List<String> previousTagNames = normalizeTagNames(tagRelationHelperService.listTagNamesByNoteId(noteId));

        String title = normalizeTitle(request.getTitle());
        String content = sanitizeContent(request.getContent());
        String language = normalizeLanguage(request.getLanguage());
        List<String> tagNames = normalizeTagNames(request.getTagNames());
        if (tagNames.isEmpty()) {
            throw new BizException(400, "At least one tag is required");
        }
        ensureVersionContentSize(content);

        boolean versionChanged = !Objects.equals(note.getTitle(), title)
                || !Objects.equals(note.getContent(), content)
                || !sameTagNames(previousTagNames, tagNames);

        note.setTitle(title);
        note.setContent(content);
        note.setLanguage(language);
        note.setCoverPath(normalizeCoverPath(userId, request.getCoverPath()));
        note.setUpdatedAt(LocalDateTime.now());
        noteMapper.updateById(note);

        List<Long> tagIds = tagRelationHelperService.resolveTagIds(tagNames, userId);
        tagRelationHelperService.replaceNoteTags(noteId, tagIds);

        replaceQuestionLinks(userId, noteId, request.getManualQuestionIds());
        replaceMaterialLinks(userId, noteId, request.getManualMaterialIds());

        if (versionChanged) {
            createNoteVersion(userId, noteId, title, content, language, tagNames,
                    resolveVersionSummary(request.getVersionSummary(), content));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long noteId) {
        StudyNote note = getOwnedNote(userId, noteId);
        noteMapper.deleteById(note.getId());
        tagRelationHelperService.replaceNoteTags(noteId, Collections.emptyList());
        clearQuestionLinks(userId, noteId);
        clearMaterialLinks(userId, noteId);
        noteFavoriteMapper.delete(new LambdaQueryWrapper<NoteFavorite>()
                .eq(NoteFavorite::getNoteId, noteId));
        reviewService.removePlanByContent(userId, ReviewContentType.NOTE, noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(Long userId, List<Long> noteIds) {
        if (CollectionUtils.isEmpty(noteIds)) {
            return;
        }
        for (Long noteId : noteIds) {
            delete(userId, noteId);
        }
    }

    @Override
    public PageResult<NoteListItemVO> page(Long userId,
                                           Long pageNo,
                                           Long pageSize,
                                           String language,
                                           String tag,
                                           String favoriteStatus,
                                           String keyword,
                                           String sortBy,
                                           String sortOrder) {
        long pageNum = pageNo == null || pageNo < 1 ? 1L : pageNo;
        long size = pageSize == null || pageSize < 1 ? 10L : pageSize;

        LambdaQueryWrapper<StudyNote> wrapper = new LambdaQueryWrapper<StudyNote>()
                .eq(StudyNote::getUserId, userId);

        if (StringUtils.hasText(language)) {
            wrapper.eq(StudyNote::getLanguage, language.trim());
        }

        Set<Long> restrictedIds = null;
        Set<Long> allFavoriteIdSet = listAllFavoriteIdSet(userId);
        if (StringUtils.hasText(favoriteStatus)) {
            String favoriteFilter = favoriteStatus.trim().toUpperCase();
            if ("FAVORITE".equals(favoriteFilter)) {
                restrictedIds = new HashSet<>(allFavoriteIdSet);
                if (restrictedIds.isEmpty()) {
                    return PageResult.empty(pageNum, size);
                }
            } else if ("UNFAVORITE".equals(favoriteFilter)) {
                if (!allFavoriteIdSet.isEmpty()) {
                    wrapper.notIn(StudyNote::getId, allFavoriteIdSet);
                }
            }
        }

        if (StringUtils.hasText(tag)) {
            Tag exactTag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getName, tag.trim())
                    .last("limit 1"));
            if (exactTag == null) {
                return PageResult.empty(pageNum, size);
            }
            Set<Long> tagFilteredNoteIds = tagRelationHelperService.findNoteIdsByTagIds(List.of(exactTag.getId()));
            if (tagFilteredNoteIds.isEmpty()) {
                return PageResult.empty(pageNum, size);
            }
            restrictedIds = mergeIntersection(restrictedIds, tagFilteredNoteIds);
            if (restrictedIds.isEmpty()) {
                return PageResult.empty(pageNum, size);
            }
        }

        if (StringUtils.hasText(keyword)) {
            String key = keyword.trim();
            List<Tag> keywordTags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                    .like(Tag::getName, key));
            Set<Long> keywordTagNoteIds = keywordTags.isEmpty()
                    ? Collections.emptySet()
                    : tagRelationHelperService.findNoteIdsByTagIds(keywordTags.stream().map(Tag::getId).collect(Collectors.toList()));

            if (restrictedIds == null) {
                if (keywordTagNoteIds.isEmpty()) {
                    wrapper.and(w -> w.like(StudyNote::getTitle, key)
                            .or()
                            .like(StudyNote::getContent, key));
                } else {
                    wrapper.and(w -> w.like(StudyNote::getTitle, key)
                            .or()
                            .like(StudyNote::getContent, key)
                            .or()
                            .in(StudyNote::getId, keywordTagNoteIds));
                }
            } else {
                if (keywordTagNoteIds.isEmpty()) {
                    wrapper.and(w -> w.like(StudyNote::getTitle, key)
                            .or()
                            .like(StudyNote::getContent, key));
                } else {
                    Set<Long> restrictedKeywordIds = intersectionCopy(restrictedIds, keywordTagNoteIds);
                    if (restrictedKeywordIds.isEmpty()) {
                        wrapper.and(w -> w.like(StudyNote::getTitle, key)
                                .or()
                                .like(StudyNote::getContent, key));
                    } else {
                        wrapper.and(w -> w.like(StudyNote::getTitle, key)
                                .or()
                                .like(StudyNote::getContent, key)
                                .or()
                                .in(StudyNote::getId, restrictedKeywordIds));
                    }
                }
            }
        }

        if (restrictedIds != null) {
            wrapper.in(StudyNote::getId, restrictedIds);
        }

        boolean updatedSort = "updated_at".equalsIgnoreCase(sortBy)
                || "updatedAt".equalsIgnoreCase(sortBy)
                || !StringUtils.hasText(sortBy);
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if (updatedSort) {
            if (asc) {
                wrapper.orderByAsc(StudyNote::getUpdatedAt);
            } else {
                wrapper.orderByDesc(StudyNote::getUpdatedAt);
            }
        } else {
            if (asc) {
                wrapper.orderByAsc(StudyNote::getCreatedAt);
            } else {
                wrapper.orderByDesc(StudyNote::getCreatedAt);
            }
        }

        Page<StudyNote> page = noteMapper.selectPage(new Page<>(pageNum, size), wrapper);
        List<StudyNote> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(pageNum, size);
        }

        List<Long> noteIds = records.stream().map(StudyNote::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagMap = tagRelationHelperService.noteTagNameMap(noteIds);
        Map<Long, Long> questionCountMap = listQuestionCountMap(userId, noteIds);
        Map<Long, Long> materialCountMap = listMaterialCountMap(userId, noteIds);
        Set<Long> favoriteIdSet = listFavoriteIdSet(userId, noteIds);
        Map<Long, String> masteryStatusMap = reviewService.noteMasteryStatusMap(userId, noteIds);

        List<NoteListItemVO> voList = records.stream().map(item -> {
            NoteListItemVO vo = new NoteListItemVO();
            vo.setId(item.getId());
            vo.setTitle(item.getTitle());
            vo.setLanguage(item.getLanguage());
            vo.setCoverPath(item.getCoverPath());
            vo.setTagNames(tagMap.getOrDefault(item.getId(), Collections.emptyList()));
            vo.setCreatedAt(item.getCreatedAt());
            vo.setUpdatedAt(item.getUpdatedAt());
            vo.setRelatedQuestionCount(questionCountMap.getOrDefault(item.getId(), 0L));
            vo.setRelatedMaterialCount(materialCountMap.getOrDefault(item.getId(), 0L));
            vo.setFavorite(favoriteIdSet.contains(item.getId()));
            vo.setMasteryStatus(masteryStatusMap.getOrDefault(item.getId(), MasteryStatus.NOT_MASTERED.name()));
            vo.setInReviewPlan(masteryStatusMap.containsKey(item.getId()));
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(page.getTotal(), pageNum, size, voList);
    }

    @Override
    public NoteDetailVO detail(Long userId, Long noteId) {
        StudyNote note = getOwnedNote(userId, noteId);

        NoteDetailVO vo = new NoteDetailVO();
        Map<Long, String> noteMasteryMap = reviewService.noteMasteryStatusMap(userId, List.of(noteId));
        vo.setId(note.getId());
        vo.setTitle(note.getTitle());
        vo.setContent(note.getContent());
        vo.setLanguage(note.getLanguage());
        vo.setCoverPath(note.getCoverPath());
        vo.setMasteryStatus(noteMasteryMap.getOrDefault(noteId, MasteryStatus.NOT_MASTERED.name()));
        vo.setInReviewPlan(noteMasteryMap.containsKey(noteId));
        vo.setCreatedAt(note.getCreatedAt());
        vo.setUpdatedAt(note.getUpdatedAt());
        vo.setFavorite(isNoteFavorited(userId, noteId));

        vo.setTagNames(tagRelationHelperService.listTagNamesByNoteId(noteId));

        List<Long> questionIds = listQuestionIdsByNoteId(userId, noteId);
        List<Long> materialIds = listMaterialIdsByNoteId(userId, noteId);
        vo.setManualQuestionIds(questionIds);
        vo.setManualMaterialIds(materialIds);

        if (questionIds.isEmpty()) {
            vo.setRelatedQuestions(Collections.emptyList());
        } else {
            List<ErrorQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                    .eq(ErrorQuestion::getUserId, userId)
                    .in(ErrorQuestion::getId, questionIds));
            Map<Long, List<String>> questionTagMap = tagRelationHelperService.questionTagNameMap(
                    questions.stream().map(ErrorQuestion::getId).collect(Collectors.toList())
            );
            List<NoteDetailVO.LinkedQuestionVO> relatedQuestions = questions.stream().map(question -> {
                NoteDetailVO.LinkedQuestionVO linked = new NoteDetailVO.LinkedQuestionVO();
                linked.setId(question.getId());
                linked.setTitle(question.getTitle());
                linked.setLanguage(question.getLanguage());
                linked.setMasteryStatus(question.getMasteryStatus());
                linked.setTagNames(questionTagMap.getOrDefault(question.getId(), Collections.emptyList()));
                return linked;
            }).collect(Collectors.toList());
            vo.setRelatedQuestions(relatedQuestions);
        }

        if (materialIds.isEmpty()) {
            vo.setRelatedMaterials(Collections.emptyList());
        } else {
            List<StudyMaterial> materials = materialMapper.selectList(new LambdaQueryWrapper<StudyMaterial>()
                    .eq(StudyMaterial::getUserId, userId)
                    .in(StudyMaterial::getId, materialIds));
            Map<Long, List<String>> materialTagMap = tagRelationHelperService.materialTagNameMap(
                    materials.stream().map(StudyMaterial::getId).collect(Collectors.toList())
            );
            List<NoteDetailVO.LinkedMaterialVO> relatedMaterials = materials.stream().map(material -> {
                NoteDetailVO.LinkedMaterialVO linked = new NoteDetailVO.LinkedMaterialVO();
                linked.setId(material.getId());
                linked.setTitle(material.getTitle());
                linked.setMaterialType(material.getMaterialType());
                linked.setLanguage(material.getLanguage());
                linked.setTagNames(materialTagMap.getOrDefault(material.getId(), Collections.emptyList()));
                return linked;
            }).collect(Collectors.toList());
            vo.setRelatedMaterials(relatedMaterials);
        }

        return vo;
    }

    @Override
    public List<NoteListItemVO> listByTagId(Long userId, Long tagId) {
        Set<Long> noteIds = tagRelationHelperService.findNoteIdsByTagIds(List.of(tagId));
        if (noteIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<StudyNote> notes = noteMapper.selectList(new LambdaQueryWrapper<StudyNote>()
                .eq(StudyNote::getUserId, userId)
                .in(StudyNote::getId, noteIds)
                .orderByDesc(StudyNote::getUpdatedAt));
        if (notes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> idList = notes.stream().map(StudyNote::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagMap = tagRelationHelperService.noteTagNameMap(idList);
        Map<Long, Long> questionCountMap = listQuestionCountMap(userId, idList);
        Map<Long, Long> materialCountMap = listMaterialCountMap(userId, idList);
        Set<Long> favoriteIdSet = listFavoriteIdSet(userId, idList);
        Map<Long, String> masteryStatusMap = reviewService.noteMasteryStatusMap(userId, idList);

        List<NoteListItemVO> voList = new ArrayList<>();
        for (StudyNote note : notes) {
            NoteListItemVO vo = new NoteListItemVO();
            vo.setId(note.getId());
            vo.setTitle(note.getTitle());
            vo.setLanguage(note.getLanguage());
            vo.setTagNames(tagMap.getOrDefault(note.getId(), Collections.emptyList()));
            vo.setCreatedAt(note.getCreatedAt());
            vo.setUpdatedAt(note.getUpdatedAt());
            vo.setRelatedQuestionCount(questionCountMap.getOrDefault(note.getId(), 0L));
            vo.setRelatedMaterialCount(materialCountMap.getOrDefault(note.getId(), 0L));
            vo.setFavorite(favoriteIdSet.contains(note.getId()));
            vo.setMasteryStatus(masteryStatusMap.getOrDefault(note.getId(), MasteryStatus.NOT_MASTERED.name()));
            vo.setInReviewPlan(masteryStatusMap.containsKey(note.getId()));
            voList.add(vo);
        }
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long userId, Long noteId) {
        StudyNote note = getOwnedNote(userId, noteId);
        NoteFavorite exists = noteFavoriteMapper.selectOne(new LambdaQueryWrapper<NoteFavorite>()
                .eq(NoteFavorite::getUserId, userId)
                .eq(NoteFavorite::getNoteId, note.getId())
                .last("limit 1"));
        if (exists != null) {
            return;
        }
        NoteFavorite favorite = new NoteFavorite();
        favorite.setUserId(userId);
        favorite.setNoteId(note.getId());
        favorite.setCreatedAt(LocalDateTime.now());
        noteFavoriteMapper.insert(favorite);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavorite(Long userId, Long noteId) {
        getOwnedNote(userId, noteId);
        noteFavoriteMapper.delete(new LambdaQueryWrapper<NoteFavorite>()
                .eq(NoteFavorite::getUserId, userId)
                .eq(NoteFavorite::getNoteId, noteId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkQuestion(Long userId, Long questionId, List<Long> noteIds) {
        long questionCount = questionMapper.selectCount(new LambdaQueryWrapper<ErrorQuestion>()
                .eq(ErrorQuestion::getId, questionId)
                .eq(ErrorQuestion::getUserId, userId));
        if (questionCount == 0) {
            throw new BizException(404, "Question not found");
        }

        LinkedHashSet<Long> dedupIds = normalizeIds(noteIds);
        if (dedupIds.isEmpty()) {
            return;
        }
        ensureOwnedNotes(userId, dedupIds);

        for (Long noteId : dedupIds) {
            NoteQuestionLink existing = noteQuestionLinkMapper.selectOne(new LambdaQueryWrapper<NoteQuestionLink>()
                    .eq(NoteQuestionLink::getUserId, userId)
                    .eq(NoteQuestionLink::getNoteId, noteId)
                    .eq(NoteQuestionLink::getQuestionId, questionId)
                    .last("limit 1"));
            if (existing != null) {
                continue;
            }
            NoteQuestionLink link = new NoteQuestionLink();
            link.setUserId(userId);
            link.setNoteId(noteId);
            link.setQuestionId(questionId);
            noteQuestionLinkMapper.insert(link);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkMaterial(Long userId, Long materialId, List<Long> noteIds) {
        long materialCount = materialMapper.selectCount(new LambdaQueryWrapper<StudyMaterial>()
                .eq(StudyMaterial::getId, materialId)
                .eq(StudyMaterial::getUserId, userId));
        if (materialCount == 0) {
            throw new BizException(404, "Material not found");
        }

        LinkedHashSet<Long> dedupIds = normalizeIds(noteIds);
        if (dedupIds.isEmpty()) {
            return;
        }
        ensureOwnedNotes(userId, dedupIds);

        for (Long noteId : dedupIds) {
            NoteMaterialLink existing = noteMaterialLinkMapper.selectOne(new LambdaQueryWrapper<NoteMaterialLink>()
                    .eq(NoteMaterialLink::getUserId, userId)
                    .eq(NoteMaterialLink::getNoteId, noteId)
                    .eq(NoteMaterialLink::getMaterialId, materialId)
                    .last("limit 1"));
            if (existing != null) {
                continue;
            }
            NoteMaterialLink link = new NoteMaterialLink();
            link.setUserId(userId);
            link.setNoteId(noteId);
            link.setMaterialId(materialId);
            noteMaterialLinkMapper.insert(link);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMasteryStatus(Long userId, Long noteId, String masteryStatus) {
        getOwnedNote(userId, noteId);
        ReviewStatusUpdateRequest request = new ReviewStatusUpdateRequest();
        request.setContentType(ReviewContentType.NOTE.name());
        request.setContentId(noteId);
        request.setMasteryStatus(masteryStatus);
        reviewService.updateContentStatus(userId, request);
    }

    @Override
    public List<NoteVersionListItemVO> listVersions(Long userId, Long noteId) {
        ensureVersionAccessible(userId, noteId);
        List<NoteVersion> versions = noteVersionMapper.selectList(new LambdaQueryWrapper<NoteVersion>()
                .eq(NoteVersion::getUserId, userId)
                .eq(NoteVersion::getNoteId, noteId)
                .orderByDesc(NoteVersion::getVersionNo));
        if (versions.isEmpty()) {
            return Collections.emptyList();
        }
        return versions.stream().map(this::toVersionListItem).collect(Collectors.toList());
    }

    @Override
    public NoteVersionDetailVO versionDetail(Long userId, Long noteId, Long versionId) {
        ensureVersionAccessible(userId, noteId);
        NoteVersion version = getOwnedVersion(userId, noteId, versionId);
        return toVersionDetail(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreVersion(Long userId, Long noteId, Long versionId) {
        StudyNote note = ensureVersionAccessible(userId, noteId);
        NoteVersion sourceVersion = getOwnedVersion(userId, noteId, versionId);

        note.setTitle(sourceVersion.getTitle());
        note.setContent(sourceVersion.getContent());
        note.setLanguage(sourceVersion.getLanguage());
        note.setUpdatedAt(LocalDateTime.now());
        noteMapper.updateById(note);

        List<String> restoredTagNames = decodeTagNames(sourceVersion.getTagNamesJson());
        List<Long> restoredTagIds = tagRelationHelperService.resolveTagIds(restoredTagNames, userId);
        tagRelationHelperService.replaceNoteTags(noteId, restoredTagIds);

        int restoredVersionNo = createNoteVersion(
                userId,
                noteId,
                sourceVersion.getTitle(),
                sourceVersion.getContent(),
                sourceVersion.getLanguage(),
                restoredTagNames,
                "恢复自 v" + sourceVersion.getVersionNo() + " 版本"
        );

        NoteVersionOperationLog operationLog = new NoteVersionOperationLog();
        operationLog.setNoteId(noteId);
        operationLog.setUserId(userId);
        operationLog.setAction("RESTORE");
        operationLog.setSourceVersionNo(sourceVersion.getVersionNo());
        operationLog.setTargetVersionNo(restoredVersionNo);
        operationLog.setRemark("恢复自 v" + sourceVersion.getVersionNo() + " 版本");
        operationLog.setCreatedAt(LocalDateTime.now());
        noteVersionOperationLogMapper.insert(operationLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVersion(Long userId, Long noteId, Long versionId) {
        ensureVersionAccessible(userId, noteId);
        NoteVersion version = getOwnedVersion(userId, noteId, versionId);
        noteVersionMapper.deleteById(version.getId());
    }

    private void validateRequest(NoteSaveRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BizException(400, "Title is required");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new BizException(400, "Content is required");
        }
        if (!StringUtils.hasText(request.getLanguage())) {
            throw new BizException(400, "Language is required");
        }
        if (CollectionUtils.isEmpty(request.getTagNames())) {
            throw new BizException(400, "At least one tag is required");
        }
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.trim();
    }

    private String normalizeLanguage(String language) {
        return language == null ? "" : language.trim();
    }

    private List<String> normalizeTagNames(List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return Collections.emptyList();
        }
        return tagNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean sameTagNames(List<String> first, List<String> second) {
        return new LinkedHashSet<>(normalizeTagNames(first)).equals(new LinkedHashSet<>(normalizeTagNames(second)));
    }

    private void ensureVersionContentSize(String content) {
        int contentSize = content == null ? 0 : content.getBytes(StandardCharsets.UTF_8).length;
        int maxSize = noteVersionProperties.getContentSizeLimitBytes() == null
                ? DEFAULT_VERSION_CONTENT_LIMIT_BYTES
                : noteVersionProperties.getContentSizeLimitBytes();
        if (contentSize > maxSize) {
            throw new BizException(400, "Note content exceeds 5MB limit");
        }
    }

    private String resolveVersionSummary(String manualSummary, String content) {
        if (StringUtils.hasText(manualSummary)) {
            return trimSummary(manualSummary.trim());
        }
        return buildAutoSummary(content);
    }

    private String buildAutoSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "内容更新";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(normalized)) {
            return "内容更新";
        }
        if (normalized.length() <= AUTO_SUMMARY_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, AUTO_SUMMARY_LENGTH);
    }

    private String trimSummary(String summary) {
        if (!StringUtils.hasText(summary)) {
            return "内容更新";
        }
        return summary.length() <= 120 ? summary : summary.substring(0, 120);
    }

    private NoteVersionListItemVO toVersionListItem(NoteVersion version) {
        NoteVersionListItemVO item = new NoteVersionListItemVO();
        item.setId(version.getId());
        item.setVersionNo(version.getVersionNo());
        item.setVersionLabel("v" + version.getVersionNo());
        item.setSummary(version.getSummary());
        item.setEditorName(VERSION_EDITOR_NAME_SELF);
        item.setCreatedAt(version.getCreatedAt());
        return item;
    }

    private NoteVersionDetailVO toVersionDetail(NoteVersion version) {
        NoteVersionDetailVO detail = new NoteVersionDetailVO();
        detail.setId(version.getId());
        detail.setVersionNo(version.getVersionNo());
        detail.setVersionLabel("v" + version.getVersionNo());
        detail.setTitle(version.getTitle());
        detail.setContent(version.getContent());
        detail.setLanguage(version.getLanguage());
        detail.setTagNames(decodeTagNames(version.getTagNamesJson()));
        detail.setSummary(version.getSummary());
        detail.setEditorName(VERSION_EDITOR_NAME_SELF);
        detail.setCreatedAt(version.getCreatedAt());
        return detail;
    }

    private int createNoteVersion(Long userId,
                                  Long noteId,
                                  String title,
                                  String content,
                                  String language,
                                  List<String> tagNames,
                                  String summary) {
        int nextVersionNo = nextVersionNo(userId, noteId);
        NoteVersion version = new NoteVersion();
        version.setUserId(userId);
        version.setNoteId(noteId);
        version.setVersionNo(nextVersionNo);
        version.setTitle(title);
        version.setContent(content);
        version.setLanguage(language);
        version.setTagNamesJson(encodeTagNames(normalizeTagNames(tagNames)));
        version.setSummary(trimSummary(summary));
        version.setCreatedAt(LocalDateTime.now());
        noteVersionMapper.insert(version);
        trimOldVersions(userId, noteId);
        return nextVersionNo;
    }

    private int nextVersionNo(Long userId, Long noteId) {
        NoteVersion latest = noteVersionMapper.selectOne(new LambdaQueryWrapper<NoteVersion>()
                .eq(NoteVersion::getUserId, userId)
                .eq(NoteVersion::getNoteId, noteId)
                .orderByDesc(NoteVersion::getVersionNo)
                .last("limit 1"));
        return latest == null ? 1 : latest.getVersionNo() + 1;
    }

    private void trimOldVersions(Long userId, Long noteId) {
        int maxVersions = noteVersionProperties.getMaxVersionsPerNote() == null
                ? DEFAULT_MAX_VERSIONS_PER_NOTE
                : noteVersionProperties.getMaxVersionsPerNote();
        if (maxVersions < 1) {
            return;
        }
        List<NoteVersion> orderedVersions = noteVersionMapper.selectList(new LambdaQueryWrapper<NoteVersion>()
                .eq(NoteVersion::getUserId, userId)
                .eq(NoteVersion::getNoteId, noteId)
                .orderByAsc(NoteVersion::getVersionNo));
        if (orderedVersions.size() <= maxVersions) {
            return;
        }
        int overflowCount = orderedVersions.size() - maxVersions;
        for (int i = 0; i < overflowCount; i++) {
            noteVersionMapper.deleteById(orderedVersions.get(i).getId());
        }
    }

    private String encodeTagNames(List<String> tagNames) {
        try {
            return objectMapper.writeValueAsString(normalizeTagNames(tagNames));
        } catch (Exception ex) {
            throw new BizException(500, "Failed to store note version tags");
        }
    }

    private List<String> decodeTagNames(String tagNamesJson) {
        if (!StringUtils.hasText(tagNamesJson)) {
            return Collections.emptyList();
        }
        try {
            List<String> tagNames = objectMapper.readValue(tagNamesJson, TAG_NAME_LIST_TYPE);
            return normalizeTagNames(tagNames);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String normalizeCoverPath(Long userId, String coverPath) {
        if (!StringUtils.hasText(coverPath)) {
            return null;
        }
        String cleaned = coverPath.replace('\\', '/').trim();
        String prefix = "note/" + userId + "/";
        if (cleaned.contains("..") || cleaned.startsWith("/") || !cleaned.startsWith(prefix)) {
            throw new BizException(400, "Invalid cover path");
        }
        return cleaned;
    }

    private String sanitizeContent(String content) {
        if (content == null) {
            return "";
        }
        String value = content.replace("\u0000", "").trim();
        if (!StringUtils.hasText(value)) {
            return "";
        }
        value = value.replaceAll("(?is)<script[^>]*>.*?</script>", "")
                .replaceAll("(?is)<iframe[^>]*>.*?</iframe>", "")
                .replaceAll("(?is)<object[^>]*>.*?</object>", "");
        return value;
    }

    private void ensureOwnedNotes(Long userId, Set<Long> noteIds) {
        List<StudyNote> notes = noteMapper.selectList(new LambdaQueryWrapper<StudyNote>()
                .eq(StudyNote::getUserId, userId)
                .in(StudyNote::getId, noteIds));
        if (notes.size() != noteIds.size()) {
            throw new BizException(400, "Invalid note links");
        }
    }

    private LinkedHashSet<Long> normalizeIds(List<Long> ids) {
        LinkedHashSet<Long> dedup = new LinkedHashSet<>();
        if (CollectionUtils.isEmpty(ids)) {
            return dedup;
        }
        for (Long id : ids) {
            if (id != null) {
                dedup.add(id);
            }
        }
        return dedup;
    }

    private Set<Long> mergeIntersection(Set<Long> base, Set<Long> incoming) {
        if (base == null) {
            return new HashSet<>(incoming);
        }
        if (base.isEmpty() || incoming.isEmpty()) {
            return Collections.emptySet();
        }
        base.retainAll(incoming);
        return base;
    }

    private Set<Long> intersectionCopy(Set<Long> base, Set<Long> incoming) {
        if (base == null) {
            return new HashSet<>(incoming);
        }
        if (base.isEmpty() || incoming.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> copy = new HashSet<>(base);
        copy.retainAll(incoming);
        return copy;
    }

    private void replaceQuestionLinks(Long userId, Long noteId, List<Long> questionIds) {
        clearQuestionLinks(userId, noteId);
        LinkedHashSet<Long> dedupIds = normalizeIds(questionIds);
        if (dedupIds.isEmpty()) {
            return;
        }

        List<ErrorQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                .in(ErrorQuestion::getId, dedupIds)
                .eq(ErrorQuestion::getUserId, userId));
        if (questions.size() != dedupIds.size()) {
            throw new BizException(400, "Invalid manual question links");
        }

        for (Long questionId : dedupIds) {
            NoteQuestionLink link = new NoteQuestionLink();
            link.setUserId(userId);
            link.setNoteId(noteId);
            link.setQuestionId(questionId);
            noteQuestionLinkMapper.insert(link);
        }
    }

    private void replaceMaterialLinks(Long userId, Long noteId, List<Long> materialIds) {
        clearMaterialLinks(userId, noteId);
        LinkedHashSet<Long> dedupIds = normalizeIds(materialIds);
        if (dedupIds.isEmpty()) {
            return;
        }

        List<StudyMaterial> materials = materialMapper.selectList(new LambdaQueryWrapper<StudyMaterial>()
                .in(StudyMaterial::getId, dedupIds)
                .eq(StudyMaterial::getUserId, userId));
        if (materials.size() != dedupIds.size()) {
            throw new BizException(400, "Invalid manual material links");
        }

        for (Long materialId : dedupIds) {
            NoteMaterialLink link = new NoteMaterialLink();
            link.setUserId(userId);
            link.setNoteId(noteId);
            link.setMaterialId(materialId);
            noteMaterialLinkMapper.insert(link);
        }
    }

    private void clearQuestionLinks(Long userId, Long noteId) {
        noteQuestionLinkMapper.delete(new LambdaQueryWrapper<NoteQuestionLink>()
                .eq(NoteQuestionLink::getUserId, userId)
                .eq(NoteQuestionLink::getNoteId, noteId));
    }

    private void clearMaterialLinks(Long userId, Long noteId) {
        noteMaterialLinkMapper.delete(new LambdaQueryWrapper<NoteMaterialLink>()
                .eq(NoteMaterialLink::getUserId, userId)
                .eq(NoteMaterialLink::getNoteId, noteId));
    }

    private List<Long> listQuestionIdsByNoteId(Long userId, Long noteId) {
        return noteQuestionLinkMapper.selectList(new LambdaQueryWrapper<NoteQuestionLink>()
                        .eq(NoteQuestionLink::getUserId, userId)
                        .eq(NoteQuestionLink::getNoteId, noteId))
                .stream()
                .map(NoteQuestionLink::getQuestionId)
                .collect(Collectors.toList());
    }

    private List<Long> listMaterialIdsByNoteId(Long userId, Long noteId) {
        return noteMaterialLinkMapper.selectList(new LambdaQueryWrapper<NoteMaterialLink>()
                        .eq(NoteMaterialLink::getUserId, userId)
                        .eq(NoteMaterialLink::getNoteId, noteId))
                .stream()
                .map(NoteMaterialLink::getMaterialId)
                .collect(Collectors.toList());
    }

    private Map<Long, Long> listQuestionCountMap(Long userId, List<Long> noteIds) {
        if (CollectionUtils.isEmpty(noteIds)) {
            return Collections.emptyMap();
        }
        List<NoteQuestionLink> links = noteQuestionLinkMapper.selectList(new LambdaQueryWrapper<NoteQuestionLink>()
                .eq(NoteQuestionLink::getUserId, userId)
                .in(NoteQuestionLink::getNoteId, noteIds));
        if (links.isEmpty()) {
            return Collections.emptyMap();
        }
        return links.stream().collect(Collectors.groupingBy(NoteQuestionLink::getNoteId, Collectors.counting()));
    }

    private Map<Long, Long> listMaterialCountMap(Long userId, List<Long> noteIds) {
        if (CollectionUtils.isEmpty(noteIds)) {
            return Collections.emptyMap();
        }
        List<NoteMaterialLink> links = noteMaterialLinkMapper.selectList(new LambdaQueryWrapper<NoteMaterialLink>()
                .eq(NoteMaterialLink::getUserId, userId)
                .in(NoteMaterialLink::getNoteId, noteIds));
        if (links.isEmpty()) {
            return Collections.emptyMap();
        }
        return links.stream().collect(Collectors.groupingBy(NoteMaterialLink::getNoteId, Collectors.counting()));
    }

    private Set<Long> listFavoriteIdSet(Long userId, List<Long> noteIds) {
        if (CollectionUtils.isEmpty(noteIds)) {
            return Collections.emptySet();
        }
        List<NoteFavorite> favorites = noteFavoriteMapper.selectList(new LambdaQueryWrapper<NoteFavorite>()
                .eq(NoteFavorite::getUserId, userId)
                .in(NoteFavorite::getNoteId, noteIds));
        if (favorites.isEmpty()) {
            return Collections.emptySet();
        }
        return favorites.stream().map(NoteFavorite::getNoteId).collect(Collectors.toSet());
    }

    private Set<Long> listAllFavoriteIdSet(Long userId) {
        List<NoteFavorite> favorites = noteFavoriteMapper.selectList(new LambdaQueryWrapper<NoteFavorite>()
                .eq(NoteFavorite::getUserId, userId));
        if (favorites.isEmpty()) {
            return Collections.emptySet();
        }
        return favorites.stream().map(NoteFavorite::getNoteId).collect(Collectors.toSet());
    }

    private boolean isNoteFavorited(Long userId, Long noteId) {
        return noteFavoriteMapper.selectCount(new LambdaQueryWrapper<NoteFavorite>()
                .eq(NoteFavorite::getUserId, userId)
                .eq(NoteFavorite::getNoteId, noteId)) > 0;
    }

    private StudyNote ensureVersionAccessible(Long userId, Long noteId) {
        StudyNote note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BizException(404, "Note not found");
        }
        if (!Objects.equals(note.getUserId(), userId)) {
            throw new BizException(403, "No permission to access note versions");
        }
        return note;
    }

    private NoteVersion getOwnedVersion(Long userId, Long noteId, Long versionId) {
        NoteVersion version = noteVersionMapper.selectOne(new LambdaQueryWrapper<NoteVersion>()
                .eq(NoteVersion::getId, versionId)
                .eq(NoteVersion::getUserId, userId)
                .eq(NoteVersion::getNoteId, noteId)
                .last("limit 1"));
        if (version == null) {
            throw new BizException(404, "Version not found");
        }
        return version;
    }

    private StudyNote getOwnedNote(Long userId, Long noteId) {
        StudyNote note = noteMapper.selectOne(new LambdaQueryWrapper<StudyNote>()
                .eq(StudyNote::getId, noteId)
                .eq(StudyNote::getUserId, userId)
                .last("limit 1"));
        if (note == null) {
            throw new BizException(404, "Note not found");
        }
        return note;
    }
}
