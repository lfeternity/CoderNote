package com.codernote.platform.controller;

import com.codernote.platform.common.ApiResponse;
import com.codernote.platform.dto.ai.AiChatRequest;
import com.codernote.platform.dto.ai.AiChatVO;
import com.codernote.platform.dto.ai.AiModelCatalogVO;
import com.codernote.platform.dto.ai.AiQuestionAnalysisRequest;
import com.codernote.platform.dto.ai.AiQuestionAnalysisVO;
import com.codernote.platform.dto.ai.AiSummaryRequest;
import com.codernote.platform.dto.ai.AiSummaryVO;
import com.codernote.platform.security.AuthContext;
import com.codernote.platform.service.AiAssistantService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiAssistantService aiAssistantService;

    public AiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @GetMapping("/models")
    public ApiResponse<AiModelCatalogVO> models() {
        return ApiResponse.success(aiAssistantService.models());
    }

    @PostMapping("/question-analysis")
    public ApiResponse<AiQuestionAnalysisVO> questionAnalysis(@RequestBody AiQuestionAnalysisRequest request,
                                                              HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(aiAssistantService.analyzeQuestion(userId, request));
    }

    @PostMapping("/summary")
    public ApiResponse<AiSummaryVO> summary(@RequestBody AiSummaryRequest request,
                                            HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(aiAssistantService.summarize(userId, request));
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatVO> chat(@Valid @RequestBody AiChatRequest request,
                                      HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(aiAssistantService.chat(userId, request));
    }
}
