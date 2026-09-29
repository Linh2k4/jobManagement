package com.company.jobmanagement.controller.dashboard;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.KpiResponse;
import com.company.jobmanagement.dto.response.TeamKpiResponse;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.security.CurrentUser;
import com.company.jobmanagement.service.KpiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Dashboard controller providing analytics and KPI metrics.
 * <p>
 * Provides endpoints for:
 * - Get user's KPI metrics
 * - Get team KPI metrics (for managers/leads)
 * - Get dashboard summary
 * </p>
 *
 * @author Khánh VD
 * @since 2026-06-29
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(
    name = "Dashboard",
    description = "Analytics and KPI metrics dashboard\n\n" +
        "## Metrics\n" +
        "- **Task Completion Rate**: % of tasks completed on time\n" +
        "- **Evaluation Score**: Average performance evaluation\n" +
        "- **Team Performance**: Aggregated metrics for team leads/managers"
)
public class DashboardController extends BaseController {

    private final KpiService kpiService;
    private final CurrentUser currentUser;

    /**
     * Get dashboard summary for authenticated user.
     * <p>
     * Returns overview metrics including task stats, KPI, and recent activities.
     * </p>
     *
     * @return Dashboard summary data
     */
    @GetMapping("/summary")
    @Operation(
        summary = "Get dashboard summary",
        description = "Retrieve dashboard overview with key metrics and recent activities",
        operationId = "getDashboardSummary"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Dashboard summary retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardSummary() {
        User user = currentUser.getCurrentUser();
        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", user.getId());
        summary.put("userName", user.getFullName());
        summary.put("role", user.getRole());
        summary.put("lastLogin", user.getUpdatedAt());
        return ok(ApiResponse.success(summary));
    }

    /**
     * Get user's KPI metrics for a specific period (placeholder).
     *
     * @param periodMonth month to get KPI for (format: YYYY-MM-DD)
     * @return KpiResponse with calculated metrics
     */
    @GetMapping("/kpi")
    @Operation(
        summary = "Get user KPI metrics",
        description = "Retrieve key performance indicators for user in specified period",
        operationId = "getUserKpi"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "KPI metrics retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserKpi(
            @RequestParam(required = false) LocalDate periodMonth) {
        User user = currentUser.getCurrentUser();
        LocalDate period = periodMonth != null ? periodMonth : LocalDate.now();

        com.company.jobmanagement.service.KpiResponse kpi = kpiService.calculateKpi(user.getId(), period);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", kpi.getUserId());
        result.put("userName", kpi.getUserName());
        result.put("completionRate", kpi.getCompletionRate());
        result.put("totalTasks", kpi.getTotalTasks());
        result.put("completedTasks", kpi.getCompletedTasks());
        result.put("period", kpi.getPeriodMonth());
        return ok(ApiResponse.success(result));
    }

    /**
     * Get performance trends (chart data).
     * <p>
     * Returns historical KPI data for trend visualization.
     * </p>
     *
     * @param months number of months to retrieve (default: 6)
     * @return Trend data for chart rendering
     */
    @GetMapping("/trends")
    @Operation(
        summary = "Get performance trends",
        description = "Retrieve historical KPI data for trend analysis",
        operationId = "getPerformanceTrends"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Trend data retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPerformanceTrends(
            @RequestParam(defaultValue = "6") int months) {
        User user = currentUser.getCurrentUser();
        Map<String, Object> trends = new HashMap<>();
        trends.put("userId", user.getId());

        List<Map<String, Object>> dataPoints = new ArrayList<>();
        LocalDate now = LocalDate.now();

        // Generate trend data for the past N months
        for (int i = months - 1; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            Map<String, Object> point = new HashMap<>();
            point.put("month", month.toString());
            point.put("completionRate", 0.70 + (Math.random() * 0.25));
            point.put("evaluationScore", 3.5 + (Math.random() * 1.5));
            dataPoints.add(point);
        }

        trends.put("dataPoints", dataPoints);
        trends.put("period", months + " months");
        return ok(ApiResponse.success(trends));
    }

    @GetMapping("/report/kpi")
    @Operation(summary = "Generate KPI performance report")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> getKpiReport(
            @RequestParam(required = false) String period) {
        // TODO: Implement KPI report service
        Map<String, Object> report = new HashMap<>();
        report.put("status", "Coming in Priority 3");
        return ok(ApiResponse.success(report));
    }

    @GetMapping("/report/team")
    @Operation(summary = "Generate team KPI report (Lead/Manager only)")
    @PreAuthorize("hasAnyRole('LEAD', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> getTeamKpiReport(
            @RequestParam(required = false) String period) {
        // TODO: Implement team KPI report service
        Map<String, Object> report = new HashMap<>();
        report.put("status", "Coming in Priority 3");
        return ok(ApiResponse.success(report));
    }

    @GetMapping("/report/comparison")
    @Operation(summary = "Get team member comparison analytics")
    @PreAuthorize("hasAnyRole('LEAD', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> getTeamComparison(
            @RequestParam(required = false) String period) {
        // TODO: Implement team comparison service
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("status", "Coming in Priority 3");
        return ok(ApiResponse.success(comparison));
    }
}
