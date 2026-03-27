package com.codernote.platform.controller;

import com.codernote.platform.common.ApiResponse;
import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.common.IdsRequest;
import com.codernote.platform.dto.note.NoteDetailVO;
import com.codernote.platform.dto.note.NoteListItemVO;
import com.codernote.platform.dto.note.NoteSaveRequest;
import com.codernote.platform.dto.note.NoteVersionDetailVO;
import com.codernote.platform.dto.note.NoteVersionListItemVO;
import com.codernote.platform.dto.question.MasteryStatusUpdateRequest;
import com.codernote.platform.security.AuthContext;
import com.codernote.platform.service.NoteExportService;
import com.codernote.platform.service.NoteService;
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
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/note")
public class NoteController {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final NoteService noteService;
    private final NoteExportService noteExportService;

    public NoteController(NoteService noteService,
                          NoteExportService noteExportService) {
        this.noteService = noteService;
        this.noteExportService = noteExportService;
    }

    @PostMapping("/add")
    public ApiResponse<Long> add(@Valid @RequestBody NoteSaveRequest request,
                                 HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success("Note created", noteService.create(userId, request));
    }

    @PutMapping("/update/{noteId}")
    public ApiResponse<Void> update(@PathVariable Long noteId,
                                    @Valid @RequestBody NoteSaveRequest request,
                                    HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        noteService.update(userId, noteId, request);
        return ApiResponse.success("Note updated", null);
    }

    @DeleteMapping("/delete/{noteId}")
    public ApiResponse<Void> delete(@PathVariable Long noteId,
                                    HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        noteService.delete(userId, noteId);
        return ApiResponse.success("Deleted", null);
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody IdsRequest request,
                                         HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        noteService.batchDelete(userId, request.getIds());
        return ApiResponse.success("Batch deleted", null);
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<NoteListItemVO>> list(@RequestParam(defaultValue = "1") Long pageNo,
                                                         @RequestParam(defaultValue = "10") Long pageSize,
                                                         @RequestParam(required = false) String language,
                                                         @RequestParam(required = false) String tag,
                                                         @RequestParam(required = false) String favoriteStatus,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String sortBy,
                                                         @RequestParam(required = false) String sortOrder,
                                                         HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(noteService.page(userId, pageNo, pageSize, language, tag, favoriteStatus, keyword, sortBy, sortOrder));
    }

    @GetMapping("/detail/{noteId}")
    public ApiResponse<NoteDetailVO> detail(@PathVariable Long noteId,
                                            HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(noteService.detail(userId, noteId));
    }

    @GetMapping("/version/list/{noteId}")
    public ApiResponse<List<NoteVersionListItemVO>> versionList(@PathVariable Long noteId,
                                                                HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(noteService.listVersions(userId, noteId));
    }

    @GetMapping("/version/detail/{noteId}/{versionId}")
    public ApiResponse<NoteVersionDetailVO> versionDetail(@PathVariable Long noteId,
                                                          @PathVariable Long versionId,
                                                          HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(noteService.versionDetail(userId, noteId, versionId));
    }

    @PostMapping("/version/restore/{noteId}/{versionId}")
    public ApiResponse<Void> restoreVersion(@PathVariable Long noteId,
                                            @PathVariable Long versionId,
                                            HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        noteService.restoreVersion(userId, noteId, versionId);
        return ApiResponse.success("Version restored", null);
    }

    @DeleteMapping("/version/delete/{noteId}/{versionId}")
    public ApiResponse<Void> deleteVersion(@PathVariable Long noteId,
                                           @PathVariable Long versionId,
                                           HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        noteService.deleteVersion(userId, noteId, versionId);
        return ApiResponse.success("Version deleted", null);
    }

    @PutMapping("/mastery-status/{noteId}")
    public ApiResponse<Void> masteryStatus(@PathVariable Long noteId,
                                           @Valid @RequestBody MasteryStatusUpdateRequest request,
                                           HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        noteService.updateMasteryStatus(userId, noteId, request.getMasteryStatus());
        return ApiResponse.success("Mastery updated", null);
    }

    @PostMapping("/favorite/{noteId}")
    public ApiResponse<Void> favorite(@PathVariable Long noteId,
                                      HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        noteService.favorite(userId, noteId);
        return ApiResponse.success("Favorited", null);
    }

    @DeleteMapping("/favorite/{noteId}")
    public ApiResponse<Void> unfavorite(@PathVariable Long noteId,
                                        HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        noteService.unfavorite(userId, noteId);
        return ApiResponse.success("Unfavorited", null);
    }

    @PostMapping("/link-question/{questionId}")
    public ApiResponse<Void> linkQuestion(@PathVariable Long questionId,
                                          @Valid @RequestBody IdsRequest request,
                                          HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        noteService.linkQuestion(userId, questionId, request.getIds());
        return ApiResponse.success("Linked", null);
    }

    @PostMapping("/link-material/{materialId}")
    public ApiResponse<Void> linkMaterial(@PathVariable Long materialId,
                                          @Valid @RequestBody IdsRequest request,
                                          HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        noteService.linkMaterial(userId, materialId, request.getIds());
        return ApiResponse.success("Linked", null);
    }

    @GetMapping("/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam(required = false) String language,
                                            @RequestParam(required = false) String tag,
                                            @RequestParam(required = false) String favoriteStatus,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String sortBy,
                                            @RequestParam(required = false) String sortOrder,
                                            HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        byte[] pdfBytes = noteExportService.exportPdf(userId, language, tag, favoriteStatus, keyword, sortBy, sortOrder);
        return buildPdfResponse(pdfBytes, "notes_filtered_");
    }

    @GetMapping("/export-pdf/{noteId}")
    public ResponseEntity<byte[]> exportSinglePdf(@PathVariable Long noteId,
                                                  HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        byte[] pdfBytes = noteExportService.exportPdfByNoteId(userId, noteId);
        return buildPdfResponse(pdfBytes, "note_");
    }

    @PostMapping("/export-pdf/batch")
    public ResponseEntity<byte[]> exportBatchPdf(@Valid @RequestBody IdsRequest request,
                                                 HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        byte[] pdfBytes = noteExportService.exportPdfByNoteIds(userId, request.getIds());
        return buildPdfResponse(pdfBytes, "notes_selected_");
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
