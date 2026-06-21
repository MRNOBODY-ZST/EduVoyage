package cn.edu.shmtu.eduvoyage.drive;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.dto.CourseRequest;
import cn.edu.shmtu.eduvoyage.course.dto.CourseResponse;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.course.service.EnrollmentService;
import cn.edu.shmtu.eduvoyage.drive.domain.DriveNode;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.DirectoryCreateRequest;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.DriveNodeResponse;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.InstantUploadRequest;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.ShareAccessRequest;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.ShareCreateRequest;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.ShareResponse;
import cn.edu.shmtu.eduvoyage.drive.service.DriveService;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end drive-module test against MySQL + MinIO. Auto-skips without Docker.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers(disabledWithoutDocker = true)
class DriveModuleIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.0")
            .withDatabaseName("eduvoyage")
            .withUsername("eduvoyage")
            .withPassword("eduvoyage");

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @Container
    static GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z"))
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server /data --address :9000")
            .withExposedPorts(9000);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.elasticsearch.repositories.enabled", () -> "false");
        registry.add("spring.elasticsearch.uris", () -> "http://localhost:9200");
        registry.add("eduvoyage.storage.minio.endpoint",
                () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("eduvoyage.storage.minio.access-key", () -> "minioadmin");
        registry.add("eduvoyage.storage.minio.secret-key", () -> "minioadmin");
        registry.add("eduvoyage.storage.minio.bucket", () -> "eduvoyage");
    }

    @Autowired DriveService driveService;
    @Autowired CourseService courseService;
    @Autowired EnrollmentService enrollmentService;

    private static final AuthUser TEACHER = new AuthUser(2L, "teacher", Set.of("TEACHER"),
            Set.of("course:create", "course:update", "drive:read", "drive:write"));
    private static final AuthUser STUDENT = new AuthUser(3L, "student", Set.of("STUDENT"),
            Set.of("course:enroll", "drive:read", "drive:write"));

    @Test
    void uploadDedupShareQuotaAndCourseSpaceFlow() throws Exception {
        byte[] bytes = "hello drive".getBytes();
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));

        DriveNodeResponse dir = driveService.createDirectory(new DirectoryCreateRequest("资料", 0L,
                DriveNode.SPACE_PERSONAL, null), STUDENT).block();
        assertThat(dir).isNotNull();

        DriveNodeResponse uploaded = driveService.upload(new TestFilePart("note.txt", bytes),
                dir.id(), null, null, STUDENT).block();
        assertThat(uploaded).isNotNull();
        assertThat(uploaded.sha256()).isEqualTo(sha);
        assertThat(uploaded.size()).isEqualTo((long) bytes.length);

        StepVerifier.create(driveService.quota(STUDENT))
                .assertNext(q -> assertThat(q.usedBytes()).isEqualTo((long) bytes.length))
                .verifyComplete();

        DriveNodeResponse copied = driveService.instantUpload(new InstantUploadRequest("note-copy.txt",
                dir.id(), null, null, sha, (long) bytes.length, "text/plain"), STUDENT).block();
        assertThat(copied).isNotNull();
        assertThat(copied.fileId()).isEqualTo(uploaded.fileId());

        StepVerifier.create(driveService.quota(STUDENT))
                .assertNext(q -> assertThat(q.usedBytes()).isEqualTo((long) bytes.length * 2))
                .verifyComplete();

        ShareResponse share = driveService.createShare(new ShareCreateRequest(uploaded.id(), "AB12",
                LocalDateTime.now().plusDays(1)), STUDENT).block();
        assertThat(share).isNotNull();

        StepVerifier.create(driveService.accessShare(share.token(), new ShareAccessRequest("BAD")))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.SHARE_CODE_INVALID))
                .verify();

        StepVerifier.create(driveService.accessShare(share.token(), new ShareAccessRequest("AB12")))
                .assertNext(view -> {
                    assertThat(view.url()).startsWith("http");
                    assertThat(view.share().viewCount()).isEqualTo(1);
                })
                .verifyComplete();

        CourseResponse course = courseService.create(new CourseRequest("网盘课程", null, "intro",
                new BigDecimal("2.0"), Course.VISIBILITY_PUBLIC, null, null, null), TEACHER.id()).block();
        assertThat(course).isNotNull();
        Long courseId = course.id();

        DriveNodeResponse courseFile = driveService.upload(new TestFilePart("course.txt", bytes),
                0L, DriveNode.SPACE_COURSE, courseId, TEACHER).block();
        assertThat(courseFile).isNotNull();

        StepVerifier.create(driveService.list(0L, DriveNode.SPACE_COURSE, courseId, STUDENT))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.ACCESS_DENIED))
                .verify();

        courseService.publish(courseId, TEACHER).block();
        enrollmentService.enroll(courseId, STUDENT.id()).block();

        StepVerifier.create(driveService.list(0L, DriveNode.SPACE_COURSE, courseId, STUDENT))
                .assertNext(rows -> assertThat(rows).extracting(DriveNodeResponse::name).contains("course.txt"))
                .verifyComplete();

        driveService.delete(dir.id(), STUDENT).block();
        StepVerifier.create(driveService.quota(STUDENT))
                .assertNext(q -> assertThat(q.usedBytes()).isZero())
                .verifyComplete();
    }

    private record TestFilePart(String filename, byte[] bytes) implements FilePart {

        @Override
        public String name() {
            return "file";
        }

        @Override
        public HttpHeaders headers() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return headers;
        }

        @Override
        public Flux<DataBuffer> content() {
            return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
        }

        @Override
        public Mono<Void> transferTo(Path dest) {
            return DataBufferUtils.write(content(), dest);
        }
    }
}
