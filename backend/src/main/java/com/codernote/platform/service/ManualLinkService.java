package com.codernote.platform.service;

import java.util.List;

public interface ManualLinkService {

    void replaceQuestionLinks(Long userId, Long questionId, List<Long> materialIds);

    void replaceMaterialLinks(Long userId, Long materialId, List<Long> questionIds);

    List<Long> listMaterialIdsByQuestionId(Long userId, Long questionId);

    List<Long> listQuestionIdsByMaterialId(Long userId, Long materialId);

    void clearByQuestionId(Long userId, Long questionId);

    void clearByMaterialId(Long userId, Long materialId);
}
