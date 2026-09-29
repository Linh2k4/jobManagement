package com.company.jobmanagement.controller.evaluation;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.request.EvaluationScoresRequest;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.EvaluationResponse;
import com.company.jobmanagement.model.entity.Evaluation;
import com.company.jobmanagement.security.CurrentUser;
import com.company.jobmanagement.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
@Tag(name = "Evaluations", description = "Performance evaluation workflow (member → lead → manager)")
public class EvaluationController extends BaseController {

    private final EvaluationService evaluationService;
    private final CurrentUser currentUser;

    @PostMapping
    @Operation(summary = "Create evaluation for user")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<EvaluationResponse>> createEvaluation(
            @RequestParam Long userId,
            @RequestParam String periodMonth,
            @RequestParam(required = false) Long groupId) {
        YearMonth period = YearMonth.parse(periodMonth);
        Evaluation eval = evaluationService.getOrCreateEvaluation(userId, groupId, period);
        return created(ApiResponse.success("Evaluation created", EvaluationResponse.from(eval)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get evaluation details")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<EvaluationResponse>> getEvaluation(@PathVariable Long id) {
        Evaluation eval = evaluationService.getEvaluation(id);
        return ok(ApiResponse.success(EvaluationResponse.from(eval)));
    }

    @PutMapping("/{id}/self-review")
    @Operation(summary = "Submit member self-evaluation (1-10 scores)")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> submitSelfReview(
            @PathVariable Long id,
            @RequestBody EvaluationScoresRequest scores) {
        evaluationService.submitSelfEvaluation(
                id,
                scores.getQuality(),
                scores.getResponsibility(),
                scores.getTeamwork(),
                scores.getInitiative(),
                scores.getDiscipline(),
                scores.getNotes()
        );
        return ok(ApiResponse.success("Self-evaluation submitted successfully"));
    }

    @PutMapping("/{id}/lead-review")
    @PreAuthorize("hasRole('LEAD')")
    @Operation(summary = "Submit lead evaluation")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> submitLeadReview(
            @PathVariable Long id,
            @RequestBody EvaluationScoresRequest scores) {
        evaluationService.submitLeadEvaluation(
                id,
                scores.getQuality(),
                scores.getResponsibility(),
                scores.getTeamwork(),
                scores.getDiscipline(),
                scores.getNotes()
        );
        return ok(ApiResponse.success("Lead evaluation submitted successfully"));
    }

    @PutMapping("/{id}/finalize")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Finalize evaluation (Manager only)")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> finalizeEvaluation(
            @PathVariable Long id,
            @RequestBody EvaluationScoresRequest scores) {
        evaluationService.finalizeEvaluation(
                id,
                scores.getQuality(),
                scores.getResponsibility(),
                scores.getInitiative(),
                scores.getDiscipline(),
                scores.getNotes()
        );
        return ok(ApiResponse.success("Evaluation finalized successfully"));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my evaluations")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Page<EvaluationResponse>>> getMyEvaluations(
            @RequestParam(required = false) String periodMonth,
            Pageable pageable) {
        YearMonth period = periodMonth != null ? YearMonth.parse(periodMonth) : YearMonth.now();
        Page<Evaluation> evaluations = evaluationService.getMyEvaluations(period, pageable);
        return ok(ApiResponse.success(evaluations.map(EvaluationResponse::from)));
    }

    @GetMapping("/team")
    @PreAuthorize("hasRole('LEAD')")
    @Operation(summary = "Get team member evaluations (Lead only)")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Page<EvaluationResponse>>> getTeamEvaluations(
            @RequestParam String periodMonth,
            Pageable pageable) {
        YearMonth period = YearMonth.parse(periodMonth);
        Long leadId = currentUser.getCurrentUserId();
        Page<Evaluation> result = evaluationService.getTeamEvaluations(leadId, period, pageable);
        return ok(ApiResponse.success(result.map(EvaluationResponse::from)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get all evaluations for period (Manager only)")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Page<EvaluationResponse>>> getAllEvaluations(
            @RequestParam String periodMonth,
            Pageable pageable) {
        YearMonth period = YearMonth.parse(periodMonth);
        Page<Evaluation> evaluations = evaluationService.getAllEvaluations(period, pageable);
        return ok(ApiResponse.success(evaluations.map(EvaluationResponse::from)));
    }
}
