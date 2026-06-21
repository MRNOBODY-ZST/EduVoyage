package cn.edu.shmtu.eduvoyage.course.web;

import cn.edu.shmtu.eduvoyage.course.dto.ChapterDtos.ChapterNode;
import cn.edu.shmtu.eduvoyage.course.dto.ChapterDtos.ChapterRequest;
import cn.edu.shmtu.eduvoyage.course.service.ChapterService;
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
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Course chapter tree. Listing the tree is open to any authenticated user;
 * mutations require {@code course:update} plus the per-course ownership check in
 * the service. Chapters are created/listed under a course, addressed by id for
 * update/delete.
 */
@Tag(name = "章节管理", description = "课程章节树的查看与维护")
@RestController
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @Operation(summary = "课程章节树")
    @GetMapping("/api/courses/{courseId}/chapters")
    public Mono<Result<List<ChapterNode>>> tree(@PathVariable Long courseId) {
        return chapterService.tree(courseId).map(Result::success);
    }

    @Operation(summary = "新增章节")
    @PreAuthorize("hasAuthority('course:update')")
    @PostMapping("/api/courses/{courseId}/chapters")
    public Mono<Result<ChapterNode>> create(@PathVariable Long courseId,
                                            @Valid @RequestBody ChapterRequest req,
                                            @AuthenticationPrincipal AuthUser user) {
        return chapterService.create(courseId, req, user).map(Result::success);
    }

    @Operation(summary = "更新章节")
    @PreAuthorize("hasAuthority('course:update')")
    @PutMapping("/api/chapters/{id}")
    public Mono<Result<ChapterNode>> update(@PathVariable Long id,
                                            @Valid @RequestBody ChapterRequest req,
                                            @AuthenticationPrincipal AuthUser user) {
        return chapterService.update(id, req, user).map(Result::success);
    }

    @Operation(summary = "删除章节")
    @PreAuthorize("hasAuthority('course:update')")
    @DeleteMapping("/api/chapters/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id,
                                     @AuthenticationPrincipal AuthUser user) {
        return chapterService.delete(id, user).thenReturn(Result.<Void>success());
    }
}
