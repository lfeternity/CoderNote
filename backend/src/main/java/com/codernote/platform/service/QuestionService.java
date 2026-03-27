package com.codernote.platform.service;

import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.question.QuestionDetailVO;
import com.codernote.platform.dto.question.QuestionListItemVO;
import com.codernote.platform.dto.question.QuestionSaveRequest;

import java.util.List;

public interface QuestionService {

    Long create(Long userId, QuestionSaveRequest request);

    void update(Long userId, Long questionId, QuestionSaveRequest request);

    void delete(Long userId, Long questionId);

    void batchDelete(Long userId, List<Long> questionIds);

    PageResult<QuestionListItemVO> page(Long userId,
                                        Long pageNo,
                                        Long pageSize,
                                        String language,
                                        String tag,
                                        String masteryStatus,
                                        String keyword);

    QuestionDetailVO detail(Long userId, Long questionId);

    void updateMasteryStatus(Long userId, Long questionId, String masteryStatus);

    List<QuestionListItemVO> listByTagId(Long userId, Long tagId);
}
