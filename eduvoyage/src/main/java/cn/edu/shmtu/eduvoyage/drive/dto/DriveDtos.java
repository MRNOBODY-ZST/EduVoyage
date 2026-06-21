package cn.edu.shmtu.eduvoyage.drive.dto;

import cn.edu.shmtu.eduvoyage.drive.domain.DriveFile;
import cn.edu.shmtu.eduvoyage.drive.domain.DriveNode;
import cn.edu.shmtu.eduvoyage.drive.domain.DriveQuota;
import cn.edu.shmtu.eduvoyage.drive.domain.DriveShare;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request and response records for the drive module.
 */
public final class DriveDtos {

    private DriveDtos() {
    }

    @Schema(description = "创建目录请求")
    public record DirectoryCreateRequest(
            @Schema(description = "目录名称")
            @NotBlank(message = "名称不能为空")
            @Size(max = 255, message = "名称不能超过255个字符")
            String name,

            @Schema(description = "父目录 id，0 表示根")
            Long parentId,

            @Schema(description = "空间类型：1 个人空间，2 课程空间")
            Integer spaceType,

            @Schema(description = "课程 id，仅课程空间需要")
            Long courseId
    ) {
    }

    @Schema(description = "节点重命名请求")
    public record RenameRequest(
            @Schema(description = "新名称")
            @NotBlank(message = "名称不能为空")
            @Size(max = 255, message = "名称不能超过255个字符")
            String name
    ) {
    }

    @Schema(description = "移动节点请求")
    public record MoveRequest(
            @Schema(description = "目标父目录 id，0 表示移动到当前空间根")
            @NotNull(message = "目标目录不能为空")
            Long targetParentId
    ) {
    }

    @Schema(description = "秒传请求")
    public record InstantUploadRequest(
            @Schema(description = "文件显示名称")
            @NotBlank(message = "文件名不能为空")
            @Size(max = 255, message = "文件名不能超过255个字符")
            String name,

            @Schema(description = "父目录 id，0 表示根")
            Long parentId,

            @Schema(description = "空间类型：1 个人空间，2 课程空间")
            Integer spaceType,

            @Schema(description = "课程 id，仅课程空间需要")
            Long courseId,

            @Schema(description = "文件 sha256")
            @NotBlank(message = "sha256 不能为空")
            @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "sha256 格式不正确")
            String sha256,

            @Schema(description = "文件大小")
            @NotNull(message = "文件大小不能为空")
            @Positive(message = "文件大小必须为正")
            Long size,

            @Schema(description = "MIME 类型")
            @Size(max = 128, message = "MIME 类型不能超过128个字符")
            String mime
    ) {
    }

    @Schema(description = "创建分享请求")
    public record ShareCreateRequest(
            @Schema(description = "节点 id")
            @NotNull(message = "节点 id 不能为空")
            Long nodeId,

            @Schema(description = "提取码，留空自动生成")
            @Size(max = 16, message = "提取码不能超过16个字符")
            String extractCode,

            @Schema(description = "过期时间，留空表示长期有效")
            @Future(message = "过期时间必须晚于当前时间")
            LocalDateTime expireAt
    ) {
    }

    @Schema(description = "访问分享请求")
    public record ShareAccessRequest(
            @Schema(description = "提取码")
            String extractCode
    ) {
    }

    @Schema(description = "网盘节点")
    public record DriveNodeResponse(
            Long id,
            Long ownerId,
            Integer spaceType,
            Long courseId,
            Long parentId,
            String name,
            boolean directory,
            Long fileId,
            String sha256,
            Long size,
            String mime,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static DriveNodeResponse from(DriveNode node, DriveFile file) {
            return new DriveNodeResponse(
                    node.getId(),
                    node.getOwnerId(),
                    node.getSpaceType(),
                    node.getCourseId(),
                    node.getParentId(),
                    node.getName(),
                    node.directory(),
                    node.getFileId(),
                    file == null ? null : file.getSha256(),
                    file == null ? null : file.getSize(),
                    file == null ? null : file.getMime(),
                    node.getCreatedAt(),
                    node.getUpdatedAt());
        }
    }

    @Schema(description = "面包屑项")
    public record BreadcrumbItem(
            Long id,
            String name,
            Long parentId
    ) {
        public static BreadcrumbItem from(DriveNode node) {
            return new BreadcrumbItem(node.getId(), node.getName(), node.getParentId());
        }
    }

    @Schema(description = "目录树节点")
    public record DriveTreeNode(
            DriveNodeResponse node,
            List<DriveTreeNode> children
    ) {
    }

    @Schema(description = "下载/预览 URL")
    public record FileUrlResponse(
            Long nodeId,
            String url,
            LocalDateTime expireAt
    ) {
    }

    @Schema(description = "配额信息")
    public record QuotaResponse(
            Long userId,
            Long totalBytes,
            Long usedBytes,
            Long remainingBytes
    ) {
        public static QuotaResponse from(DriveQuota quota) {
            long used = quota.getUsedBytes() == null ? 0 : quota.getUsedBytes();
            long total = quota.getTotalBytes() == null ? 0 : quota.getTotalBytes();
            return new QuotaResponse(quota.getUserId(), total, used, Math.max(total - used, 0));
        }
    }

    @Schema(description = "分享信息")
    public record ShareResponse(
            Long id,
            Long nodeId,
            Long ownerId,
            String token,
            String extractCode,
            LocalDateTime expireAt,
            Integer viewCount,
            LocalDateTime createdAt,
            String accessPath
    ) {
        public static ShareResponse from(DriveShare share) {
            return new ShareResponse(
                    share.getId(),
                    share.getNodeId(),
                    share.getOwnerId(),
                    share.getToken(),
                    share.getExtractCode(),
                    share.getExpireAt(),
                    share.getViewCount(),
                    share.getCreatedAt(),
                    "/api/drive/share/" + share.getToken());
        }
    }

    @Schema(description = "分享访问视图")
    public record ShareViewResponse(
            ShareResponse share,
            DriveNodeResponse node,
            List<DriveNodeResponse> children,
            String url,
            LocalDateTime urlExpireAt
    ) {
    }
}
