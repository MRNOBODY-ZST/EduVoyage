package cn.edu.shmtu.eduvoyage.interaction.dto;

import cn.edu.shmtu.eduvoyage.interaction.domain.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    @Schema(description = "课程公告请求")
    public record AnnouncementRequest(
            @Schema(description = "标题")
            @NotBlank(message = "标题不能为空")
            @Size(max = 200, message = "标题不能超过200个字符")
            String title,

            @Schema(description = "正文")
            @NotBlank(message = "内容不能为空")
            @Size(max = 4000, message = "内容不能超过4000个字符")
            String body
    ) {
    }

    @Schema(description = "管理员站内信请求")
    public record DirectMessageRequest(
            @Schema(description = "接收用户 id")
            @NotNull(message = "接收人不能为空")
            Long toUserId,

            @Schema(description = "标题")
            @NotBlank(message = "标题不能为空")
            @Size(max = 200, message = "标题不能超过200个字符")
            String title,

            @Schema(description = "正文")
            @NotBlank(message = "内容不能为空")
            @Size(max = 4000, message = "内容不能超过4000个字符")
            String body,

            @Schema(description = "业务引用")
            String refId,

            @Schema(description = "分类")
            String category
    ) {
    }

    @Schema(description = "通知响应")
    public record NotificationResponse(
            String id,
            Long toUserId,
            String type,
            String title,
            String body,
            String refId,
            String category,
            boolean read,
            Instant ts
    ) {
        public static NotificationResponse from(Notification notification) {
            return new NotificationResponse(
                    notification.getId(),
                    notification.getToUserId(),
                    notification.getType(),
                    notification.getTitle(),
                    notification.getBody(),
                    notification.getRefId(),
                    notification.getCategory(),
                    notification.isRead(),
                    notification.getTs());
        }
    }

    @Schema(description = "未读数量")
    public record UnreadCountResponse(long unread) {
    }

    @Schema(description = "批量已读结果")
    public record MarkReadResult(long modifiedCount) {
    }

    @Schema(description = "公告发送结果")
    public record AnnouncementResult(long recipientCount) {
    }

    @Schema(description = "SSE 通知事件")
    public record NotificationEvent(
            String event,
            Long toUserId,
            NotificationResponse notification,
            Long unreadCount,
            Instant ts
    ) {
        public static NotificationEvent notification(Notification notification, long unreadCount, Instant ts) {
            return new NotificationEvent("notification", notification.getToUserId(),
                    NotificationResponse.from(notification), unreadCount, ts);
        }

        public static NotificationEvent unread(Long toUserId, long unreadCount, Instant ts) {
            return new NotificationEvent("unread-count", toUserId, null, unreadCount, ts);
        }
    }
}
