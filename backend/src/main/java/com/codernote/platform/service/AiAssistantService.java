package com.codernote.platform.service;

import com.codernote.platform.dto.ai.AiChatRequest;
import com.codernote.platform.dto.ai.AiChatVO;
import com.codernote.platform.dto.ai.AiModelCatalogVO;
import com.codernote.platform.dto.ai.AiQuestionAnalysisRequest;
import com.codernote.platform.dto.ai.AiQuestionAnalysisVO;
import com.codernote.platform.dto.ai.AiSummaryRequest;
import com.codernote.platform.dto.ai.AiSummaryVO;

public interface AiAssistantService {

    AiModelCatalogVO models();

    AiQuestionAnalysisVO analyzeQuestion(Long userId, AiQuestionAnalysisRequest request);

    AiSummaryVO summarize(Long userId, AiSummaryRequest request);

    AiChatVO chat(Long userId, AiChatRequest request);
}
