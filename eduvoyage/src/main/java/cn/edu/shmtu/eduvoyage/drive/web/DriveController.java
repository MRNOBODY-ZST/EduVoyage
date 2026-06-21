package cn.edu.shmtu.eduvoyage.drive.web;

import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.BreadcrumbItem;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.DirectoryCreateRequest;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.DriveNodeResponse;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.DriveTreeNode;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.FileUrlResponse;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.InstantUploadRequest;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.MoveRequest;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.QuotaResponse;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.RenameRequest;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.ShareAccessRequest;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.ShareCreateRequest;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.ShareResponse;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.ShareViewResponse;
import cn.edu.shmtu.eduvoyage.drive.service.DriveService;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Drive APIs for personal and course spaces.
 */
@Tag(name = "网盘", description = "个人/课程网盘、上传下载与分享")
@RestController
public class DriveController {

    private final DriveService driveService;

    public DriveController(DriveService driveService) {
        this.driveService = driveService;
    }

    @Operation(summary = "列出目录")
    @PreAuthorize("hasAuthority('drive:read')")
    @GetMapping("/api/drive/nodes")
    public Mono<Result<List<DriveNodeResponse>>> list(@RequestParam(required = false) Long parentId,
                                                      @RequestParam(required = false) Integer spaceType,
                                                      @RequestParam(required = false) Long courseId,
                                                      @AuthenticationPrincipal AuthUser user) {
        return driveService.list(parentId, spaceType, courseId, user).map(Result::success);
    }

    @Operation(summary = "目录树")
    @PreAuthorize("hasAuthority('drive:read')")
    @GetMapping("/api/drive/tree")
    public Mono<Result<List<DriveTreeNode>>> tree(@RequestParam(required = false) Integer spaceType,
                                                  @RequestParam(required = false) Long courseId,
                                                  @AuthenticationPrincipal AuthUser user) {
        return driveService.tree(spaceType, courseId, user).map(Result::success);
    }

    @Operation(summary = "面包屑")
    @PreAuthorize("hasAuthority('drive:read')")
    @GetMapping("/api/drive/nodes/{id}/breadcrumb")
    public Mono<Result<List<BreadcrumbItem>>> breadcrumb(@PathVariable Long id,
                                                        @AuthenticationPrincipal AuthUser user) {
        return driveService.breadcrumb(id, user).map(Result::success);
    }

    @Operation(summary = "创建目录")
    @PreAuthorize("hasAuthority('drive:write')")
    @PostMapping("/api/drive/directories")
    public Mono<Result<DriveNodeResponse>> createDirectory(@Valid @RequestBody DirectoryCreateRequest req,
                                                          @AuthenticationPrincipal AuthUser user) {
        return driveService.createDirectory(req, user).map(Result::success);
    }

    @Operation(summary = "上传文件")
    @PreAuthorize("hasAuthority('drive:write')")
    @PostMapping(value = "/api/drive/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Result<DriveNodeResponse>> upload(@RequestPart("file") FilePart file,
                                                  @RequestParam(required = false) Long parentId,
                                                  @RequestParam(required = false) Integer spaceType,
                                                  @RequestParam(required = false) Long courseId,
                                                  @AuthenticationPrincipal AuthUser user) {
        return driveService.upload(file, parentId, spaceType, courseId, user).map(Result::success);
    }

    @Operation(summary = "文件秒传")
    @PreAuthorize("hasAuthority('drive:write')")
    @PostMapping("/api/drive/files/instant")
    public Mono<Result<DriveNodeResponse>> instantUpload(@Valid @RequestBody InstantUploadRequest req,
                                                        @AuthenticationPrincipal AuthUser user) {
        return driveService.instantUpload(req, user).map(Result::success);
    }

    @Operation(summary = "重命名节点")
    @PreAuthorize("hasAuthority('drive:write')")
    @PutMapping("/api/drive/nodes/{id}/name")
    public Mono<Result<DriveNodeResponse>> rename(@PathVariable Long id,
                                                  @Valid @RequestBody RenameRequest req,
                                                  @AuthenticationPrincipal AuthUser user) {
        return driveService.rename(id, req, user).map(Result::success);
    }

    @Operation(summary = "移动节点")
    @PreAuthorize("hasAuthority('drive:write')")
    @PutMapping("/api/drive/nodes/{id}/parent")
    public Mono<Result<DriveNodeResponse>> move(@PathVariable Long id,
                                                @Valid @RequestBody MoveRequest req,
                                                @AuthenticationPrincipal AuthUser user) {
        return driveService.move(id, req, user).map(Result::success);
    }

    @Operation(summary = "删除节点")
    @PreAuthorize("hasAuthority('drive:write')")
    @DeleteMapping("/api/drive/nodes/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id,
                                     @AuthenticationPrincipal AuthUser user) {
        return driveService.delete(id, user).thenReturn(Result.<Void>success());
    }

    @Operation(summary = "下载 URL")
    @PreAuthorize("hasAuthority('drive:read')")
    @GetMapping("/api/drive/nodes/{id}/download-url")
    public Mono<Result<FileUrlResponse>> downloadUrl(@PathVariable Long id,
                                                     @AuthenticationPrincipal AuthUser user) {
        return driveService.fileUrl(id, user).map(Result::success);
    }

    @Operation(summary = "预览 URL")
    @PreAuthorize("hasAuthority('drive:read')")
    @GetMapping("/api/drive/nodes/{id}/preview-url")
    public Mono<Result<FileUrlResponse>> previewUrl(@PathVariable Long id,
                                                    @AuthenticationPrincipal AuthUser user) {
        return driveService.fileUrl(id, user).map(Result::success);
    }

    @Operation(summary = "我的网盘配额")
    @PreAuthorize("hasAuthority('drive:read')")
    @GetMapping("/api/drive/quota")
    public Mono<Result<QuotaResponse>> quota(@AuthenticationPrincipal AuthUser user) {
        return driveService.quota(user).map(Result::success);
    }

    @Operation(summary = "创建分享")
    @PreAuthorize("hasAuthority('drive:write')")
    @PostMapping("/api/drive/shares")
    public Mono<Result<ShareResponse>> createShare(@Valid @RequestBody ShareCreateRequest req,
                                                   @AuthenticationPrincipal AuthUser user) {
        return driveService.createShare(req, user).map(Result::success);
    }

    @Operation(summary = "我的分享")
    @PreAuthorize("hasAuthority('drive:read')")
    @GetMapping("/api/drive/shares/my")
    public Mono<Result<List<ShareResponse>>> myShares(@AuthenticationPrincipal AuthUser user) {
        return driveService.myShares(user).map(Result::success);
    }

    @Operation(summary = "取消分享")
    @PreAuthorize("hasAuthority('drive:write')")
    @DeleteMapping("/api/drive/shares/{id}")
    public Mono<Result<Void>> deleteShare(@PathVariable Long id,
                                          @AuthenticationPrincipal AuthUser user) {
        return driveService.deleteShare(id, user).thenReturn(Result.<Void>success());
    }

    @Operation(summary = "访问分享")
    @PostMapping("/api/drive/share/{token}")
    public Mono<Result<ShareViewResponse>> accessShare(@PathVariable String token,
                                                       @RequestBody(required = false) Mono<ShareAccessRequest> req) {
        return req.defaultIfEmpty(new ShareAccessRequest(null))
                .flatMap(body -> driveService.accessShare(token, body))
                .map(Result::success);
    }
}
