package cn.edu.shmtu.eduvoyage.course.dto;

import cn.edu.shmtu.eduvoyage.course.domain.CourseChapter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Chapter create/update request and the recursive tree node returned by the
 * chapter-tree endpoint. {@code parentId} of {@code 0} (or null on create) means
 * a top-level chapter.
 */
public final class ChapterDtos {

    private ChapterDtos() {
    }

    @Schema(description = "章节创建/更新请求")
    public record ChapterRequest(
            @Schema(description = "章节标题", example = "第一章 绪论")
            @NotBlank(message = "章节标题不能为空")
            @Size(max = 200, message = "标题过长")
            String title,

            @Schema(description = "父章节 id，顶级章节填 0 或留空")
            Long parentId,

            @Schema(description = "同级排序号，越小越靠前")
            Integer sortNo
    ) {
    }

    @Schema(description = "章节树节点")
    public record ChapterNode(
            @Schema(description = "章节 id") Long id,
            @Schema(description = "课程 id") Long courseId,
            @Schema(description = "父章节 id") Long parentId,
            @Schema(description = "标题") String title,
            @Schema(description = "排序号") Integer sortNo,
            @Schema(description = "子章节") List<ChapterNode> children
    ) {
        public static ChapterNode of(CourseChapter c) {
            return new ChapterNode(c.getId(), c.getCourseId(), c.getParentId(),
                    c.getTitle(), c.getSortNo(), new ArrayList<>());
        }
    }
}
