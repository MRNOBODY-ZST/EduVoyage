package cn.edu.shmtu.eduvoyage.assessment.web;

import cn.edu.shmtu.eduvoyage.assessment.dto.GradingDtos.GradeRequest;
import cn.edu.shmtu.eduvoyage.assessment.dto.SubmissionDtos.ExamPaper;
import cn.edu.shmtu.eduvoyage.assessment.dto.SubmissionDtos.SubmissionResult;
import cn.edu.shmtu.eduvoyage.assessment.dto.SubmissionDtos.SubmitRequest;
import cn.edu.shmtu.eduvoyage.assessment.service.SubmissionService;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Submission flow. Students start an attempt, submit answers and read their own
 * results ({@code homework:submit}); teachers grade subjective items
 * ({@code homework:grade}). All student-facing routes act on the authenticated
 * principal and never expose another student's submission.
 */
@Tag(name = "答题与批改", description = "学生作答提交与教师批改")
@RestController
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @Operation(summary = "开始作答（获取试卷）")
    @PreAuthorize("hasAuthority('homework:submit')")
    @PostMapping("/api/homeworks/{homeworkId}/submissions/start")
    public Mono<Result<ExamPaper>> start(@PathVariable Long homeworkId,
                                         @AuthenticationPrincipal AuthUser user) {
        return submissionService.start(homeworkId, user.id()).map(Result::success);
    }

    @Operation(summary = "提交作答")
    @PreAuthorize("hasAuthority('homework:submit')")
    @PostMapping("/api/submissions/{submissionId}/submit")
    public Mono<Result<SubmissionResult>> submit(@PathVariable Long submissionId,
                                                 @Valid @RequestBody SubmitRequest req,
                                                 @AuthenticationPrincipal AuthUser user) {
        return submissionService.submit(submissionId, req, user.id()).map(Result::success);
    }

    @Operation(summary = "查看我的提交结果")
    @PreAuthorize("hasAuthority('homework:submit')")
    @GetMapping("/api/submissions/{submissionId}")
    public Mono<Result<SubmissionResult>> getResult(@PathVariable Long submissionId,
                                                    @AuthenticationPrincipal AuthUser user) {
        return submissionService.getResult(submissionId, user.id()).map(Result::success);
    }

    @Operation(summary = "我对某作业的提交记录")
    @PreAuthorize("hasAuthority('homework:submit')")
    @GetMapping("/api/homeworks/{homeworkId}/submissions/me")
    public Mono<Result<List<SubmissionResult>>> myAttempts(@PathVariable Long homeworkId,
                                                           @AuthenticationPrincipal AuthUser user) {
        return submissionService.myAttempts(homeworkId, user.id()).map(Result::success);
    }

    @Operation(summary = "教师批改提交")
    @PreAuthorize("hasAuthority('homework:grade')")
    @PostMapping("/api/submissions/{submissionId}/grade")
    public Mono<Result<SubmissionResult>> grade(@PathVariable Long submissionId,
                                                @Valid @RequestBody GradeRequest req,
                                                @AuthenticationPrincipal AuthUser user) {
        return submissionService.grade(submissionId, req, user).map(Result::success);
    }
}
