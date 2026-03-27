package com.codernote.platform.dto.search;

import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.material.MaterialListItemVO;
import com.codernote.platform.dto.note.NoteListItemVO;
import com.codernote.platform.dto.question.QuestionListItemVO;
import lombok.Data;

@Data
public class SearchResultVO {
    private String keyword;
    private PageResult<QuestionListItemVO> questionPage;
    private PageResult<MaterialListItemVO> materialPage;
    private PageResult<NoteListItemVO> notePage;
}
