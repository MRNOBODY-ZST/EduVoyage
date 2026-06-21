package cn.edu.shmtu.eduvoyage.assessment.web;

import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.QuestionRequest;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.QuestionResponse;
import cn.edu.shmtu.eduvoyage.assessment.service.QuestionService;
import cn.edu.shmtu.eduvoyage.shared.api.PageResult;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Question-bank management. Browsing/detail requires {@code homework:create};
 * authoring requires {@code homework:create} plus the service-level course
 * ownership check (or ADMIN for the global bank). Reference answers are never
 * exposed through the student-facing exam paper — only here, to authors.
 */
@Tag(name = "题库管理", description = "题目的检索与维护")
@RestController
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Operation(summary = "题目分页检索")
    @PreAuthorize("hasAuthority('homework:create')")
    @GetMapping("/api/questions")
    public Mono<Result<PageResult<QuestionResponse>>> page(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Long nodeId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return questionService.page(courseId, keyword, type, difficulty, nodeId, pageNo, pageSize)
                .map(Result::success);
    }

    @Operation(summary = "题目详情")
    @PreAuthorize("hasAuthority('homework:create')")
    @GetMapping("/api/questions/{id}")
    public Mono<Result<QuestionResponse>> get(@PathVariable Long id) {
        return questionService.get(id).map(Result::success);
    }

    @Operation(summary = "新增题目")
    @PreAuthorize("hasAuthority('homework:create')")
    @PostMapping("/api/questions")
    public Mono<Result<QuestionResponse>> create(@Valid @RequestBody QuestionRequest req,
                                                 @AuthenticationPrincipal AuthUser user) {
        return questionService.create(req, user).map(Result::success);
    }

    @Operation(summary = "更新题目")
    @PreAuthorize("hasAuthority('homework:create')")
    @PutMapping("/api/questions/{id}")
    public Mono<Result<QuestionResponse>> update(@PathVariable Long id,
                                                 @Valid @RequestBody QuestionRequest req,
                                                 @AuthenticationPrincipal AuthUser user) {
        return questionService.update(id, req, user).map(Result::success);
    }

    @Operation(summary = "删除题目")
    @PreAuthorize("hasAuthority('homework:create')")
    @DeleteMapping("/api/questions/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id,
                                     @AuthenticationPrincipal AuthUser user) {
        return questionService.delete(id, user).thenReturn(Result.<Void>success());
    }
}
