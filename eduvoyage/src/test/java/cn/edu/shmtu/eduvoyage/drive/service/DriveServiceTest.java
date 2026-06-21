package cn.edu.shmtu.eduvoyage.drive.service;

import cn.edu.shmtu.eduvoyage.course.repository.CourseEnrollmentRepository;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.drive.domain.DriveFile;
import cn.edu.shmtu.eduvoyage.drive.domain.DriveNode;
import cn.edu.shmtu.eduvoyage.drive.domain.DriveQuota;
import cn.edu.shmtu.eduvoyage.drive.domain.DriveShare;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.InstantUploadRequest;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.ShareAccessRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation.ReactiveInsert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriveServiceTest {

    @Mock DriveNodeRepository nodeRepository;
    @Mock DriveFileRepository fileRepository;
    @Mock DriveShareRepository shareRepository;
    @Mock DriveQuotaRepository quotaRepository;
    @Mock DriveQueryRepository queryRepository;
    @Mock CourseService courseService;
    @Mock CourseEnrollmentRepository enrollmentRepository;
    @Mock MinioStorageService storageService;
    @Mock DriveUploadReader uploadReader;
    @Mock R2dbcEntityTemplate entityTemplate;
    @Mock ReactiveInsert<DriveNode> nodeInsert;

    private DriveService service;

    private static final AuthUser STUDENT = new AuthUser(3L, "student",
            Set.of("STUDENT"), Set.of("drive:read", "drive:write"));
    private static final String SHA = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @BeforeEach
    void setUp() {
        service = new DriveService(nodeRepository, fileRepository, shareRepository, quotaRepository, queryRepository,
                courseService, enrollmentRepository, storageService, uploadReader, entityTemplate, new IdGenerator(1L),
                Clock.fixed(Instant.parse("2026-06-21T00:00:00Z"), ZoneId.of("UTC")), properties());
    }

    @Test
    void instantUploadReservesQuotaAndIncrementsRef() {
        DriveFile physical = DriveFile.builder()
                .id(10L)
                .sha256(SHA)
                .size(5L)
                .mime("text/plain")
                .bucket("eduvoyage")
                .objectKey(DriveRules.objectKey(SHA))
                .refCount(1)
                .build();
        when(quotaRepository.findById(STUDENT.id()))
                .thenReturn(Mono.just(DriveQuota.builder().userId(STUDENT.id()).totalBytes(100L).usedBytes(0L).build()));
        when(queryRepository.personalRootNameExists(eq(STUDENT.id()), eq("copy.txt"), isNull()))
                .thenReturn(Mono.just(false));
        when(queryRepository.reserveQuota(STUDENT.id(), 5L)).thenReturn(Mono.just(true));
        when(fileRepository.findBySha256(SHA)).thenReturn(Mono.just(physical));
        when(queryRepository.incrementFileRef(10L)).thenReturn(Mono.empty());
        when(entityTemplate.insert(DriveNode.class)).thenReturn(nodeInsert);
        when(nodeInsert.using(any(DriveNode.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(fileRepository.findById(10L)).thenReturn(Mono.just(physical));

        InstantUploadRequest req = new InstantUploadRequest("copy.txt", 0L, DriveNode.SPACE_PERSONAL,
                null, SHA, 5L, "text/plain");

        StepVerifier.create(service.instantUpload(req, STUDENT))
                .assertNext(resp -> {
                    assertThat(resp.name()).isEqualTo("copy.txt");
                    assertThat(resp.fileId()).isEqualTo(10L);
                    assertThat(resp.size()).isEqualTo(5L);
                })
                .verifyComplete();
    }

    @Test
    void instantUploadRejectsQuotaExceededBeforeRefIncrement() {
        when(quotaRepository.findById(STUDENT.id()))
                .thenReturn(Mono.just(DriveQuota.builder().userId(STUDENT.id()).totalBytes(4L).usedBytes(4L).build()));
        when(queryRepository.personalRootNameExists(eq(STUDENT.id()), eq("too-big.txt"), isNull()))
                .thenReturn(Mono.just(false));
        when(queryRepository.reserveQuota(STUDENT.id(), 5L)).thenReturn(Mono.just(false));

        InstantUploadRequest req = new InstantUploadRequest("too-big.txt", 0L, DriveNode.SPACE_PERSONAL,
                null, SHA, 5L, "text/plain");

        StepVerifier.create(service.instantUpload(req, STUDENT))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.QUOTA_EXCEEDED))
                .verify();
        verify(queryRepository, never()).incrementFileRef(any());
    }

    @Test
    void shareAccessRejectsExpiredShare() {
        DriveShare share = DriveShare.builder()
                .id(1L)
                .nodeId(2L)
                .ownerId(STUDENT.id())
                .token("token")
                .extractCode("AB12")
                .expireAt(LocalDateTime.of(2026, 6, 20, 23, 59))
                .viewCount(0)
                .deleted(0)
                .build();
        when(shareRepository.findActiveByToken("token")).thenReturn(Mono.just(share));

        StepVerifier.create(service.accessShare("token", new ShareAccessRequest("AB12")))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.SHARE_EXPIRED))
                .verify();
    }

    @Test
    void listingPersonalRootRequiresAuthenticatedUser() {
        StepVerifier.create(service.list(0L, DriveNode.SPACE_PERSONAL, null, null))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.UNAUTHENTICATED))
                .verify();
    }

    private static EduVoyageProperties properties() {
        return new EduVoyageProperties(
                null,
                null,
                new EduVoyageProperties.Storage(
                        new EduVoyageProperties.Minio("http://localhost:9000", "minioadmin", "minioadmin",
                                "eduvoyage", Duration.ofHours(1)),
                        new EduVoyageProperties.Quota(2_147_483_648L, 10_737_418_240L, 53_687_091_200L)),
                null);
    }
}
