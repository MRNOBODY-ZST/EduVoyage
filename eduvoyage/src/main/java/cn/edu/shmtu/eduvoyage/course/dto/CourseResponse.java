package cn.edu.shmtu.eduvoyage.course.dto;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbound course view. {@code classScope} and {@code favorite}/{@code enrolled}
 * flags are populated only on detail lookups (they need extra queries); list
 * views leave them null/empty to stay cheap.
 */
@Schema(description = "课程信息")
public record CourseResponse(
        @Schema(description = "课程 id") Long id,
        @Schema(description = "标题") String title,
        @Schema(description = "封面 URL") String coverUrl,
        @Schema(description = "简介") String intro,
        @Schema(description = "学分") BigDecimal credit,
        @Schema(description = "授课教师 id") Long teacherId,
        @Schema(description = "可见性 0私有1公开") Integer visibility,
        @Schema(description = "状态 0草稿1已发布2归档") Integer status,
        @Schema(description = "开课日期") LocalDate startDate,
        @Schema(description = "结课日期") LocalDate endDate,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "可见班级 id（详情）") List<Long> classScope,
        @Schema(description = "当前学生是否已选课（详情，学生视角）") Boolean enrolled,
        @Schema(description = "当前学生是否已收藏（详情，学生视角）") Boolean favorite
) {

    public static CourseResponse from(Course c) {
        return from(c, null, null, null);
    }

    public static CourseResponse from(Course c, List<Long> classScope, Boolean enrolled, Boolean favorite) {
        return new CourseResponse(
                c.getId(), c.getTitle(), c.getCoverUrl(), c.getIntro(), c.getCredit(),
                c.getTeacherId(), c.getVisibility(), c.getStatus(),
                c.getStartDate(), c.getEndDate(), c.getCreatedAt(),
                classScope, enrolled, favorite);
    }
}
