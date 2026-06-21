package cn.edu.shmtu.eduvoyage.graph.service;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeGraph;
import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeNode;
import cn.edu.shmtu.eduvoyage.course.repository.KnowledgeGraphRepository;
import cn.edu.shmtu.eduvoyage.course.repository.KnowledgeNodeRepository;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.graph.domain.KnowledgeEdge;
import cn.edu.shmtu.eduvoyage.graph.dto.EdgeDtos.EdgeRequest;
import cn.edu.shmtu.eduvoyage.graph.repository.KnowledgeEdgeRepository;
import cn.edu.shmtu.eduvoyage.graph.repository.KnowledgeMasteryRepository;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link KnowledgeGraphService} edge-authoring guards (duplicate,
 * cycle, self-loop, ownership) with mocked collaborators. The graph algorithms
 * themselves are covered separately in {@code KnowledgeGraphModelTest}; here we
 * assert the service wires validation around them correctly.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {

    @Mock KnowledgeGraphRepository graphRepository;
    @Mock KnowledgeNodeRepository nodeRepository;
    @Mock KnowledgeEdgeRepository edgeRepository;
    @Mock KnowledgeMasteryRepository masteryRepository;
    @Mock CourseService courseService;
    @Mock R2dbcEntityTemplate entityTemplate;
    @Mock ReactiveInsert<KnowledgeEdge> edgeInsert;

    private final IdGenerator idGenerator = new IdGenerator(1L);
    private KnowledgeGraphService service;

    private static final long COURSE_ID = 900L;
    private static final long GRAPH_ID = 10L;
    private static final long NODE_A = 1L;
    private static final long NODE_B = 2L;

    private static final AuthUser TEACHER =
            new AuthUser(200L, "t", Set.of("TEACHER"), Set.of("graph:edit"));

    @BeforeEach
    void setUp() {
        service = new KnowledgeGraphService(graphRepository, nodeRepository, edgeRepository,
                masteryRepository, courseService, entityTemplate, idGenerator);
    }

    private KnowledgeGraph graph() {
        return KnowledgeGraph.builder().id(GRAPH_ID).courseId(COURSE_ID).name("默认图谱")
                .version(1).deleted(0).build();
    }

    private KnowledgeNode node(long id) {
        return KnowledgeNode.builder().id(id).courseId(COURSE_ID).graphId(GRAPH_ID)
                .name("n" + id).deleted(0).build();
    }

    private Course course() {
        return Course.builder().id(COURSE_ID).title("c").teacherId(TEACHER.id())
                .status(Course.STATUS_DRAFT).deleted(0).build();
    }

    @Test
    void addPrerequisiteEdgeSucceedsWhenAcyclic() {
        when(courseService.requireCourseEditable(COURSE_ID, TEACHER)).thenReturn(Mono.just(course()));
        when(graphRepository.findByCourseId(COURSE_ID)).thenReturn(Mono.just(graph()));
        when(nodeRepository.findActiveById(NODE_A)).thenReturn(Mono.just(node(NODE_A)));
        when(nodeRepository.findActiveById(NODE_B)).thenReturn(Mono.just(node(NODE_B)));
        when(edgeRepository.findActiveEdge(NODE_A, NODE_B, KnowledgeEdge.TYPE_PREREQUISITE))
                .thenReturn(Mono.empty());
        // cycle model: both nodes, no existing prerequisite edges
        when(nodeRepository.findByGraphId(GRAPH_ID)).thenReturn(Flux.just(node(NODE_A), node(NODE_B)));
        when(edgeRepository.findByGraphIdAndType(GRAPH_ID, KnowledgeEdge.TYPE_PREREQUISITE))
                .thenReturn(Flux.empty());
        when(entityTemplate.insert(KnowledgeEdge.class)).thenReturn(edgeInsert);
        when(edgeInsert.using(any(KnowledgeEdge.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.addEdge(COURSE_ID,
                        new EdgeRequest(NODE_A, NODE_B, KnowledgeEdge.TYPE_PREREQUISITE, null), TEACHER))
                .assertNext(e -> {
                    assertThat(e.fromId()).isEqualTo(NODE_A);
                    assertThat(e.toId()).isEqualTo(NODE_B);
                    assertThat(e.weight()).isEqualTo(1.0);
                })
                .verifyComplete();
    }

    @Test
    void selfLoopIsRejectedAsCycle() {
        StepVerifier.create(service.addEdge(COURSE_ID,
                        new EdgeRequest(NODE_A, NODE_A, KnowledgeEdge.TYPE_PREREQUISITE, null), TEACHER))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.GRAPH_CYCLE))
                .verify();
    }

    @Test
    void duplicateEdgeIsRejected() {
        when(courseService.requireCourseEditable(COURSE_ID, TEACHER)).thenReturn(Mono.just(course()));
        when(graphRepository.findByCourseId(COURSE_ID)).thenReturn(Mono.just(graph()));
        when(nodeRepository.findActiveById(NODE_A)).thenReturn(Mono.just(node(NODE_A)));
        when(nodeRepository.findActiveById(NODE_B)).thenReturn(Mono.just(node(NODE_B)));
        when(edgeRepository.findActiveEdge(NODE_A, NODE_B, KnowledgeEdge.TYPE_PREREQUISITE))
                .thenReturn(Mono.just(KnowledgeEdge.builder().id(5L).graphId(GRAPH_ID)
                        .fromId(NODE_A).toId(NODE_B).type(KnowledgeEdge.TYPE_PREREQUISITE).build()));

        StepVerifier.create(service.addEdge(COURSE_ID,
                        new EdgeRequest(NODE_A, NODE_B, KnowledgeEdge.TYPE_PREREQUISITE, null), TEACHER))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.GRAPH_EDGE_EXISTS))
                .verify();
    }

    @Test
    void backEdgeThatClosesCycleIsRejected() {
        // existing A→B prerequisite; adding B→A must be refused
        when(courseService.requireCourseEditable(COURSE_ID, TEACHER)).thenReturn(Mono.just(course()));
        when(graphRepository.findByCourseId(COURSE_ID)).thenReturn(Mono.just(graph()));
        when(nodeRepository.findActiveById(NODE_A)).thenReturn(Mono.just(node(NODE_A)));
        when(nodeRepository.findActiveById(NODE_B)).thenReturn(Mono.just(node(NODE_B)));
        when(edgeRepository.findActiveEdge(NODE_B, NODE_A, KnowledgeEdge.TYPE_PREREQUISITE))
                .thenReturn(Mono.empty());
        when(nodeRepository.findByGraphId(GRAPH_ID)).thenReturn(Flux.just(node(NODE_A), node(NODE_B)));
        when(edgeRepository.findByGraphIdAndType(GRAPH_ID, KnowledgeEdge.TYPE_PREREQUISITE))
                .thenReturn(Flux.just(KnowledgeEdge.builder().id(5L).graphId(GRAPH_ID)
                        .fromId(NODE_A).toId(NODE_B).type(KnowledgeEdge.TYPE_PREREQUISITE).build()));

        StepVerifier.create(service.addEdge(COURSE_ID,
                        new EdgeRequest(NODE_B, NODE_A, KnowledgeEdge.TYPE_PREREQUISITE, null), TEACHER))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.GRAPH_CYCLE))
                .verify();
    }

    @Test
    void relatedEdgeSkipsCycleCheck() {
        // a RELATED edge B→A is allowed even though A→B prerequisite exists
        when(courseService.requireCourseEditable(COURSE_ID, TEACHER)).thenReturn(Mono.just(course()));
        when(graphRepository.findByCourseId(COURSE_ID)).thenReturn(Mono.just(graph()));
        when(nodeRepository.findActiveById(NODE_A)).thenReturn(Mono.just(node(NODE_A)));
        when(nodeRepository.findActiveById(NODE_B)).thenReturn(Mono.just(node(NODE_B)));
        when(edgeRepository.findActiveEdge(NODE_B, NODE_A, KnowledgeEdge.TYPE_RELATED))
                .thenReturn(Mono.empty());
        // cycle model must NOT be consulted for a RELATED edge
        lenient().when(nodeRepository.findByGraphId(GRAPH_ID))
                .thenReturn(Flux.just(node(NODE_A), node(NODE_B)));
        lenient().when(edgeRepository.findByGraphIdAndType(eq(GRAPH_ID), any()))
                .thenReturn(Flux.empty());
        when(entityTemplate.insert(KnowledgeEdge.class)).thenReturn(edgeInsert);
        when(edgeInsert.using(any(KnowledgeEdge.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.addEdge(COURSE_ID,
                        new EdgeRequest(NODE_B, NODE_A, KnowledgeEdge.TYPE_RELATED, 3.0), TEACHER))
                .assertNext(e -> {
                    assertThat(e.type()).isEqualTo(KnowledgeEdge.TYPE_RELATED);
                    assertThat(e.weight()).isEqualTo(3.0);
                })
                .verifyComplete();
    }

    @Test
    void edgeAcrossDifferentGraphNodeIsRejected() {
        when(courseService.requireCourseEditable(COURSE_ID, TEACHER)).thenReturn(Mono.just(course()));
        when(graphRepository.findByCourseId(COURSE_ID)).thenReturn(Mono.just(graph()));
        when(nodeRepository.findActiveById(NODE_A)).thenReturn(Mono.just(node(NODE_A)));
        // NODE_B belongs to a different graph
        KnowledgeNode foreign = KnowledgeNode.builder().id(NODE_B).courseId(COURSE_ID)
                .graphId(999L).name("foreign").deleted(0).build();
        when(nodeRepository.findActiveById(NODE_B)).thenReturn(Mono.just(foreign));

        StepVerifier.create(service.addEdge(COURSE_ID,
                        new EdgeRequest(NODE_A, NODE_B, KnowledgeEdge.TYPE_PREREQUISITE, null), TEACHER))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.PARAM_INVALID))
                .verify();
    }
}
