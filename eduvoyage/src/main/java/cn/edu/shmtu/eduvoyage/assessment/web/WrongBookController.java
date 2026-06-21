package cn.edu.shmtu.eduvoyage.assessment.web;

import cn.edu.shmtu.eduvoyage.assessment.dto.WrongBookDtos.WrongBookEntry;
import cn.edu.shmtu.eduvoyage.assessment.service.WrongBookService;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * A student's wrong book. Entries are accumulated automatically during grading;
 * this read-only API lets the student review them, optionally filtered to the
 * not-yet-mastered ones. Always scoped to the authenticated student.
 */
@Tag(name = "错题本", description = "学生错题查看")
@RestController
public class WrongBookController {

    private final WrongBookService wrongBookService;

    public WrongBookController(WrongBookService wrongBookService) {
        this.wrongBookService = wrongBookService;
    }

    @Operation(summary = "我的错题本")
    @PreAuthorize("hasAuthority('homework:submit')")
    @GetMapping("/api/wrong-book/me")
    public Mono<Result<List<WrongBookEntry>>> mine(
            @RequestParam(defaultValue = "false") boolean onlyUnmastered,
            @AuthenticationPrincipal AuthUser user) {
        return wrongBookService.list(user.id(), onlyUnmastered).map(Result::success);
    }
}
