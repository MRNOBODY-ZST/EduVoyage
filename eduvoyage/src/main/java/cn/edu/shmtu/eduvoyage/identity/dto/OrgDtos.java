package cn.edu.shmtu.eduvoyage.identity.dto;

import cn.edu.shmtu.eduvoyage.identity.domain.OrgClass;
import cn.edu.shmtu.eduvoyage.identity.domain.OrgDepartment;
import cn.edu.shmtu.eduvoyage.identity.domain.OrgMajor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Organization DTOs (department → major → class) grouped in one file since each
 * is a small CRUD shape. Responses map straight from the entities.
 */
public final class OrgDtos {

    private OrgDtos() {
    }

    // ---------- department ----------
    @Schema(description = "院系")
    public record DepartmentResponse(Long id, String name, String code) {
        public static DepartmentResponse from(OrgDepartment d) {
            return new DepartmentResponse(d.getId(), d.getName(), d.getCode());
        }
    }

    @Schema(description = "创建/更新院系")
    public record DepartmentRequest(
            @NotBlank(message = "名称不能为空") @Size(max = 128) String name,
            @Size(max = 64) String code
    ) {
    }

    // ---------- major ----------
    @Schema(description = "专业")
    public record MajorResponse(Long id, Long departmentId, String name, String code) {
        public static MajorResponse from(OrgMajor m) {
            return new MajorResponse(m.getId(), m.getDepartmentId(), m.getName(), m.getCode());
        }
    }

    @Schema(description = "创建/更新专业")
    public record MajorRequest(
            @NotNull(message = "所属院系不能为空") Long departmentId,
            @NotBlank(message = "名称不能为空") @Size(max = 128) String name,
            @Size(max = 64) String code
    ) {
    }

    // ---------- class ----------
    @Schema(description = "班级")
    public record ClassResponse(Long id, Long majorId, String name, Integer grade) {
        public static ClassResponse from(OrgClass c) {
            return new ClassResponse(c.getId(), c.getMajorId(), c.getName(), c.getGrade());
        }
    }

    @Schema(description = "创建/更新班级")
    public record ClassRequest(
            @NotNull(message = "所属专业不能为空") Long majorId,
            @NotBlank(message = "名称不能为空") @Size(max = 128) String name,
            Integer grade
    ) {
    }
}
