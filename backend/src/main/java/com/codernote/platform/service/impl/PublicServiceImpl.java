package com.codernote.platform.service.impl;

import com.codernote.platform.dto.common.OptionVO;
import com.codernote.platform.service.PublicService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PublicServiceImpl implements PublicService {

    @Override
    public Map<String, Object> options() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("majors", Arrays.asList(
                new OptionVO("Computer Science", "Computer Science"),
                new OptionVO("Software Engineering", "Software Engineering"),
                new OptionVO("Big Data", "Big Data"),
                new OptionVO("AI", "AI"),
                new OptionVO("Network Engineering", "Network Engineering")
        ));
        map.put("languages", Arrays.asList(
                new OptionVO("Java", "Java"),
                new OptionVO("Python", "Python"),
                new OptionVO("FrontEnd", "FrontEnd"),
                new OptionVO("MySQL", "MySQL"),
                new OptionVO("C++", "C++")
        ));
        map.put("masteryStatuses", Arrays.asList(
                new OptionVO("Not Reviewed", "NOT_MASTERED"),
                new OptionVO("Reviewing", "REVIEWING"),
                new OptionVO("Mastered", "MASTERED")
        ));
        map.put("materialTypes", Arrays.asList(
                new OptionVO("\u77E5\u8BC6\u70B9\u7B14\u8BB0", "KNOWLEDGE_NOTE"),
                new OptionVO("\u89E3\u9898\u6559\u7A0B", "SOLUTION_TUTORIAL"),
                new OptionVO("\u89C6\u9891\u94FE\u63A5", "VIDEO_LINK"),
                new OptionVO("\u4EE3\u7801\u6A21\u677F", "CODE_TEMPLATE"),
                new OptionVO("\u6587\u6863\u94FE\u63A5", "DOC_LINK")
        ));
        map.put("questionSources", List.of("LeetCode", "Homework", "Final Exam", "Interview", "Other"));
        map.put("materialSources", List.of("BiliBili", "CSDN", "Self", "Official", "Other"));
        return map;
    }
}
