package com.codernote.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codernote.platform.entity.MaterialTag;
import com.codernote.platform.entity.NoteTag;
import com.codernote.platform.entity.QuestionTag;
import com.codernote.platform.entity.Tag;
import com.codernote.platform.mapper.MaterialTagMapper;
import com.codernote.platform.mapper.NoteTagMapper;
import com.codernote.platform.mapper.QuestionTagMapper;
import com.codernote.platform.mapper.TagMapper;
import com.codernote.platform.service.TagRelationHelperService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TagRelationHelperServiceImpl implements TagRelationHelperService {

    private final TagMapper tagMapper;
    private final QuestionTagMapper questionTagMapper;
    private final MaterialTagMapper materialTagMapper;
    private final NoteTagMapper noteTagMapper;

    public TagRelationHelperServiceImpl(TagMapper tagMapper,
                                        QuestionTagMapper questionTagMapper,
                                        MaterialTagMapper materialTagMapper,
                                        NoteTagMapper noteTagMapper) {
        this.tagMapper = tagMapper;
        this.questionTagMapper = questionTagMapper;
        this.materialTagMapper = materialTagMapper;
        this.noteTagMapper = noteTagMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> resolveTagIds(List<String> tagNames, Long userId) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> normalized = tagNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Long> result = new ArrayList<>();
        for (String tagName : normalized) {
            Tag exists = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getName, tagName)
                    .last("limit 1"));
            if (exists != null) {
                result.add(exists.getId());
                continue;
            }
            Tag tag = new Tag();
            tag.setName(tagName);
            tag.setCreatorUserId(userId);
            tag.setCreatedAt(LocalDateTime.now());
            tag.setUpdatedAt(LocalDateTime.now());
            tagMapper.insert(tag);
            result.add(tag.getId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceQuestionTags(Long questionId, List<Long> tagIds) {
        questionTagMapper.delete(new LambdaQueryWrapper<QuestionTag>()
                .eq(QuestionTag::getQuestionId, questionId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : new LinkedHashSet<>(tagIds)) {
            QuestionTag relation = new QuestionTag();
            relation.setQuestionId(questionId);
            relation.setTagId(tagId);
            questionTagMapper.insert(relation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceMaterialTags(Long materialId, List<Long> tagIds) {
        materialTagMapper.delete(new LambdaQueryWrapper<MaterialTag>()
                .eq(MaterialTag::getMaterialId, materialId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : new LinkedHashSet<>(tagIds)) {
            MaterialTag relation = new MaterialTag();
            relation.setMaterialId(materialId);
            relation.setTagId(tagId);
            materialTagMapper.insert(relation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceNoteTags(Long noteId, List<Long> tagIds) {
        noteTagMapper.delete(new LambdaQueryWrapper<NoteTag>()
                .eq(NoteTag::getNoteId, noteId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : new LinkedHashSet<>(tagIds)) {
            NoteTag relation = new NoteTag();
            relation.setNoteId(noteId);
            relation.setTagId(tagId);
            noteTagMapper.insert(relation);
        }
    }

    @Override
    public List<String> listTagNamesByQuestionId(Long questionId) {
        List<Long> tagIds = listTagIdsByQuestionId(questionId);
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        return tagMapper.selectBatchIds(tagIds).stream().map(Tag::getName).collect(Collectors.toList());
    }

    @Override
    public List<String> listTagNamesByMaterialId(Long materialId) {
        List<Long> tagIds = listTagIdsByMaterialId(materialId);
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        return tagMapper.selectBatchIds(tagIds).stream().map(Tag::getName).collect(Collectors.toList());
    }

    @Override
    public List<String> listTagNamesByNoteId(Long noteId) {
        List<Long> tagIds = listTagIdsByNoteId(noteId);
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        return tagMapper.selectBatchIds(tagIds).stream().map(Tag::getName).collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<String>> questionTagNameMap(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<QuestionTag> relations = questionTagMapper.selectList(new LambdaQueryWrapper<QuestionTag>()
                .in(QuestionTag::getQuestionId, questionIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> tagIds = relations.stream().map(QuestionTag::getTagId).collect(Collectors.toSet());
        Map<Long, String> tagNameMap = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        return relations.stream().collect(Collectors.groupingBy(
                QuestionTag::getQuestionId,
                Collectors.mapping(rel -> tagNameMap.getOrDefault(rel.getTagId(), ""), Collectors.toList())
        ));
    }

    @Override
    public Map<Long, List<String>> materialTagNameMap(Collection<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MaterialTag> relations = materialTagMapper.selectList(new LambdaQueryWrapper<MaterialTag>()
                .in(MaterialTag::getMaterialId, materialIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> tagIds = relations.stream().map(MaterialTag::getTagId).collect(Collectors.toSet());
        Map<Long, String> tagNameMap = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        return relations.stream().collect(Collectors.groupingBy(
                MaterialTag::getMaterialId,
                Collectors.mapping(rel -> tagNameMap.getOrDefault(rel.getTagId(), ""), Collectors.toList())
        ));
    }

    @Override
    public Map<Long, List<String>> noteTagNameMap(Collection<Long> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<NoteTag> relations = noteTagMapper.selectList(new LambdaQueryWrapper<NoteTag>()
                .in(NoteTag::getNoteId, noteIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> tagIds = relations.stream().map(NoteTag::getTagId).collect(Collectors.toSet());
        Map<Long, String> tagNameMap = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        return relations.stream().collect(Collectors.groupingBy(
                NoteTag::getNoteId,
                Collectors.mapping(rel -> tagNameMap.getOrDefault(rel.getTagId(), ""), Collectors.toList())
        ));
    }

    @Override
    public List<Long> listTagIdsByQuestionId(Long questionId) {
        return questionTagMapper.selectList(new LambdaQueryWrapper<QuestionTag>()
                        .eq(QuestionTag::getQuestionId, questionId))
                .stream()
                .map(QuestionTag::getTagId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> listTagIdsByMaterialId(Long materialId) {
        return materialTagMapper.selectList(new LambdaQueryWrapper<MaterialTag>()
                        .eq(MaterialTag::getMaterialId, materialId))
                .stream()
                .map(MaterialTag::getTagId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> listTagIdsByNoteId(Long noteId) {
        return noteTagMapper.selectList(new LambdaQueryWrapper<NoteTag>()
                        .eq(NoteTag::getNoteId, noteId))
                .stream()
                .map(NoteTag::getTagId)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Long> findMaterialIdsByTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Collections.emptySet();
        }
        return materialTagMapper.selectList(new LambdaQueryWrapper<MaterialTag>()
                        .in(MaterialTag::getTagId, tagIds))
                .stream()
                .map(MaterialTag::getMaterialId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> findQuestionIdsByTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Collections.emptySet();
        }
        return questionTagMapper.selectList(new LambdaQueryWrapper<QuestionTag>()
                        .in(QuestionTag::getTagId, tagIds))
                .stream()
                .map(QuestionTag::getQuestionId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> findNoteIdsByTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Collections.emptySet();
        }
        return noteTagMapper.selectList(new LambdaQueryWrapper<NoteTag>()
                        .in(NoteTag::getTagId, tagIds))
                .stream()
                .map(NoteTag::getNoteId)
                .collect(Collectors.toSet());
    }

    @Override
    public Long usageCount(Long tagId) {
        long questionCount = questionTagMapper.selectCount(new LambdaQueryWrapper<QuestionTag>()
                .eq(QuestionTag::getTagId, tagId));
        long materialCount = materialTagMapper.selectCount(new LambdaQueryWrapper<MaterialTag>()
                .eq(MaterialTag::getTagId, tagId));
        long noteCount = noteTagMapper.selectCount(new LambdaQueryWrapper<NoteTag>()
                .eq(NoteTag::getTagId, tagId));
        return questionCount + materialCount + noteCount;
    }

    @Override
    public boolean isUsed(Long tagId) {
        return usageCount(tagId) > 0;
    }
}
