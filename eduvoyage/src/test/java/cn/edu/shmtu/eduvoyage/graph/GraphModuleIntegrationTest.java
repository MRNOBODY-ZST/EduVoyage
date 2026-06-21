package cn.edu.shmtu.eduvoyage.graph;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.dto.CourseRequest;
import cn.edu.shmtu.eduvoyage.course.dto.CourseResponse;
import cn.edu.shmtu.eduvoyage.course.dto.KnowledgeNodeDtos.NodeRequest;
import cn.edu.shmtu.eduvoyage.course.dto.KnowledgeNodeDtos.NodeResponse;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.course.service.KnowledgeNodeService;
import cn.edu.shmtu.eduvoyage.graph.domain.KnowledgeEdge;
import cn.edu.shmtu.eduvoyage.graph.domain.KnowledgeMastery;
import cn.edu.shmtu.eduvoyage.graph.dto.EdgeDtos.EdgeRequest;
import cn.edu.shmtu.eduvoyage.graph.dto.MasteryDtos.MasteryRequest;
import cn.edu.shmtu.eduvoyage.graph.service.KnowledgeGraphService;
import cn.edu.shmtu.eduvoyage.graph.service.MasteryService;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end knowledge-graph test against real MySQL (schema.sql + data.sql via
 * the dev profile). Builds a small prerequisite graph, asserts the cycle and
 * duplicate guards, then drives the analysis endpoints (topological order,
 * prerequisite chain) and the per-student learning path as mastery is reported.
 *
 * <p>Auto-skips when no Docker daemon is reachable.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers(disabledWithoutDocker = true)
class GraphModuleIntegrationTest {

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

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.elasticsearch.repositories.enabled", () -> "false");
        registry.add("spring.elasticsearch.uris", () -> "http://localhost:9200");
    }

    @Autowired CourseService courseService;
    @Autowired KnowledgeNodeService nodeService;
    @Autowired KnowledgeGraphService graphService;
    @Autowired MasteryService masteryService;

    // seeded accounts (data.sql): teacher id=2, student id=3
    private static final AuthUser TEACHER = new AuthUser(2L, "teacher",
            Set.of("TEACHER"), Set.of("course:create", "course:update", "graph:edit", "graph:read"));
    private static final AuthUser OTHER_TEACHER = new AuthUser(999L, "ghost",
            Set.of("TEACHER"), Set.of("graph:edit"));
    private static final long STUDENT_ID = 3L;

    @Test
    void buildGraphThenAnalyseAndRecommendPath() {
        // ---- arrange: a course with three knowledge points A, B, C ----
        CourseRequest courseReq = new CourseRequest("图谱集成课程", null, "intro",
                new BigDecimal("2.0"), Course.VISIBILITY_PUBLIC, null, null, null);
        CourseResponse course = courseService.create(courseReq, TEACHER.id()).block();
        assertThat(course).isNotNull();
        Long courseId = course.id();

        NodeResponse a = nodeService.create(courseId,
                new NodeRequest("基础A", null, null, null, 20, null, null), TEACHER).block();
        NodeResponse b = nodeService.create(courseId,
                new NodeRequest("进阶B", null, null, null, 30, null, null), TEACHER).block();
        NodeResponse c = nodeService.create(courseId,
                new NodeRequest("高级C", null, null, null, 40, null, null), TEACHER).block();
        assertThat(a).isNotNull();
        assertThat(b).isNotNull();
        assertThat(c).isNotNull();

        // ---- act + assert: link A → B → C as prerequisites ----
        StepVerifier.create(graphService.addEdge(courseId,
                        new EdgeRequest(a.id(), b.id(), KnowledgeEdge.TYPE_PREREQUISITE, null), TEACHER))
                .assertNext(e -> {
                    assertThat(e.fromId()).isEqualTo(a.id());
                    assertThat(e.toId()).isEqualTo(b.id());
                    assertThat(e.weight()).isEqualTo(1.0);
                })
                .verifyComplete();

        graphService.addEdge(courseId,
                new EdgeRequest(b.id(), c.id(), KnowledgeEdge.TYPE_PREREQUISITE, 2.0), TEACHER).block();

        // duplicate edge is rejected
        StepVerifier.create(graphService.addEdge(courseId,
                        new EdgeRequest(a.id(), b.id(), KnowledgeEdge.TYPE_PREREQUISITE, null), TEACHER))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.GRAPH_EDGE_EXISTS))
                .verify();

        // a back-edge C → A would create a cycle → rejected
        StepVerifier.create(graphService.addEdge(courseId,
                        new EdgeRequest(c.id(), a.id(), KnowledgeEdge.TYPE_PREREQUISITE, null), TEACHER))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.GRAPH_CYCLE))
                .verify();

        // a foreign teacher cannot author edges on this course
        StepVerifier.create(graphService.addEdge(courseId,
                        new EdgeRequest(a.id(), c.id(), KnowledgeEdge.TYPE_RELATED, null), OTHER_TEACHER))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.ACCESS_DENIED))
                .verify();

        // ---- analysis: topo order is A, B, C ----
        StepVerifier.create(graphService.topologicalOrder(courseId))
                .assertNext(order -> assertThat(order.stream().map(n -> n.id()).toList())
                        .containsExactly(a.id(), b.id(), c.id()))
                .verifyComplete();

        // prerequisite chain of C is [A, B]
        StepVerifier.create(graphService.prerequisiteChain(courseId, c.id()))
                .assertNext(chain -> {
                    assertThat(chain.targetId()).isEqualTo(c.id());
                    assertThat(chain.prerequisites().stream().map(n -> n.id()).toList())
                            .containsExactly(a.id(), b.id());
                })
                .verifyComplete();

        // ---- learning path: nothing mastered → only A is learnable ----
        StepVerifier.create(graphService.learningPath(courseId, STUDENT_ID))
                .assertNext(path -> {
                    assertThat(path.totalCount()).isEqualTo(3);
                    assertThat(path.masteredCount()).isZero();
                    assertThat(path.learnable().stream().map(n -> n.id()).toList())
                            .containsExactly(a.id());
                    assertThat(path.recommended().stream().map(n -> n.id()).toList())
                            .containsExactly(a.id(), b.id(), c.id());
                })
                .verifyComplete();

        // student masters A → B becomes learnable, recommended drops A
        StepVerifier.create(masteryService.report(a.id(),
                        new MasteryRequest(KnowledgeMastery.LEVEL_MASTERED, new BigDecimal("90.0"),
                                new BigDecimal("100.0")), STUDENT_ID))
                .assertNext(m -> assertThat(m.masteryLevel()).isEqualTo(KnowledgeMastery.LEVEL_MASTERED))
                .verifyComplete();

        StepVerifier.create(graphService.learningPath(courseId, STUDENT_ID))
                .assertNext(path -> {
                    assertThat(path.masteredCount()).isEqualTo(1);
                    assertThat(path.learnable().stream().map(n -> n.id()).toList())
                            .containsExactly(b.id());
                    assertThat(path.recommended().stream().map(n -> n.id()).toList())
                            .containsExactly(b.id(), c.id());
                })
                .verifyComplete();

        // reporting A again upserts in place (no duplicate row)
        masteryService.report(a.id(),
                new MasteryRequest(KnowledgeMastery.LEVEL_LEARNING, null, new BigDecimal("50.0")),
                STUDENT_ID).block();
        Long graphId = graphService.view(courseId).block().graphId();
        StepVerifier.create(masteryService.listByGraph(graphId, STUDENT_ID))
                .assertNext(list -> assertThat(list).hasSize(1))
                .verifyComplete();

        // deleting the A→B edge frees B's prerequisite; topo order still valid
        Long edgeId = graphService.listEdges(courseId).block().stream()
                .filter(e -> e.fromId().equals(a.id()) && e.toId().equals(b.id()))
                .findFirst().orElseThrow().id();
        graphService.deleteEdge(edgeId, TEACHER).block();
        StepVerifier.create(graphService.listEdges(courseId))
                .assertNext(edges -> assertThat(edges).hasSize(1))
                .verifyComplete();
    }

    private Long graphIdOf(Long courseId) {
        return graphService.view(courseId).block().graphId();
    }
}
