package com.codernote.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codernote.platform.entity.QuestionMaterialLink;
import com.codernote.platform.mapper.QuestionMaterialLinkMapper;
import com.codernote.platform.service.ManualLinkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ManualLinkServiceImpl implements ManualLinkService {

    private final QuestionMaterialLinkMapper linkMapper;

    public ManualLinkServiceImpl(QuestionMaterialLinkMapper linkMapper) {
        this.linkMapper = linkMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceQuestionLinks(Long userId, Long questionId, List<Long> materialIds) {
        clearByQuestionId(userId, questionId);
        if (materialIds == null || materialIds.isEmpty()) {
            return;
        }
        for (Long materialId : new LinkedHashSet<>(materialIds)) {
            QuestionMaterialLink link = new QuestionMaterialLink();
            link.setUserId(userId);
            link.setQuestionId(questionId);
            link.setMaterialId(materialId);
            linkMapper.insert(link);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceMaterialLinks(Long userId, Long materialId, List<Long> questionIds) {
        clearByMaterialId(userId, materialId);
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }
        for (Long questionId : new LinkedHashSet<>(questionIds)) {
            QuestionMaterialLink link = new QuestionMaterialLink();
            link.setUserId(userId);
            link.setQuestionId(questionId);
            link.setMaterialId(materialId);
            linkMapper.insert(link);
        }
    }

    @Override
    public List<Long> listMaterialIdsByQuestionId(Long userId, Long questionId) {
        return linkMapper.selectList(new LambdaQueryWrapper<QuestionMaterialLink>()
                        .eq(QuestionMaterialLink::getUserId, userId)
                        .eq(QuestionMaterialLink::getQuestionId, questionId))
                .stream()
                .map(QuestionMaterialLink::getMaterialId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> listQuestionIdsByMaterialId(Long userId, Long materialId) {
        return linkMapper.selectList(new LambdaQueryWrapper<QuestionMaterialLink>()
                        .eq(QuestionMaterialLink::getUserId, userId)
                        .eq(QuestionMaterialLink::getMaterialId, materialId))
                .stream()
                .map(QuestionMaterialLink::getQuestionId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearByQuestionId(Long userId, Long questionId) {
        linkMapper.delete(new LambdaQueryWrapper<QuestionMaterialLink>()
                .eq(QuestionMaterialLink::getUserId, userId)
                .eq(QuestionMaterialLink::getQuestionId, questionId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearByMaterialId(Long userId, Long materialId) {
        linkMapper.delete(new LambdaQueryWrapper<QuestionMaterialLink>()
                .eq(QuestionMaterialLink::getUserId, userId)
                .eq(QuestionMaterialLink::getMaterialId, materialId));
    }
}
