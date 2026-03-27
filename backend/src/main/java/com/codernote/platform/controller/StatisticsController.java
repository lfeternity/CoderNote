package com.codernote.platform.controller;

import com.codernote.platform.common.ApiResponse;
import com.codernote.platform.dto.statistics.StatisticsOverviewVO;
import com.codernote.platform.security.AuthContext;
import com.codernote.platform.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public ApiResponse<StatisticsOverviewVO> overview(HttpServletRequest servletRequest) {
        Long userId = AuthContext.getRequiredUserId(servletRequest);
        return ApiResponse.success(statisticsService.overview(userId));
    }
}
