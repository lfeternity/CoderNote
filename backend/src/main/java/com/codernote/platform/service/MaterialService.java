package com.codernote.platform.service;

import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.material.MaterialDetailVO;
import com.codernote.platform.dto.material.MaterialListItemVO;
import com.codernote.platform.dto.material.MaterialSaveRequest;

import java.util.List;

public interface MaterialService {

    Long create(Long userId, MaterialSaveRequest request);

    void update(Long userId, Long materialId, MaterialSaveRequest request);

    void delete(Long userId, Long materialId);

    void batchDelete(Long userId, List<Long> materialIds);

    PageResult<MaterialListItemVO> page(Long userId,
                                        Long pageNo,
                                        Long pageSize,
                                        String materialType,
                                        String language,
                                        String tag,
                                        String keyword);

    MaterialDetailVO detail(Long userId, Long materialId);

    List<MaterialListItemVO> listByTagId(Long userId, Long tagId);

    void favorite(Long userId, Long materialId);

    void unfavorite(Long userId, Long materialId);

    PageResult<MaterialListItemVO> favoritePage(Long userId,
                                                Long pageNo,
                                                Long pageSize,
                                                String materialType,
                                                String language,
                                                String tag,
                                                String keyword);
}