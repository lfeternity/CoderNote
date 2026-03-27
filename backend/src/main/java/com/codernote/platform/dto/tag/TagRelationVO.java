package com.codernote.platform.dto.tag;

import com.codernote.platform.dto.material.MaterialListItemVO;
import com.codernote.platform.dto.note.NoteListItemVO;
import com.codernote.platform.dto.question.QuestionListItemVO;
import lombok.Data;

import java.util.List;

@Data
public class TagRelationVO {
    private Long tagId;
    private String tagName;
    private Long usageCount;
    private List<QuestionListItemVO> relatedQuestions;
    private List<MaterialListItemVO> relatedMaterials;
    private List<NoteListItemVO> relatedNotes;
}
