package cn.edu.shmtu.eduvoyage.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Create/update payload for a course. The owning teacher is taken from the
 * authenticated principal (create) or left unchanged (update) — never from the
 * client. {@code status} transitions go through dedicated publish/archive
 * endpoints, so it is not settable here.
 */
@Schema(description = "课程创建/更新请求")
public record CourseRequest(
        @Schema(description = "课程标题", example = "数据结构与算法")
        @NotBlank(message = "课程标题不能为空")
        @Size(max = 200, message = "标题过长")
        String title,

        @Schema(description = "封面 URL")
        @Size(max = 512, message = "封面 URL 过长")
        String coverUrl,

        @Schema(description = "课程简介")
        String intro,

        @Schema(description = "学分", example = "3.0")
        @DecimalMin(value = "0.0", message = "学分不能为负")
        BigDecimal credit,

        @Schema(description = "可见性 0私有 1公开")
        Integer visibility,

        @Schema(description = "开课日期")
        LocalDate startDate,

        @Schema(description = "结课日期")
        LocalDate endDate,

        @Schema(description = "可见班级 id 列表（私有课程的授课范围）")
        List<Long> classScope
) {
}
