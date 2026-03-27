package com.codernote.platform.service;

import java.util.List;

public interface QuestionExportService {

    byte[] exportPdf(Long userId,
                     String language,
                     String tag,
                     String masteryStatus,
                     String keyword);

    byte[] exportPdfByQuestionId(Long userId, Long questionId);

    byte[] exportPdfByQuestionIds(Long userId, List<Long> questionIds);
}
