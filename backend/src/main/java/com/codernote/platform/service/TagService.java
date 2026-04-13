package com.codernote.platform.service;

import com.codernote.platform.dto.tag.TagListItemVO;
import com.codernote.platform.dto.tag.TagRelationVO;

import java.util.List;

public interface TagService {

    List<TagListItemVO> list(Long userId);

    Long create(Long userId, String name, String coverPath);

    void update(Long userId, Long tagId, String newName, String coverPath);

    void delete(Long userId, Long tagId);

    TagRelationVO relation(Long userId, Long tagId);
}
