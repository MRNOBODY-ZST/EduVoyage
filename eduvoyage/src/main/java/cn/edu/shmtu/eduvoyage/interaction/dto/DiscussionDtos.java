package cn.edu.shmtu.eduvoyage.interaction.dto;

import cn.edu.shmtu.eduvoyage.interaction.domain.Discussion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class DiscussionDtos {

    private DiscussionDtos() {
    }

    @Schema(description = "发帖请求")
    public record PostRequest(
            @Schema(description = "关联知识点，可空")
            Long nodeId,

            @Schema(description = "标题")
            @NotBlank(message = "标题不能为空")
            @Size(max = 200, message = "标题不能超过200个字符")
            String title,

            @Schema(description = "正文")
            @NotBlank(message = "内容不能为空")
            @Size(max = 8000, message = "内容不能超过8000个字符")
            String content
    ) {
    }

    @Schema(description = "回复请求")
    public record ReplyRequest(
            @Schema(description = "正文")
            @NotBlank(message = "内容不能为空")
            @Size(max = 8000, message = "内容不能超过8000个字符")
            String content
    ) {
    }

    @Schema(description = "讨论响应")
    public record DiscussionResponse(
            String id,
            Long courseId,
            Long nodeId,
            Long authorId,
            String title,
            String content,
            String parentId,
            int likeCount,
            boolean liked,
            int replyCount,
            Instant ts
    ) {
        public static DiscussionResponse from(Discussion discussion, Long viewerId) {
            boolean liked = viewerId != null
                    && discussion.getLikes() != null
                    && discussion.getLikes().contains(viewerId);
            int likeCount = discussion.getLikes() == null ? 0 : discussion.getLikes().size();
            return new DiscussionResponse(
                    discussion.getId(),
                    discussion.getCourseId(),
                    discussion.getNodeId(),
                    discussion.getAuthorId(),
                    discussion.getTitle(),
                    discussion.getContent(),
                    discussion.getParentId(),
                    likeCount,
                    liked,
                    discussion.getReplyCount(),
                    discussion.getTs());
        }
    }
}
