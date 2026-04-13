package com.codernote.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codernote.platform.dto.material.MaterialListItemVO;
import com.codernote.platform.dto.note.NoteListItemVO;
import com.codernote.platform.dto.question.QuestionListItemVO;
import com.codernote.platform.dto.tag.TagListItemVO;
import com.codernote.platform.dto.tag.TagRelationVO;
import com.codernote.platform.entity.Tag;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.mapper.TagMapper;
import com.codernote.platform.service.MaterialService;
import com.codernote.platform.service.NoteService;
import com.codernote.platform.service.QuestionService;
import com.codernote.platform.service.TagRelationHelperService;
import com.codernote.platform.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final TagRelationHelperService tagRelationHelperService;
    private final QuestionService questionService;
    private final MaterialService materialService;
    private final NoteService noteService;

    public TagServiceImpl(TagMapper tagMapper,
                          TagRelationHelperService tagRelationHelperService,
                          QuestionService questionService,
                          MaterialService materialService,
                          NoteService noteService) {
        this.tagMapper = tagMapper;
        this.tagRelationHelperService = tagRelationHelperService;
        this.questionService = questionService;
        this.materialService = materialService;
        this.noteService = noteService;
    }

    @Override
    public List<TagListItemVO> list(Long userId) {
        List<Tag> allTags = tagMapper.selectList(new LambdaQueryWrapper<Tag>());
        List<TagListItemVO> voList = new ArrayList<>();
        for (Tag tag : allTags) {
            Long usageCount = tagRelationHelperService.usageCount(tag.getId());
            boolean editable = userId.equals(tag.getCreatorUserId());
            TagListItemVO vo = new TagListItemVO();
            vo.setId(tag.getId());
            vo.setName(tag.getName());
            vo.setCoverPath(tag.getCoverPath());
            vo.setUsageCount(usageCount);
            vo.setCanEdit(editable);
            vo.setCanDelete(editable && usageCount == 0);
            voList.add(vo);
        }
        voList.sort(Comparator.comparing(TagListItemVO::getUsageCount).reversed());
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, String name, String coverPath) {
        if (!StringUtils.hasText(name)) {
            throw new BizException(400, "Tag name is required");
        }
        String normalized = name.trim();
        String normalizedCoverPath = normalizeCoverPath(userId, coverPath);
        Tag existing = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, normalized)
                .last("limit 1"));
        if (existing != null) {
            return existing.getId();
        }
        Tag tag = new Tag();
        tag.setName(normalized);
        tag.setCoverPath(normalizedCoverPath);
        tag.setCreatorUserId(userId);
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUpdatedAt(LocalDateTime.now());
        tagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long tagId, String newName, String coverPath) {
        if (!StringUtils.hasText(newName)) {
            throw new BizException(400, "Tag name is required");
        }
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BizException(404, "Tag not found");
        }
        if (!userId.equals(tag.getCreatorUserId())) {
            throw new BizException(403, "No permission to edit this tag");
        }
        Tag duplicate = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, newName.trim())
                .ne(Tag::getId, tagId)
                .last("limit 1"));
        if (duplicate != null) {
            throw new BizException(400, "Tag name already exists");
        }
        tag.setName(newName.trim());
        tag.setCoverPath(normalizeCoverPath(userId, coverPath));
        tag.setUpdatedAt(LocalDateTime.now());
        tagMapper.updateById(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BizException(404, "Tag not found");
        }
        if (!userId.equals(tag.getCreatorUserId())) {
            throw new BizException(403, "No permission to delete this tag");
        }
        if (tagRelationHelperService.isUsed(tagId)) {
            throw new BizException(400, "Tag is still used and cannot be deleted");
        }
        tagMapper.deleteById(tagId);
    }

    @Override
    public TagRelationVO relation(Long userId, Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BizException(404, "Tag not found");
        }
        TagRelationVO vo = new TagRelationVO();
        vo.setTagId(tag.getId());
        vo.setTagName(tag.getName());
        vo.setUsageCount(tagRelationHelperService.usageCount(tagId));
        List<QuestionListItemVO> questionList = questionService.listByTagId(userId, tagId);
        List<MaterialListItemVO> materialList = materialService.listByTagId(userId, tagId);
        List<NoteListItemVO> noteList = noteService.listByTagId(userId, tagId);
        vo.setRelatedQuestions(questionList);
        vo.setRelatedMaterials(materialList);
        vo.setRelatedNotes(noteList);
        return vo;
    }

    private String normalizeCoverPath(Long userId, String coverPath) {
        if (!StringUtils.hasText(coverPath)) {
            return null;
        }
        String cleaned = coverPath.replace('\\', '/').trim();
        String expectedPrefix = "tag/" + userId + "/";
        if (!cleaned.startsWith(expectedPrefix)) {
            throw new BizException(400, "Invalid cover path");
        }
        return cleaned;
    }
}
