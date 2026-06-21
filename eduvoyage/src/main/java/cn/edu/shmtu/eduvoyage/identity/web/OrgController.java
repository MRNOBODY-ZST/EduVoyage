package cn.edu.shmtu.eduvoyage.identity.web;

import cn.edu.shmtu.eduvoyage.identity.dto.OrgDtos.ClassRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.OrgDtos.ClassResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.OrgDtos.DepartmentRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.OrgDtos.DepartmentResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.OrgDtos.MajorRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.OrgDtos.MajorResponse;
import cn.edu.shmtu.eduvoyage.identity.service.OrgService;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Organization management (院系 / 专业 / 班级). Reads are available to any
 * authenticated user (needed for dropdowns when enrolling/assigning); writes
 * require the {@code org:manage} permission.
 */
@Tag(name = "组织管理", description = "院系、专业、班级管理")
@RestController
@RequestMapping("/api/org")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    // ---------------------------------------------------------- department

    @Operation(summary = "院系列表")
    @GetMapping("/departments")
    public Mono<Result<List<DepartmentResponse>>> listDepartments() {
        return orgService.listDepartments().collectList().map(Result::success);
    }

    @Operation(summary = "创建院系")
    @PreAuthorize("hasAuthority('org:manage')")
    @PostMapping("/departments")
    public Mono<Result<DepartmentResponse>> createDepartment(@Valid @RequestBody DepartmentRequest req) {
        return orgService.createDepartment(req).map(Result::success);
    }

    @Operation(summary = "更新院系")
    @PreAuthorize("hasAuthority('org:manage')")
    @PutMapping("/departments/{id}")
    public Mono<Result<DepartmentResponse>> updateDepartment(@PathVariable Long id,
                                                             @Valid @RequestBody DepartmentRequest req) {
        return orgService.updateDepartment(id, req).map(Result::success);
    }

    @Operation(summary = "删除院系")
    @PreAuthorize("hasAuthority('org:manage')")
    @DeleteMapping("/departments/{id}")
    public Mono<Result<Void>> deleteDepartment(@PathVariable Long id) {
        return orgService.deleteDepartment(id).thenReturn(Result.<Void>success());
    }

    // --------------------------------------------------------------- major

    @Operation(summary = "专业列表（可按院系过滤）")
    @GetMapping("/majors")
    public Mono<Result<List<MajorResponse>>> listMajors(@RequestParam(required = false) Long departmentId) {
        return orgService.listMajors(departmentId).collectList().map(Result::success);
    }

    @Operation(summary = "创建专业")
    @PreAuthorize("hasAuthority('org:manage')")
    @PostMapping("/majors")
    public Mono<Result<MajorResponse>> createMajor(@Valid @RequestBody MajorRequest req) {
        return orgService.createMajor(req).map(Result::success);
    }

    @Operation(summary = "更新专业")
    @PreAuthorize("hasAuthority('org:manage')")
    @PutMapping("/majors/{id}")
    public Mono<Result<MajorResponse>> updateMajor(@PathVariable Long id,
                                                   @Valid @RequestBody MajorRequest req) {
        return orgService.updateMajor(id, req).map(Result::success);
    }

    @Operation(summary = "删除专业")
    @PreAuthorize("hasAuthority('org:manage')")
    @DeleteMapping("/majors/{id}")
    public Mono<Result<Void>> deleteMajor(@PathVariable Long id) {
        return orgService.deleteMajor(id).thenReturn(Result.<Void>success());
    }

    // --------------------------------------------------------------- class

    @Operation(summary = "班级列表（可按专业过滤）")
    @GetMapping("/classes")
    public Mono<Result<List<ClassResponse>>> listClasses(@RequestParam(required = false) Long majorId) {
        return orgService.listClasses(majorId).collectList().map(Result::success);
    }

    @Operation(summary = "创建班级")
    @PreAuthorize("hasAuthority('org:manage')")
    @PostMapping("/classes")
    public Mono<Result<ClassResponse>> createClass(@Valid @RequestBody ClassRequest req) {
        return orgService.createClass(req).map(Result::success);
    }

    @Operation(summary = "更新班级")
    @PreAuthorize("hasAuthority('org:manage')")
    @PutMapping("/classes/{id}")
    public Mono<Result<ClassResponse>> updateClass(@PathVariable Long id,
                                                   @Valid @RequestBody ClassRequest req) {
        return orgService.updateClass(id, req).map(Result::success);
    }

    @Operation(summary = "删除班级")
    @PreAuthorize("hasAuthority('org:manage')")
    @DeleteMapping("/classes/{id}")
    public Mono<Result<Void>> deleteClass(@PathVariable Long id) {
        return orgService.deleteClass(id).thenReturn(Result.<Void>success());
    }
}
