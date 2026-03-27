package com.codernote.platform.service;

import java.util.List;

public interface NoteExportService {

    byte[] exportPdf(Long userId,
                     String language,
                     String tag,
                     String favoriteStatus,
                     String keyword,
                     String sortBy,
                     String sortOrder);

    byte[] exportPdfByNoteId(Long userId, Long noteId);

    byte[] exportPdfByNoteIds(Long userId, List<Long> noteIds);
}