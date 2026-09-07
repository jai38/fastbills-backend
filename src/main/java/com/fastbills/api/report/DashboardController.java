package com.fastbills.api.report;

import com.fastbills.api.auth.AuthenticatedUser;
import com.fastbills.api.report.dto.DashboardMetricsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Executive metrics, ageing breakdown, and unpaid invoice tracking")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Dashboard Metrics", description = "Aggregated KPI cards, receivables ageing visualizer, and top 10 oldest unpaid invoices")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics(@AuthenticationPrincipal AuthenticatedUser user) {
        DashboardMetricsResponse response = dashboardService.getDashboardMetrics(user.organisationId());
        return ResponseEntity.ok(response);
    }
}
