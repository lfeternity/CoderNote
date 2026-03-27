package com.codernote.platform.service.impl;

import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.material.MaterialListItemVO;
import com.codernote.platform.dto.note.NoteListItemVO;
import com.codernote.platform.dto.question.QuestionListItemVO;
import com.codernote.platform.dto.search.SearchResultVO;
import com.codernote.platform.service.MaterialService;
import com.codernote.platform.service.NoteService;
import com.codernote.platform.service.QuestionService;
import com.codernote.platform.service.SearchService;
import org.springframework.stereotype.Service;

@Service
public class SearchServiceImpl implements SearchService {

    private final QuestionService questionService;
    private final MaterialService materialService;
    private final NoteService noteService;

    public SearchServiceImpl(QuestionService questionService,
                             MaterialService materialService,
                             NoteService noteService) {
        this.questionService = questionService;
        this.materialService = materialService;
        this.noteService = noteService;
    }

    @Override
    public SearchResultVO search(Long userId, String keyword, Long pageNo, Long pageSize) {
        SearchResultVO vo = new SearchResultVO();
        vo.setKeyword(keyword);
        PageResult<QuestionListItemVO> questionPage = questionService.page(userId, pageNo, pageSize,
                null, null, null, keyword);
        PageResult<MaterialListItemVO> materialPage = materialService.page(userId, pageNo, pageSize,
                null, null, null, keyword);
        PageResult<NoteListItemVO> notePage = noteService.page(userId, pageNo, pageSize,
                null, null, null, keyword, null, null);
        vo.setQuestionPage(questionPage);
        vo.setMaterialPage(materialPage);
        vo.setNotePage(notePage);
        return vo;
    }
}
