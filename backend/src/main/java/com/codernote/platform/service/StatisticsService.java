package com.codernote.platform.service;

import com.codernote.platform.dto.statistics.StatisticsOverviewVO;

public interface StatisticsService {

    StatisticsOverviewVO overview(Long userId);
}
