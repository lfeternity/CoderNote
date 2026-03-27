package com.codernote.platform.service;

import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.note.NoteDetailVO;
import com.codernote.platform.dto.note.NoteListItemVO;
import com.codernote.platform.dto.note.NoteSaveRequest;
import com.codernote.platform.dto.note.NoteVersionDetailVO;
import com.codernote.platform.dto.note.NoteVersionListItemVO;

import java.util.List;

public interface NoteService {

    Long create(Long userId, NoteSaveRequest request);

    void update(Long userId, Long noteId, NoteSaveRequest request);

    void delete(Long userId, Long noteId);

    void batchDelete(Long userId, List<Long> noteIds);

    PageResult<NoteListItemVO> page(Long userId,
                                    Long pageNo,
                                    Long pageSize,
                                    String language,
                                    String tag,
                                    String favoriteStatus,
                                    String keyword,
                                    String sortBy,
                                    String sortOrder);

    NoteDetailVO detail(Long userId, Long noteId);

    List<NoteListItemVO> listByTagId(Long userId, Long tagId);

    void favorite(Long userId, Long noteId);

    void unfavorite(Long userId, Long noteId);

    void linkQuestion(Long userId, Long questionId, List<Long> noteIds);

    void linkMaterial(Long userId, Long materialId, List<Long> noteIds);

    void updateMasteryStatus(Long userId, Long noteId, String masteryStatus);

    List<NoteVersionListItemVO> listVersions(Long userId, Long noteId);

    NoteVersionDetailVO versionDetail(Long userId, Long noteId, Long versionId);

    void restoreVersion(Long userId, Long noteId, Long versionId);

    void deleteVersion(Long userId, Long noteId, Long versionId);
}
