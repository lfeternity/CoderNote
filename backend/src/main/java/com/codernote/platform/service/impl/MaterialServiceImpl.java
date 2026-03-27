package com.codernote.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.material.MaterialAttachmentVO;
import com.codernote.platform.dto.material.MaterialDetailVO;
import com.codernote.platform.dto.material.MaterialListItemVO;
import com.codernote.platform.dto.material.MaterialRichFieldPayload;
import com.codernote.platform.dto.material.MaterialSaveRequest;
import com.codernote.platform.entity.ErrorQuestion;
import com.codernote.platform.entity.MaterialFavorite;
import com.codernote.platform.entity.NoteMaterialLink;
import com.codernote.platform.entity.StudyMaterial;
import com.codernote.platform.entity.StudyNote;
import com.codernote.platform.entity.Tag;
import com.codernote.platform.enums.MaterialType;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.mapper.ErrorQuestionMapper;
import com.codernote.platform.mapper.MaterialFavoriteMapper;
import com.codernote.platform.mapper.NoteMaterialLinkMapper;
import com.codernote.platform.mapper.StudyMaterialMapper;
import com.codernote.platform.mapper.StudyNoteMapper;
import com.codernote.platform.mapper.TagMapper;
import com.codernote.platform.service.ManualLinkService;
import com.codernote.platform.service.MaterialService;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MaterialServiceImpl implements MaterialService {

    private final StudyMaterialMapper materialMapper;
    private final MaterialFavoriteMapper materialFavoriteMapper;
    private final ErrorQuestionMapper questionMapper;
    private final StudyNoteMapper noteMapper;
    private final NoteMaterialLinkMapper noteMaterialLinkMapper;
    private final TagMapper tagMapper;
    private final TagRelationHelperService tagRelationHelperService;
    private final ManualLinkService manualLinkService;
    private final ObjectMapper objectMapper;

    public MaterialServiceImpl(StudyMaterialMapper materialMapper,
                               MaterialFavoriteMapper materialFavoriteMapper,
                               ErrorQuestionMapper questionMapper,
                               StudyNoteMapper noteMapper,
                               NoteMaterialLinkMapper noteMaterialLinkMapper,
                               TagMapper tagMapper,
                               TagRelationHelperService tagRelationHelperService,
                               ManualLinkService manualLinkService,
                               ObjectMapper objectMapper) {
        this.materialMapper = materialMapper;
        this.materialFavoriteMapper = materialFavoriteMapper;
        this.questionMapper = questionMapper;
        this.noteMapper = noteMapper;
        this.noteMaterialLinkMapper = noteMaterialLinkMapper;
        this.tagMapper = tagMapper;
        this.tagRelationHelperService = tagRelationHelperService;
        this.manualLinkService = manualLinkService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, MaterialSaveRequest request) {
        validateRequest(request);
        StudyMaterial material = new StudyMaterial();
        material.setUserId(userId);
        material.setTitle(request.getTitle().trim());
        material.setMaterialType(request.getMaterialType().trim());
        material.setLanguage(request.getLanguage().trim());
        material.setCoverPath(normalizeCoverPath(userId, request.getCoverPath()));
        material.setContent(encodeRichContent(userId, request.getContent(), request.getContentAttachments()));
        material.setSource(request.getSource());
        material.setRemark(request.getRemark());
        material.setCreatedAt(LocalDateTime.now());
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.insert(material);

        List<Long> tagIds = tagRelationHelperService.resolveTagIds(request.getTagNames(), userId);
        tagRelationHelperService.replaceMaterialTags(material.getId(), tagIds);

        validateManualQuestionIds(userId, request.getManualQuestionIds());
        manualLinkService.replaceMaterialLinks(userId, material.getId(), request.getManualQuestionIds());
        return material.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long materialId, MaterialSaveRequest request) {
        validateRequest(request);
        StudyMaterial material = getOwnedMaterial(userId, materialId);
        material.setTitle(request.getTitle().trim());
        material.setMaterialType(request.getMaterialType().trim());
        material.setLanguage(request.getLanguage().trim());
        material.setCoverPath(normalizeCoverPath(userId, request.getCoverPath()));
        material.setContent(encodeRichContent(userId, request.getContent(), request.getContentAttachments()));
        material.setSource(request.getSource());
        material.setRemark(request.getRemark());
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.updateById(material);

        List<Long> tagIds = tagRelationHelperService.resolveTagIds(request.getTagNames(), userId);
        tagRelationHelperService.replaceMaterialTags(materialId, tagIds);

        validateManualQuestionIds(userId, request.getManualQuestionIds());
        manualLinkService.replaceMaterialLinks(userId, materialId, request.getManualQuestionIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long materialId) {
        StudyMaterial material = getOwnedMaterial(userId, materialId);
        materialMapper.deleteById(material.getId());
        tagRelationHelperService.replaceMaterialTags(materialId, Collections.emptyList());
        manualLinkService.clearByMaterialId(userId, materialId);
        materialFavoriteMapper.delete(new LambdaQueryWrapper<MaterialFavorite>()
                .eq(MaterialFavorite::getMaterialId, materialId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(Long userId, List<Long> materialIds) {
        if (CollectionUtils.isEmpty(materialIds)) {
            return;
        }
        for (Long materialId : materialIds) {
            delete(userId, materialId);
        }
    }

    @Override
    public PageResult<MaterialListItemVO> page(Long userId,
                                               Long pageNo,
                                               Long pageSize,
                                               String materialType,
                                               String language,
                                               String tag,
                                               String keyword) {
        return pageInternal(userId, pageNo, pageSize, materialType, language, tag, keyword, null);
    }

    @Override
    public MaterialDetailVO detail(Long userId, Long materialId) {
        StudyMaterial material = getOwnedMaterial(userId, materialId);

        MaterialDetailVO vo = new MaterialDetailVO();
        vo.setId(material.getId());
        vo.setTitle(material.getTitle());
        vo.setMaterialType(material.getMaterialType());
        vo.setLanguage(material.getLanguage());
        vo.setCoverPath(material.getCoverPath());

        MaterialRichFieldPayload contentField = decodeRichContent(material.getContent());
        fillAttachmentUrls(contentField.getAttachments());
        vo.setContent(contentField.getText());
        vo.setContentAttachments(contentField.getAttachments());

        vo.setSource(material.getSource());
        vo.setRemark(material.getRemark());
        vo.setCreatedAt(material.getCreatedAt());
        vo.setUpdatedAt(material.getUpdatedAt());
        vo.setFavorite(isMaterialFavorited(userId, materialId));

        List<String> tagNames = tagRelationHelperService.listTagNamesByMaterialId(materialId);
        vo.setTagNames(tagNames);

        List<Long> materialTagIds = tagRelationHelperService.listTagIdsByMaterialId(materialId);
        Set<Long> autoQuestionIds = materialTagIds.isEmpty()
                ? Collections.emptySet()
                : tagRelationHelperService.findQuestionIdsByTagIds(materialTagIds);
        List<Long> manualQuestionIds = manualLinkService.listQuestionIdsByMaterialId(userId, materialId);
        vo.setManualQuestionIds(manualQuestionIds);

        LinkedHashSet<Long> relatedIds = new LinkedHashSet<>();
        relatedIds.addAll(manualQuestionIds);
        relatedIds.addAll(autoQuestionIds);

        List<Long> linkedNoteIds = noteMaterialLinkMapper.selectList(new LambdaQueryWrapper<NoteMaterialLink>()
                        .eq(NoteMaterialLink::getUserId, userId)
                        .eq(NoteMaterialLink::getMaterialId, materialId))
                .stream()
                .map(NoteMaterialLink::getNoteId)
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
                List<MaterialDetailVO.LinkedNoteVO> relatedNotes = linkedNotes.stream().map(note -> {
                    MaterialDetailVO.LinkedNoteVO linked = new MaterialDetailVO.LinkedNoteVO();
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
            vo.setRelatedQuestions(Collections.emptyList());
            return vo;
        }

        List<ErrorQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                .eq(ErrorQuestion::getUserId, userId)
                .in(ErrorQuestion::getId, relatedIds));
        if (questions.isEmpty()) {
            vo.setRelatedQuestions(Collections.emptyList());
            return vo;
        }

        Map<Long, List<String>> questionTagMap = tagRelationHelperService.questionTagNameMap(
                questions.stream().map(ErrorQuestion::getId).collect(Collectors.toList())
        );

        Set<String> materialTagNameSet = new LinkedHashSet<>(tagNames);
        List<MaterialDetailVO.LinkedQuestionVO> linkedQuestions = questions.stream().map(question -> {
            MaterialDetailVO.LinkedQuestionVO linked = new MaterialDetailVO.LinkedQuestionVO();
            linked.setId(question.getId());
            linked.setTitle(question.getTitle());
            linked.setLanguage(question.getLanguage());
            linked.setMasteryStatus(question.getMasteryStatus());
            linked.setTagNames(questionTagMap.getOrDefault(question.getId(), Collections.emptyList()));
            return linked;
        }).collect(Collectors.toList());

        linkedQuestions.sort(Comparator.comparingInt((MaterialDetailVO.LinkedQuestionVO item) ->
                matchCount(materialTagNameSet, item.getTagNames())).reversed());

        vo.setRelatedQuestions(linkedQuestions);
        return vo;
    }

    @Override
    public List<MaterialListItemVO> listByTagId(Long userId, Long tagId) {
        Set<Long> materialIds = tagRelationHelperService.findMaterialIdsByTagIds(List.of(tagId));
        if (materialIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<StudyMaterial> materials = materialMapper.selectList(new LambdaQueryWrapper<StudyMaterial>()
                .eq(StudyMaterial::getUserId, userId)
                .in(StudyMaterial::getId, materialIds)
                .orderByDesc(StudyMaterial::getCreatedAt));
        if (materials.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> recordIds = materials.stream().map(StudyMaterial::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagMap = tagRelationHelperService.materialTagNameMap(recordIds);
        Set<Long> favoriteIdSet = listFavoriteIdSet(userId, recordIds);

        List<MaterialListItemVO> voList = new ArrayList<>();
        for (StudyMaterial item : materials) {
            MaterialListItemVO vo = new MaterialListItemVO();
            vo.setId(item.getId());
            vo.setTitle(item.getTitle());
            vo.setMaterialType(item.getMaterialType());
            vo.setLanguage(item.getLanguage());
            vo.setCoverPath(item.getCoverPath());
            vo.setCreatedAt(item.getCreatedAt());
            vo.setTagNames(tagMap.getOrDefault(item.getId(), Collections.emptyList()));
            vo.setFavorite(favoriteIdSet.contains(item.getId()));
            voList.add(vo);
        }
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long userId, Long materialId) {
        StudyMaterial material = getOwnedMaterial(userId, materialId);
        MaterialFavorite exists = materialFavoriteMapper.selectOne(new LambdaQueryWrapper<MaterialFavorite>()
                .eq(MaterialFavorite::getUserId, userId)
                .eq(MaterialFavorite::getMaterialId, material.getId())
                .last("limit 1"));
        if (exists != null) {
            return;
        }
        MaterialFavorite favorite = new MaterialFavorite();
        favorite.setUserId(userId);
        favorite.setMaterialId(material.getId());
        favorite.setCreatedAt(LocalDateTime.now());
        materialFavoriteMapper.insert(favorite);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavorite(Long userId, Long materialId) {
        getOwnedMaterial(userId, materialId);
        materialFavoriteMapper.delete(new LambdaQueryWrapper<MaterialFavorite>()
                .eq(MaterialFavorite::getUserId, userId)
                .eq(MaterialFavorite::getMaterialId, materialId));
    }

    @Override
    public PageResult<MaterialListItemVO> favoritePage(Long userId,
                                                       Long pageNo,
                                                       Long pageSize,
                                                       String materialType,
                                                       String language,
                                                       String tag,
                                                       String keyword) {
        Set<Long> favoriteIdSet = listAllFavoriteIdSet(userId);
        if (favoriteIdSet.isEmpty()) {
            long pageNum = pageNo == null || pageNo < 1 ? 1L : pageNo;
            long size = pageSize == null || pageSize < 1 ? 10L : pageSize;
            return PageResult.empty(pageNum, size);
        }
        return pageInternal(userId, pageNo, pageSize, materialType, language, tag, keyword, favoriteIdSet);
    }

    private PageResult<MaterialListItemVO> pageInternal(Long userId,
                                                        Long pageNo,
                                                        Long pageSize,
                                                        String materialType,
                                                        String language,
                                                        String tag,
                                                        String keyword,
                                                        Set<Long> allowedMaterialIds) {
        long pageNum = pageNo == null || pageNo < 1 ? 1L : pageNo;
        long size = pageSize == null || pageSize < 1 ? 10L : pageSize;

        LambdaQueryWrapper<StudyMaterial> wrapper = new LambdaQueryWrapper<StudyMaterial>()
                .eq(StudyMaterial::getUserId, userId)
                .orderByDesc(StudyMaterial::getCreatedAt);

        if (StringUtils.hasText(materialType)) {
            wrapper.eq(StudyMaterial::getMaterialType, materialType.trim());
        }
        if (StringUtils.hasText(language)) {
            wrapper.eq(StudyMaterial::getLanguage, language.trim());
        }

        Set<Long> restrictedIds = null;
        if (allowedMaterialIds != null) {
            restrictedIds = new HashSet<>(allowedMaterialIds);
            if (restrictedIds.isEmpty()) {
                return PageResult.empty(pageNum, size);
            }
        }

        if (StringUtils.hasText(tag)) {
            Tag exactTag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getName, tag.trim())
                    .last("limit 1"));
            if (exactTag == null) {
                return PageResult.empty(pageNum, size);
            }
            Set<Long> tagFilteredMaterialIds = tagRelationHelperService.findMaterialIdsByTagIds(List.of(exactTag.getId()));
            if (tagFilteredMaterialIds.isEmpty()) {
                return PageResult.empty(pageNum, size);
            }
            restrictedIds = mergeIntersection(restrictedIds, tagFilteredMaterialIds);
            if (restrictedIds.isEmpty()) {
                return PageResult.empty(pageNum, size);
            }
        }

        if (StringUtils.hasText(keyword)) {
            String key = keyword.trim();
            List<Tag> keywordTags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                    .like(Tag::getName, key));
            Set<Long> keywordTagMaterialIds = keywordTags.isEmpty()
                    ? Collections.emptySet()
                    : tagRelationHelperService.findMaterialIdsByTagIds(keywordTags.stream().map(Tag::getId).collect(Collectors.toList()));

            if (restrictedIds == null) {
                if (keywordTagMaterialIds.isEmpty()) {
                    wrapper.like(StudyMaterial::getTitle, key);
                } else {
                    wrapper.and(w -> w.like(StudyMaterial::getTitle, key)
                            .or()
                            .in(StudyMaterial::getId, keywordTagMaterialIds));
                }
            } else {
                if (keywordTagMaterialIds.isEmpty()) {
                    wrapper.like(StudyMaterial::getTitle, key);
                } else {
                    Set<Long> restrictedKeywordIds = intersectionCopy(restrictedIds, keywordTagMaterialIds);
                    if (restrictedKeywordIds.isEmpty()) {
                        wrapper.like(StudyMaterial::getTitle, key);
                    } else {
                        wrapper.and(w -> w.like(StudyMaterial::getTitle, key)
                                .or()
                                .in(StudyMaterial::getId, restrictedKeywordIds));
                    }
                }
            }
        }

        if (restrictedIds != null) {
            wrapper.in(StudyMaterial::getId, restrictedIds);
        }

        Page<StudyMaterial> page = materialMapper.selectPage(new Page<>(pageNum, size), wrapper);
        List<StudyMaterial> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(pageNum, size);
        }

        List<Long> materialIds = records.stream().map(StudyMaterial::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagMap = tagRelationHelperService.materialTagNameMap(materialIds);
        Set<Long> favoriteIdSet = listFavoriteIdSet(userId, materialIds);

        List<MaterialListItemVO> voList = records.stream().map(item -> {
            MaterialListItemVO vo = new MaterialListItemVO();
            vo.setId(item.getId());
            vo.setTitle(item.getTitle());
            vo.setMaterialType(item.getMaterialType());
            vo.setLanguage(item.getLanguage());
            vo.setCoverPath(item.getCoverPath());
            vo.setCreatedAt(item.getCreatedAt());
            vo.setTagNames(tagMap.getOrDefault(item.getId(), Collections.emptyList()));
            vo.setFavorite(favoriteIdSet.contains(item.getId()));
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(page.getTotal(), pageNum, size, voList);
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
    private Set<Long> listFavoriteIdSet(Long userId, List<Long> materialIds) {
        if (CollectionUtils.isEmpty(materialIds)) {
            return Collections.emptySet();
        }
        List<MaterialFavorite> favorites = materialFavoriteMapper.selectList(new LambdaQueryWrapper<MaterialFavorite>()
                .eq(MaterialFavorite::getUserId, userId)
                .in(MaterialFavorite::getMaterialId, materialIds));
        if (favorites.isEmpty()) {
            return Collections.emptySet();
        }
        return favorites.stream().map(MaterialFavorite::getMaterialId).collect(Collectors.toSet());
    }

    private Set<Long> listAllFavoriteIdSet(Long userId) {
        List<MaterialFavorite> favorites = materialFavoriteMapper.selectList(new LambdaQueryWrapper<MaterialFavorite>()
                .eq(MaterialFavorite::getUserId, userId));
        if (favorites.isEmpty()) {
            return Collections.emptySet();
        }
        return favorites.stream().map(MaterialFavorite::getMaterialId).collect(Collectors.toSet());
    }

    private boolean isMaterialFavorited(Long userId, Long materialId) {
        return materialFavoriteMapper.selectCount(new LambdaQueryWrapper<MaterialFavorite>()
                .eq(MaterialFavorite::getUserId, userId)
                .eq(MaterialFavorite::getMaterialId, materialId)) > 0;
    }

    private void validateRequest(MaterialSaveRequest request) {
        if (!MaterialType.isValid(request.getMaterialType())) {
            throw new BizException(400, "Invalid material type");
        }

        boolean hasContentText = StringUtils.hasText(request.getContent());
        boolean hasContentAttachments = !CollectionUtils.isEmpty(request.getContentAttachments());
        if (!hasContentText && !hasContentAttachments) {
            throw new BizException(400, "Content text/link or attachments are required");
        }

        if (("VIDEO_LINK".equals(request.getMaterialType()) || "DOC_LINK".equals(request.getMaterialType()))
                && !hasContentAttachments
                && !isHttpUrl(request.getContent())) {
            throw new BizException(400, "Link materials require a valid URL when no attachment is provided");
        }
    }

    private String encodeRichContent(Long userId, String text, List<MaterialAttachmentVO> attachments) {
        List<MaterialAttachmentVO> safeAttachments = sanitizeAttachments(userId, attachments);
        String safeText = text == null ? "" : text;

        if (safeAttachments.isEmpty()) {
            return safeText;
        }

        MaterialRichFieldPayload payload = new MaterialRichFieldPayload();
        payload.setText(safeText);
        payload.setAttachments(safeAttachments);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BizException(500, "Failed to encode content attachments");
        }
    }

    private MaterialRichFieldPayload decodeRichContent(String rawValue) {
        MaterialRichFieldPayload fallback = new MaterialRichFieldPayload();
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
            MaterialRichFieldPayload payload = objectMapper.readValue(trimmed, MaterialRichFieldPayload.class);
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

    private List<MaterialAttachmentVO> sanitizeAttachments(Long userId, List<MaterialAttachmentVO> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            return Collections.emptyList();
        }

        String expectedPrefix = "material/" + userId + "/";
        LinkedHashSet<String> pathDedup = new LinkedHashSet<>();
        List<MaterialAttachmentVO> safeList = new ArrayList<>();
        for (MaterialAttachmentVO item : attachments) {
            if (item == null || !StringUtils.hasText(item.getPath())) {
                continue;
            }

            String cleanedPath = item.getPath().replace('\\', '/').trim();
            if (cleanedPath.contains("..") || cleanedPath.startsWith("/") || !cleanedPath.startsWith(expectedPrefix)) {
                throw new BizException(400, "Invalid content attachment path");
            }
            if (!pathDedup.add(cleanedPath)) {
                continue;
            }

            MaterialAttachmentVO safe = new MaterialAttachmentVO();
            safe.setPath(cleanedPath);
            safe.setFileName(StringUtils.hasText(item.getFileName()) ? item.getFileName().trim() : cleanedPath.substring(cleanedPath.lastIndexOf('/') + 1));
            safe.setContentType(item.getContentType());
            safe.setSize(item.getSize());
            safe.setImage(isImageByType(safe.getFileName(), safe.getContentType()));
            safeList.add(safe);
        }
        return safeList;
    }

    private void fillAttachmentUrls(List<MaterialAttachmentVO> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            return;
        }
        for (MaterialAttachmentVO item : attachments) {
            if (item == null || !StringUtils.hasText(item.getPath())) {
                continue;
            }
            String fileName = StringUtils.hasText(item.getFileName()) ? item.getFileName() : "file";
            String preview = "/api/v1/file/download?path=" + encodeUrl(item.getPath()) + "&name=" + encodeUrl(fileName);
            item.setPreviewUrl(preview);
            item.setDownloadUrl(preview + "&download=true");
        }
    }

    private boolean isHttpUrl(String value) {
        return StringUtils.hasText(value) && value.trim().matches("^(https?://).+");
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

    private void validateManualQuestionIds(Long userId, List<Long> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return;
        }
        List<ErrorQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                .in(ErrorQuestion::getId, questionIds)
                .eq(ErrorQuestion::getUserId, userId));
        if (questions.size() != new LinkedHashSet<>(questionIds).size()) {
            throw new BizException(400, "Invalid manual question links");
        }
    }

    private String normalizeCoverPath(Long userId, String coverPath) {
        if (!StringUtils.hasText(coverPath)) {
            return null;
        }
        String cleaned = coverPath.replace('\\', '/').trim();
        String prefix = "material/" + userId + "/";
        if (cleaned.contains("..") || cleaned.startsWith("/") || !cleaned.startsWith(prefix)) {
            throw new BizException(400, "Invalid cover path");
        }
        return cleaned;
    }

    private StudyMaterial getOwnedMaterial(Long userId, Long materialId) {
        StudyMaterial material = materialMapper.selectOne(new LambdaQueryWrapper<StudyMaterial>()
                .eq(StudyMaterial::getId, materialId)
                .eq(StudyMaterial::getUserId, userId)
                .last("limit 1"));
        if (material == null) {
            throw new BizException(404, "Material not found");
        }
        return material;
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
