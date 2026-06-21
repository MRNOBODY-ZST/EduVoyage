package cn.edu.shmtu.eduvoyage.course.dto;

import cn.edu.shmtu.eduvoyage.course.domain.Courseware;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Courseware create/update request and outbound view. {@code type} selects the
 * payload semantics: video/document reference a drive {@code fileId}; rich-text
 * stores a MongoDB {@code contentRef}; link stores a URL in {@code contentRef}.
 */
public final class CoursewareDtos {

    private CoursewareDtos() {
    }

    @Schema(description = "课件创建/更新请求")
    public record CoursewareRequest(
            @Schema(description = "课件标题", example = "二叉树遍历讲解视频")
            @NotBlank(message = "课件标题不能为空")
            @Size(max = 200, message = "标题过长")
            String title,

            @Schema(description = "类型 1视频2文档3图文4链接")
            @NotNull(message = "课件类型不能为空")
            @Min(value = 1, message = "无效的课件类型")
            @Max(value = 4, message = "无效的课件类型")
            Integer type,

            @Schema(description = "内容引用（图文 MongoDB id 或链接 URL）")
            @Size(max = 64, message = "内容引用过长")
            String contentRef,

            @Schema(description = "网盘文件 id（视频/文档）")
            Long fileId,

            @Schema(description = "时长（秒，视频）")
            @PositiveOrZero(message = "时长不能为负")
            Integer durationSec,

            @Schema(description = "排序号")
            Integer sortNo
    ) {
    }

    @Schema(description = "课件信息")
    public record CoursewareResponse(
            @Schema(description = "课件 id") Long id,
            @Schema(description = "知识点 id") Long nodeId,
            @Schema(description = "标题") String title,
            @Schema(description = "类型") Integer type,
            @Schema(description = "内容引用") String contentRef,
            @Schema(description = "文件 id") Long fileId,
            @Schema(description = "时长（秒）") Integer durationSec,
            @Schema(description = "排序号") Integer sortNo,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {
        public static CoursewareResponse from(Courseware c) {
            return new CoursewareResponse(c.getId(), c.getNodeId(), c.getTitle(), c.getType(),
                    c.getContentRef(), c.getFileId(), c.getDurationSec(), c.getSortNo(), c.getCreatedAt());
        }
    }
}
