package com.codernote.platform.controller;

import com.codernote.platform.common.ApiResponse;
import com.codernote.platform.dto.search.SearchResultVO;
import com.codernote.platform.security.AuthContext;
import com.codernote.platform.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/result")
    public ApiResponse<SearchResultVO> search(@RequestParam String keyword,
                                              @RequestParam(defaultValue = "1") Long pageNo,
                                              @RequestParam(defaultValue = "10") Long pageSize,
                                              HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(searchService.search(userId, keyword, pageNo, pageSize));
    }
}
