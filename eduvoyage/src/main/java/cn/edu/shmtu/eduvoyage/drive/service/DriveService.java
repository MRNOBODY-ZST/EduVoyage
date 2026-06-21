package cn.edu.shmtu.eduvoyage.drive.service;

import cn.edu.shmtu.eduvoyage.course.domain.CourseEnrollment;
import cn.edu.shmtu.eduvoyage.course.repository.CourseEnrollmentRepository;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.drive.domain.DriveFile;
import cn.edu.shmtu.eduvoyage.drive.domain.DriveNode;
import cn.edu.shmtu.eduvoyage.drive.domain.DriveQuota;
import cn.edu.shmtu.eduvoyage.drive.domain.DriveShare;
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
import cn.edu.shmtu.eduvoyage.drive.repository.DriveFileRepository;
import cn.edu.shmtu.eduvoyage.drive.repository.DriveNodeRepository;
import cn.edu.shmtu.eduvoyage.drive.repository.DriveQueryRepository;
import cn.edu.shmtu.eduvoyage.drive.repository.DriveQuotaRepository;
import cn.edu.shmtu.eduvoyage.drive.repository.DriveShareRepository;
import cn.edu.shmtu.eduvoyage.shared.config.EduVoyageProperties;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import cn.edu.shmtu.eduvoyage.shared.storage.MinioStorageService;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Netdisk application service: logical tree operations, uploads with sha256
 * deduplication, quota accounting, presigned URLs and public shares.
 */
@Service
public class DriveService {

    private static final long ROOT_PARENT = 0L;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String TOKEN_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DriveNodeRepository nodeRepository;
    private final DriveFileRepository fileRepository;
    private final DriveShareRepository shareRepository;
    private final DriveQuotaRepository quotaRepository;
    private final DriveQueryRepository queryRepository;
    private final CourseService courseService;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final MinioStorageService storageService;
    private final DriveUploadReader uploadReader;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final EduVoyageProperties properties;

    public DriveService(DriveNodeRepository nodeRepository,
                        DriveFileRepository fileRepository,
                        DriveShareRepository shareRepository,
                        DriveQuotaRepository quotaRepository,
                        DriveQueryRepository queryRepository,
                        CourseService courseService,
                        CourseEnrollmentRepository enrollmentRepository,
                        MinioStorageService storageService,
                        DriveUploadReader uploadReader,
                        R2dbcEntityTemplate entityTemplate,
                        IdGenerator idGenerator,
                        Clock clock,
                        EduVoyageProperties properties) {
        this.nodeRepository = nodeRepository;
        this.fileRepository = fileRepository;
        this.shareRepository = shareRepository;
        this.quotaRepository = quotaRepository;
        this.queryRepository = queryRepository;
        this.courseService = courseService;
        this.enrollmentRepository = enrollmentRepository;
        this.storageService = storageService;
        this.uploadReader = uploadReader;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.properties = properties;
    }

    // ------------------------------------------------------------- queries

    public Mono<List<DriveNodeResponse>> list(Long parentId, Integer spaceType, Long courseId, AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        long safeParent = parentId == null ? ROOT_PARENT : parentId;
        if (safeParent == ROOT_PARENT) {
            BizException typeError = DriveRules.validateSpaceType(spaceType);
            if (typeError != null) {
                return Mono.error(typeError);
            }
            BizException spaceError = DriveRules.validateSpace(spaceType, courseId);
            if (spaceError != null) {
                return Mono.error(spaceError);
            }
            int space = DriveRules.normalizeSpaceType(spaceType);
            Flux<DriveNode> roots = space == DriveNode.SPACE_PERSONAL
                    ? nodeRepository.findPersonalRootChildren(requireUser(user).id())
                    : nodeRepository.findCourseRootChildren(courseId);
            return requireSpaceReadable(space, courseId, user)
                    .thenMany(roots.flatMap(this::toResponse))
                    .collectList();
        }
        return requireNode(safeParent)
                .flatMap(parent -> {
                    if (!parent.directory()) {
                        return Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "父节点不是目录"));
                    }
                    return requireNodeReadable(parent, user)
                            .thenMany(nodeRepository.findActiveChildren(parent.getId()).flatMap(this::toResponse))
                            .collectList();
                });
    }

    public Mono<List<BreadcrumbItem>> breadcrumb(Long nodeId, AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        return requireNode(nodeId)
                .flatMap(node -> requireNodeReadable(node, user).thenReturn(node))
                .thenMany(queryRepository.findBreadcrumb(nodeId))
                .map(BreadcrumbItem::from)
                .collectList();
    }

    public Mono<List<DriveTreeNode>> tree(Integer spaceType, Long courseId, AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        BizException typeError = DriveRules.validateSpaceType(spaceType);
        if (typeError != null) {
            return Mono.error(typeError);
        }
        BizException spaceError = DriveRules.validateSpace(spaceType, courseId);
        if (spaceError != null) {
            return Mono.error(spaceError);
        }
        int space = DriveRules.normalizeSpaceType(spaceType);
        Flux<DriveNode> rows = space == DriveNode.SPACE_PERSONAL
                ? queryRepository.findPersonalTree(requireUser(user).id())
                : queryRepository.findCourseTree(courseId);
        return requireSpaceReadable(space, courseId, user)
                .thenMany(rows.flatMap(this::toResponse))
                .collectList()
                .map(DriveTreeBuilder::build);
    }

    public Mono<QuotaResponse> quota(AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        return getOrCreateQuota(requireUser(user)).map(QuotaResponse::from);
    }

    public Mono<FileUrlResponse> fileUrl(Long nodeId, AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        return requireNode(nodeId)
                .flatMap(node -> requireNodeReadable(node, user).thenReturn(node))
                .flatMap(this::requireFileNode)
                .flatMap(pair -> storageService.presignedGetUrl(pair.file().getObjectKey())
                        .map(url -> new FileUrlResponse(pair.node().getId(), url,
                                LocalDateTime.now(clock).plus(properties.storage().minio().presignExpiry()))));
    }

    // -------------------------------------------------------------- create

    @Transactional
    public Mono<DriveNodeResponse> createDirectory(DirectoryCreateRequest req, AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        String name = DriveRules.normalizeName(req.name());
        BizException nameError = DriveRules.validateNodeName(name);
        if (nameError != null) {
            return Mono.error(nameError);
        }
        return resolveTarget(req.parentId(), req.spaceType(), req.courseId(), user, true)
                .flatMap(target -> assertNameAvailable(target, name, requireUser(user).id(), null)
                        .then(Mono.defer(() -> {
                            DriveNode node = DriveNode.builder()
                                    .id(idGenerator.nextId())
                                    .ownerId(user.id())
                                    .spaceType(target.spaceType())
                                    .courseId(target.courseId())
                                    .parentId(target.parentId())
                                    .name(name)
                                    .isDir(DriveNode.DIRECTORY)
                                    .deleted(0)
                                    .build();
                            return entityTemplate.insert(DriveNode.class).using(node);
                        })))
                .flatMap(this::toResponse);
    }

    @Transactional
    public Mono<DriveNodeResponse> upload(FilePart file, Long parentId, Integer spaceType, Long courseId, AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        return uploadReader.read(file)
                .flatMap(payload -> createFileNode(payload, parentId, spaceType, courseId, user)
                        .flatMap(resp -> uploadReader.deleteTemp(payload.path()).thenReturn(resp))
                        .onErrorResume(e -> uploadReader.deleteTemp(payload.path()).then(Mono.error(e))));
    }

    @Transactional
    public Mono<DriveNodeResponse> instantUpload(InstantUploadRequest req, AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        String name = DriveRules.normalizeName(req.name());
        String sha256 = DriveRules.normalizeSha256(req.sha256());
        BizException nameError = DriveRules.validateNodeName(name);
        if (nameError != null) {
            return Mono.error(nameError);
        }
        BizException shaError = DriveRules.validateSha256(sha256);
        if (shaError != null) {
            return Mono.error(shaError);
        }
        if (req.size() == null || req.size() <= 0) {
            return Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "文件大小必须为正"));
        }
        String mime = DriveRules.normalizeMime(req.mime());
        return resolveTarget(req.parentId(), req.spaceType(), req.courseId(), user, true)
                .flatMap(target -> assertNameAvailable(target, name, requireUser(user).id(), null)
                        .then(reserveQuotaOrError(requireUser(user), req.size()))
                        .then(Mono.defer(() -> fileRepository.findBySha256(sha256)
                                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.FILE_NOT_FOUND,
                                        "服务端未命中该文件，无法秒传")))
                                .flatMap(file -> {
                                    if (!Objects.equals(file.getSize(), req.size())) {
                                        return Mono.error(new BizException(BizErrorCode.DATA_CONFLICT,
                                                "sha256 对应文件大小不一致"));
                                    }
                                    return queryRepository.incrementFileRef(file.getId()).thenReturn(file);
                                })
                                .flatMap(file -> insertFileNode(name, mime, target, file, user)))))
                .flatMap(this::toResponse);
    }

    private Mono<DriveNodeResponse> createFileNode(DriveUploadPayload payload, Long parentId,
                                                  Integer spaceType, Long courseId, AuthUser user) {
        return resolveTarget(parentId, spaceType, courseId, user, true)
                .flatMap(target -> assertNameAvailable(target, payload.name(), requireUser(user).id(), null)
                        .then(reserveQuotaOrError(requireUser(user), payload.size()))
                        .then(Mono.defer(() -> ensurePhysicalFile(payload)))
                        .flatMap(file -> insertFileNode(payload.name(), payload.mime(), target, file, user)))
                .flatMap(this::toResponse);
    }

    private Mono<DriveFile> ensurePhysicalFile(DriveUploadPayload payload) {
        return fileRepository.findBySha256(payload.sha256())
                .flatMap(existing -> {
                    if (!Objects.equals(existing.getSize(), payload.size())) {
                        return Mono.error(new BizException(BizErrorCode.DATA_CONFLICT, "sha256 对应文件大小不一致"));
                    }
                    return queryRepository.incrementFileRef(existing.getId()).thenReturn(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    String objectKey = DriveRules.objectKey(payload.sha256());
                    DriveFile file = DriveFile.builder()
                            .id(idGenerator.nextId())
                            .sha256(payload.sha256())
                            .size(payload.size())
                            .mime(payload.mime())
                            .bucket(storageService.bucket())
                            .objectKey(objectKey)
                            .refCount(1)
                            .build();
                    return storageService.putFile(objectKey, payload.path(), payload.size(), payload.mime())
                            .then(entityTemplate.insert(DriveFile.class).using(file))
                            .onErrorResume(DataIntegrityViolationException.class, e -> fileRepository.findBySha256(payload.sha256())
                                    .flatMap(existing -> queryRepository.incrementFileRef(existing.getId()).thenReturn(existing)));
                }));
    }

    private Mono<DriveNode> insertFileNode(String name, String mime, Target target, DriveFile file, AuthUser user) {
        DriveNode node = DriveNode.builder()
                .id(idGenerator.nextId())
                .ownerId(requireUser(user).id())
                .spaceType(target.spaceType())
                .courseId(target.courseId())
                .parentId(target.parentId())
                .name(name)
                .isDir(DriveNode.FILE)
                .fileId(file.getId())
                .deleted(0)
                .build();
        return entityTemplate.insert(DriveNode.class).using(node);
    }

    // -------------------------------------------------------------- update

    @Transactional
    public Mono<DriveNodeResponse> rename(Long nodeId, RenameRequest req, AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        String name = DriveRules.normalizeName(req.name());
        BizException nameError = DriveRules.validateNodeName(name);
        if (nameError != null) {
            return Mono.error(nameError);
        }
        return requireNode(nodeId)
                .flatMap(node -> requireNodeWritable(node, user).thenReturn(node))
                .flatMap(node -> assertNameAvailable(targetOf(node.getParentId(), node.getSpaceType(), node.getCourseId()),
                        name, node.getOwnerId(), node.getId()).thenReturn(node))
                .flatMap(node -> {
                    node.setName(name);
                    return nodeRepository.save(node);
                })
                .flatMap(this::toResponse);
    }

    @Transactional
    public Mono<DriveNodeResponse> move(Long nodeId, MoveRequest req, AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        if (req.targetParentId() == null || req.targetParentId() < 0) {
            return Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "目标目录不正确"));
        }
        return requireNode(nodeId)
                .flatMap(node -> requireNodeWritable(node, user).thenReturn(node))
                .flatMap(node -> resolveMoveTarget(node, req.targetParentId(), user)
                        .flatMap(target -> assertNotMovingIntoSelf(node, target.parentId())
                                .then(assertNameAvailable(target, node.getName(), node.getOwnerId(), node.getId()))
                                .thenReturn(target))
                        .flatMap(target -> {
                            node.setParentId(target.parentId());
                            return nodeRepository.save(node);
                        }))
                .flatMap(this::toResponse);
    }

    @Transactional
    public Mono<Void> delete(Long nodeId, AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        return requireNode(nodeId)
                .flatMap(node -> requireNodeWritable(node, user).thenReturn(node))
                .thenMany(queryRepository.findSubtree(nodeId))
                .collectList()
                .flatMap(nodes -> {
                    if (nodes.isEmpty()) {
                        return Mono.error(new BizException(BizErrorCode.FILE_NOT_FOUND, "节点不存在"));
                    }
                    Mono<Void> markDeleted = Flux.fromIterable(nodes)
                            .concatMap(node -> {
                                node.setDeleted(1);
                                return nodeRepository.save(node);
                            })
                            .then();
                    Mono<Void> releaseFiles = Flux.fromIterable(nodes)
                            .filter(DriveNode::file)
                            .concatMap(this::releaseFileReference)
                            .then();
                    return markDeleted.then(releaseFiles);
                });
    }

    // -------------------------------------------------------------- share

    @Transactional
    public Mono<ShareResponse> createShare(ShareCreateRequest req, AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        BizException codeError = DriveRules.validateExtractCode(req.extractCode());
        if (codeError != null) {
            return Mono.error(codeError);
        }
        String extractCode = req.extractCode() == null || req.extractCode().isBlank()
                ? randomString(4)
                : req.extractCode().trim();
        return requireNode(req.nodeId())
                .flatMap(node -> requireNodeWritable(node, user).thenReturn(node))
                .flatMap(node -> insertShare(node, requireUser(user), extractCode, req.expireAt(), 0))
                .map(ShareResponse::from);
    }

    public Mono<List<ShareResponse>> myShares(AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        return shareRepository.findActiveByOwnerId(requireUser(user).id())
                .map(ShareResponse::from)
                .collectList();
    }

    @Transactional
    public Mono<Void> deleteShare(Long shareId, AuthUser user) {
        if (user == null) {
            return Mono.error(unauthenticated());
        }
        return shareRepository.findActiveById(shareId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "分享不存在")))
                .flatMap(share -> {
                    AuthUser actor = requireUser(user);
                    if (!isAdmin(actor) && !Objects.equals(share.getOwnerId(), actor.id())) {
                        return Mono.error(new BizException(BizErrorCode.ACCESS_DENIED, "无权删除该分享"));
                    }
                    share.setDeleted(1);
                    return shareRepository.save(share);
                })
                .then();
    }

    @Transactional
    public Mono<ShareViewResponse> accessShare(String token, ShareAccessRequest req) {
        String code = req == null ? null : req.extractCode();
        return shareRepository.findActiveByToken(token)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.SHARE_EXPIRED, "分享链接不存在或已失效")))
                .flatMap(share -> validateShareAccess(share, code).thenReturn(share))
                .flatMap(share -> nodeRepository.findActiveById(share.getNodeId())
                        .switchIfEmpty(Mono.error(new BizException(BizErrorCode.FILE_NOT_FOUND, "分享节点不存在")))
                        .flatMap(node -> queryRepository.incrementShareView(share.getId())
                                .thenReturn(incrementLocalView(share))
                                .flatMap(updatedShare -> shareView(updatedShare, node))));
    }

    private Mono<DriveShare> insertShare(DriveNode node, AuthUser user, String extractCode,
                                        LocalDateTime expireAt, int attempt) {
        if (attempt >= 3) {
            return Mono.error(new BizException(BizErrorCode.DATA_CONFLICT, "分享 token 生成冲突，请重试"));
        }
        DriveShare share = DriveShare.builder()
                .id(idGenerator.nextId())
                .nodeId(node.getId())
                .ownerId(user.id())
                .token(randomString(32))
                .extractCode(extractCode)
                .expireAt(expireAt)
                .viewCount(0)
                .deleted(0)
                .build();
        return entityTemplate.insert(DriveShare.class).using(share)
                .onErrorResume(DataIntegrityViolationException.class,
                        e -> insertShare(node, user, extractCode, expireAt, attempt + 1));
    }

    private Mono<Void> validateShareAccess(DriveShare share, String code) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (share.getExpireAt() != null && now.isAfter(share.getExpireAt())) {
            return Mono.error(new BizException(BizErrorCode.SHARE_EXPIRED, "分享链接已过期"));
        }
        String expected = share.getExtractCode();
        if (expected != null && !expected.isBlank() && !expected.equals(code == null ? "" : code.trim())) {
            return Mono.error(new BizException(BizErrorCode.SHARE_CODE_INVALID, "提取码错误"));
        }
        return Mono.empty();
    }

    private Mono<ShareViewResponse> shareView(DriveShare share, DriveNode node) {
        Mono<DriveNodeResponse> nodeView = toResponse(node);
        if (node.directory()) {
            Mono<List<DriveNodeResponse>> children = nodeRepository.findActiveChildren(node.getId())
                    .flatMap(this::toResponse)
                    .collectList();
            return Mono.zip(nodeView, children)
                    .map(t -> new ShareViewResponse(ShareResponse.from(share), t.getT1(), t.getT2(), null, null));
        }
        return requireFileNode(node)
                .flatMap(pair -> storageService.presignedGetUrl(pair.file().getObjectKey()))
                .zipWith(nodeView)
                .map(t -> new ShareViewResponse(ShareResponse.from(share), t.getT2(), List.of(), t.getT1(),
                        LocalDateTime.now(clock).plus(properties.storage().minio().presignExpiry())));
    }

    // ------------------------------------------------------------ helpers

    private Mono<DriveNode> requireNode(Long id) {
        if (id == null || id <= 0) {
            return Mono.error(new BizException(BizErrorCode.FILE_NOT_FOUND, "节点不存在"));
        }
        return nodeRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.FILE_NOT_FOUND, "节点不存在")));
    }

    private Mono<NodeFilePair> requireFileNode(DriveNode node) {
        if (!node.file() || node.getFileId() == null) {
            return Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "节点不是文件"));
        }
        return fileRepository.findById(node.getFileId())
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.FILE_NOT_FOUND, "物理文件不存在")))
                .map(file -> new NodeFilePair(node, file));
    }

    private Mono<DriveNodeResponse> toResponse(DriveNode node) {
        if (node.getFileId() == null) {
            return Mono.just(DriveNodeResponse.from(node, null));
        }
        return fileRepository.findById(node.getFileId())
                .map(file -> DriveNodeResponse.from(node, file))
                .defaultIfEmpty(DriveNodeResponse.from(node, null));
    }

    private Mono<Target> resolveTarget(Long parentId, Integer spaceType, Long courseId, AuthUser user, boolean write) {
        long safeParent = parentId == null ? ROOT_PARENT : parentId;
        if (safeParent == ROOT_PARENT) {
            BizException typeError = DriveRules.validateSpaceType(spaceType);
            if (typeError != null) {
                return Mono.error(typeError);
            }
            BizException spaceError = DriveRules.validateSpace(spaceType, courseId);
            if (spaceError != null) {
                return Mono.error(spaceError);
            }
            int space = DriveRules.normalizeSpaceType(spaceType);
            Mono<Void> auth = write ? requireSpaceWritable(space, courseId, user)
                    : requireSpaceReadable(space, courseId, user);
            return auth.thenReturn(new Target(ROOT_PARENT, space, courseId));
        }
        return requireNode(safeParent)
                .flatMap(parent -> {
                    if (!parent.directory()) {
                        return Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "父节点不是目录"));
                    }
                    Mono<Void> auth = write ? requireNodeWritable(parent, user) : requireNodeReadable(parent, user);
                    return auth.thenReturn(targetOf(parent.getId(), parent.getSpaceType(), parent.getCourseId()));
                });
    }

    private Mono<Target> resolveMoveTarget(DriveNode node, Long targetParentId, AuthUser user) {
        if (targetParentId == ROOT_PARENT) {
            return requireSpaceWritable(node.getSpaceType(), node.getCourseId(), user)
                    .thenReturn(targetOf(ROOT_PARENT, node.getSpaceType(), node.getCourseId()));
        }
        return requireNode(targetParentId)
                .flatMap(parent -> {
                    if (!parent.directory()) {
                        return Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "目标节点不是目录"));
                    }
                    if (!Objects.equals(parent.getSpaceType(), node.getSpaceType())
                            || !Objects.equals(parent.getCourseId(), node.getCourseId())) {
                        return Mono.error(new BizException(BizErrorCode.OPERATION_NOT_ALLOWED, "不能跨空间移动节点"));
                    }
                    return requireNodeWritable(parent, user)
                            .thenReturn(targetOf(parent.getId(), parent.getSpaceType(), parent.getCourseId()));
                });
    }

    private Mono<Void> assertNotMovingIntoSelf(DriveNode node, Long targetParentId) {
        if (!node.directory()) {
            return Mono.empty();
        }
        if (Objects.equals(node.getId(), targetParentId)) {
            return Mono.error(new BizException(BizErrorCode.OPERATION_NOT_ALLOWED, "不能移动到自身目录"));
        }
        return queryRepository.findSubtree(node.getId())
                .any(child -> Objects.equals(child.getId(), targetParentId))
                .flatMap(found -> found
                        ? Mono.error(new BizException(BizErrorCode.OPERATION_NOT_ALLOWED, "不能移动到子目录"))
                        : Mono.empty());
    }

    private Mono<Void> assertNameAvailable(Target target, String name, Long ownerId, Long excludeId) {
        Mono<Boolean> exists = target.parentId() == ROOT_PARENT
                ? (target.spaceType() == DriveNode.SPACE_PERSONAL
                ? queryRepository.personalRootNameExists(ownerId, name, excludeId)
                : queryRepository.courseRootNameExists(target.courseId(), name, excludeId))
                : queryRepository.childNameExists(target.parentId(), name, excludeId);
        return exists.flatMap(found -> found
                ? Mono.error(new BizException(BizErrorCode.DATA_CONFLICT, "同级目录下已存在同名节点"))
                : Mono.empty());
    }

    private Mono<Void> reserveQuotaOrError(AuthUser user, long bytes) {
        return getOrCreateQuota(user)
                .then(queryRepository.reserveQuota(user.id(), bytes))
                .flatMap(ok -> ok
                        ? Mono.empty()
                        : Mono.error(new BizException(BizErrorCode.QUOTA_EXCEEDED, "存储空间不足")));
    }

    private Mono<DriveQuota> getOrCreateQuota(AuthUser user) {
        return quotaRepository.findById(user.id())
                .switchIfEmpty(Mono.defer(() -> {
                    DriveQuota quota = DriveQuota.builder()
                            .userId(user.id())
                            .totalBytes(defaultQuota(user))
                            .usedBytes(0L)
                            .build();
                    return entityTemplate.insert(DriveQuota.class).using(quota)
                            .onErrorResume(DataIntegrityViolationException.class, e -> quotaRepository.findById(user.id()));
                }));
    }

    private long defaultQuota(AuthUser user) {
        EduVoyageProperties.Quota quota = properties.storage().quota();
        if (user.hasRole(ROLE_ADMIN)) {
            return quota.adminBytes();
        }
        if (user.hasRole(ROLE_TEACHER)) {
            return quota.teacherBytes();
        }
        return quota.studentBytes();
    }

    private Mono<Void> releaseFileReference(DriveNode node) {
        if (node.getFileId() == null) {
            return Mono.empty();
        }
        return fileRepository.findById(node.getFileId())
                .flatMap(file -> queryRepository.releaseQuota(node.getOwnerId(), file.getSize())
                        .then(queryRepository.decrementFileRef(file.getId()))
                        .then(fileRepository.findById(file.getId()))
                        .flatMap(updated -> updated.getRefCount() != null && updated.getRefCount() == 0
                                ? storageService.remove(updated.getObjectKey())
                                .then(queryRepository.deletePhysicalFileRow(updated.getId()))
                                : Mono.empty()))
                .then();
    }

    private Mono<Void> requireNodeReadable(DriveNode node, AuthUser user) {
        return requireSpaceReadable(node.getSpaceType(), node.getCourseId(), user)
                .then(Mono.defer(() -> {
                    AuthUser actor = requireUser(user);
                    if (node.getSpaceType() == DriveNode.SPACE_PERSONAL
                            && !isAdmin(actor)
                            && !Objects.equals(node.getOwnerId(), actor.id())) {
                        return Mono.error(new BizException(BizErrorCode.ACCESS_DENIED, "无权访问该节点"));
                    }
                    return Mono.empty();
                }));
    }

    private Mono<Void> requireNodeWritable(DriveNode node, AuthUser user) {
        return requireSpaceWritable(node.getSpaceType(), node.getCourseId(), user)
                .then(Mono.defer(() -> {
                    AuthUser actor = requireUser(user);
                    if (node.getSpaceType() == DriveNode.SPACE_PERSONAL
                            && !isAdmin(actor)
                            && !Objects.equals(node.getOwnerId(), actor.id())) {
                        return Mono.error(new BizException(BizErrorCode.ACCESS_DENIED, "无权操作该节点"));
                    }
                    return Mono.empty();
                }));
    }

    private Mono<Void> requireSpaceReadable(Integer spaceType, Long courseId, AuthUser user) {
        AuthUser actor = requireUser(user);
        if (isAdmin(actor)) {
            return Mono.empty();
        }
        if (spaceType == DriveNode.SPACE_PERSONAL) {
            return Mono.empty();
        }
        return courseService.requireCourseEditable(courseId, actor)
                .then()
                .onErrorResume(BizException.class, e -> {
                    if (e.getErrorCode() != BizErrorCode.ACCESS_DENIED) {
                        return Mono.error(e);
                    }
                    return enrollmentRepository.findByCourseAndStudent(courseId, actor.id())
                            .filter(enrollment -> enrollment.getStatus() != null
                                    && enrollment.getStatus() == CourseEnrollment.STATUS_ENROLLED)
                            .switchIfEmpty(Mono.error(new BizException(BizErrorCode.ACCESS_DENIED, "无权访问课程空间")))
                            .then();
                });
    }

    private Mono<Void> requireSpaceWritable(Integer spaceType, Long courseId, AuthUser user) {
        AuthUser actor = requireUser(user);
        if (isAdmin(actor)) {
            return Mono.empty();
        }
        if (spaceType == DriveNode.SPACE_PERSONAL) {
            return Mono.empty();
        }
        return courseService.requireCourseEditable(courseId, actor).then();
    }

    private static Target targetOf(Long parentId, Integer spaceType, Long courseId) {
        return new Target(parentId == null ? ROOT_PARENT : parentId, spaceType, courseId);
    }

    private static AuthUser requireUser(AuthUser user) {
        if (user == null) {
            throw unauthenticated();
        }
        return user;
    }

    private static BizException unauthenticated() {
        return new BizException(BizErrorCode.UNAUTHENTICATED, "未登录");
    }

    private static boolean isAdmin(AuthUser user) {
        return user != null && user.hasRole(ROLE_ADMIN);
    }

    private static DriveShare incrementLocalView(DriveShare share) {
        share.setViewCount((share.getViewCount() == null ? 0 : share.getViewCount()) + 1);
        return share;
    }

    private static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(TOKEN_ALPHABET.charAt(RANDOM.nextInt(TOKEN_ALPHABET.length())));
        }
        return sb.toString();
    }

    private record Target(Long parentId, Integer spaceType, Long courseId) {
    }

    private record NodeFilePair(DriveNode node, DriveFile file) {
    }
}
