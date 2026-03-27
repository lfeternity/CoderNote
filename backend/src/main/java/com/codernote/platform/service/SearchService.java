package com.codernote.platform.service;

import com.codernote.platform.dto.search.SearchResultVO;

public interface SearchService {

    SearchResultVO search(Long userId, String keyword, Long pageNo, Long pageSize);
}
