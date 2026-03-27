package com.codernote.platform.controller;

import com.codernote.platform.common.ApiResponse;
import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.common.IdsRequest;
import com.codernote.platform.dto.question.MasteryStatusUpdateRequest;
import com.codernote.platform.dto.question.QuestionDetailVO;
import com.codernote.platform.dto.question.QuestionListItemVO;
import com.codernote.platform.dto.question.QuestionSaveRequest;
import com.codernote.platform.security.AuthContext;
import com.codernote.platform.service.QuestionExportService;
import com.codernote.platform.service.QuestionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Validated
@RestController
@RequestMapping("/api/v1/error-question")
public class QuestionController {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final QuestionService questionService;
    private final QuestionExportService questionExportService;

    public QuestionController(QuestionService questionService,
                              QuestionExportService questionExportService) {
        this.questionService = questionService;
        this.questionExportService = questionExportService;
    }

    @PostMapping("/add")
    public ApiResponse<Long> add(@Valid @RequestBody QuestionSaveRequest request,
                                 HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success("Question created", questionService.create(userId, request));
    }

    @PutMapping("/update/{questionId}")
    public ApiResponse<Void> update(@PathVariable Long questionId,
                                    @Valid @RequestBody QuestionSaveRequest request,
                                    HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        questionService.update(userId, questionId, request);
        return ApiResponse.success("Question updated", null);
    }

    @DeleteMapping("/delete/{questionId}")
    public ApiResponse<Void> delete(@PathVariable Long questionId,
                                    HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        questionService.delete(userId, questionId);
        return ApiResponse.success("Deleted", null);
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody IdsRequest request,
                                         HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        questionService.batchDelete(userId, request.getIds());
        return ApiResponse.success("Batch deleted", null);
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<QuestionListItemVO>> list(@RequestParam(defaultValue = "1") Long pageNo,
                                                             @RequestParam(defaultValue = "10") Long pageSize,
                                                             @RequestParam(required = false) String language,
                                                             @RequestParam(required = false) String tag,
                                                             @RequestParam(required = false) String masteryStatus,
                                                             @RequestParam(required = false) String keyword,
                                                             HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(questionService.page(userId, pageNo, pageSize, language, tag, masteryStatus, keyword));
    }

    @GetMapping("/detail/{questionId}")
    public ApiResponse<QuestionDetailVO> detail(@PathVariable Long questionId,
                                                HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(questionService.detail(userId, questionId));
    }

    @PutMapping("/mastery-status/{questionId}")
    public ApiResponse<Void> masteryStatus(@PathVariable Long questionId,
                                           @Valid @RequestBody MasteryStatusUpdateRequest request,
                                           HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        questionService.updateMasteryStatus(userId, questionId, request.getMasteryStatus());
        return ApiResponse.success("Mastery updated", null);
    }

    @GetMapping("/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam(required = false) String language,
                                            @RequestParam(required = false) String tag,
                                            @RequestParam(required = false) String masteryStatus,
                                            @RequestParam(required = false) String keyword,
                                            HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        byte[] pdfBytes = questionExportService.exportPdf(userId, language, tag, masteryStatus, keyword);
        return buildPdfResponse(pdfBytes, "error_questions_filtered_");
    }

    @GetMapping("/export-pdf/{questionId}")
    public ResponseEntity<byte[]> exportSinglePdf(@PathVariable Long questionId,
                                                  HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        byte[] pdfBytes = questionExportService.exportPdfByQuestionId(userId, questionId);
        return buildPdfResponse(pdfBytes, "error_question_");
    }

    @PostMapping("/export-pdf/batch")
    public ResponseEntity<byte[]> exportBatchPdf(@Valid @RequestBody IdsRequest request,
                                                 HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        byte[] pdfBytes = questionExportService.exportPdfByQuestionIds(userId, request.getIds());
        return buildPdfResponse(pdfBytes, "error_questions_selected_");
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdfBytes, String filePrefix) {
        String fileName = filePrefix + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".pdf";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(pdfBytes);
    }
}
