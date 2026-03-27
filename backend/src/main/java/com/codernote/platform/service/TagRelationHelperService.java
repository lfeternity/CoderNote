package com.codernote.platform.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TagRelationHelperService {

    List<Long> resolveTagIds(List<String> tagNames, Long userId);

    void replaceQuestionTags(Long questionId, List<Long> tagIds);

    void replaceMaterialTags(Long materialId, List<Long> tagIds);

    void replaceNoteTags(Long noteId, List<Long> tagIds);

    List<String> listTagNamesByQuestionId(Long questionId);

    List<String> listTagNamesByMaterialId(Long materialId);

    List<String> listTagNamesByNoteId(Long noteId);

    Map<Long, List<String>> questionTagNameMap(Collection<Long> questionIds);

    Map<Long, List<String>> materialTagNameMap(Collection<Long> materialIds);

    Map<Long, List<String>> noteTagNameMap(Collection<Long> noteIds);

    List<Long> listTagIdsByQuestionId(Long questionId);

    List<Long> listTagIdsByMaterialId(Long materialId);

    List<Long> listTagIdsByNoteId(Long noteId);

    Set<Long> findMaterialIdsByTagIds(List<Long> tagIds);

    Set<Long> findQuestionIdsByTagIds(List<Long> tagIds);

    Set<Long> findNoteIdsByTagIds(List<Long> tagIds);

    Long usageCount(Long tagId);

    boolean isUsed(Long tagId);
}
