package cn.edu.shmtu.eduvoyage.course.service;

import cn.edu.shmtu.eduvoyage.course.domain.Courseware;
import cn.edu.shmtu.eduvoyage.course.dto.CoursewareDtos.CoursewareRequest;
import cn.edu.shmtu.eduvoyage.course.dto.CoursewareDtos.CoursewareResponse;
import cn.edu.shmtu.eduvoyage.course.repository.CoursewareRepository;
import cn.edu.shmtu.eduvoyage.course.repository.KnowledgeNodeRepository;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Courseware management under a knowledge node. Mutations require edit rights on
 * the node's owning course; the node→course link is resolved via the node lookup.
 */
@Service
public class CoursewareService {

    private final CoursewareRepository coursewareRepository;
    private final KnowledgeNodeRepository nodeRepository;
    private final CourseService courseService;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;

    public CoursewareService(CoursewareRepository coursewareRepository,
                             KnowledgeNodeRepository nodeRepository,
                             CourseService courseService,
                             R2dbcEntityTemplate entityTemplate,
                             IdGenerator idGenerator) {
        this.coursewareRepository = coursewareRepository;
        this.nodeRepository = nodeRepository;
        this.courseService = courseService;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
    }

    public Mono<List<CoursewareResponse>> listByNode(Long nodeId) {
        return coursewareRepository.findByNodeId(nodeId).map(CoursewareResponse::from).collectList();
    }

    @Transactional
    public Mono<CoursewareResponse> create(Long nodeId, CoursewareRequest req, AuthUser editor) {
        return requireEditableNodeCourse(nodeId, editor)
                .then(Mono.defer(() -> {
                    Courseware cw = Courseware.builder()
                            .id(idGenerator.nextId())
                            .nodeId(nodeId)
                            .title(req.title())
                            .type(req.type())
                            .contentRef(req.contentRef())
                            .fileId(req.fileId())
                            .durationSec(req.durationSec())
                            .sortNo(req.sortNo() == null ? 0 : req.sortNo())
                            .deleted(0)
                            .build();
                    return entityTemplate.insert(Courseware.class).using(cw);
                }))
                .map(CoursewareResponse::from);
    }

    @Transactional
    public Mono<CoursewareResponse> update(Long id, CoursewareRequest req, AuthUser editor) {
        return requireCourseware(id)
                .flatMap(cw -> requireEditableNodeCourse(cw.getNodeId(), editor).thenReturn(cw))
                .flatMap(cw -> {
                    cw.setTitle(req.title());
                    cw.setType(req.type());
                    cw.setContentRef(req.contentRef());
                    cw.setFileId(req.fileId());
                    cw.setDurationSec(req.durationSec());
                    if (req.sortNo() != null) {
                        cw.setSortNo(req.sortNo());
                    }
                    return coursewareRepository.save(cw);
                })
                .map(CoursewareResponse::from);
    }

    @Transactional
    public Mono<Void> delete(Long id, AuthUser editor) {
        return requireCourseware(id)
                .flatMap(cw -> requireEditableNodeCourse(cw.getNodeId(), editor).thenReturn(cw))
                .flatMap(cw -> {
                    cw.setDeleted(1);
                    return coursewareRepository.save(cw);
                })
                .then();
    }

    // ------------------------------------------------------------ helpers

    private Mono<Courseware> requireCourseware(Long id) {
        return coursewareRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "课件不存在")));
    }

    /** Resolves the node's course and asserts the editor may modify it. */
    private Mono<Void> requireEditableNodeCourse(Long nodeId, AuthUser editor) {
        return nodeRepository.findActiveById(nodeId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.GRAPH_NODE_NOT_FOUND)))
                .flatMap(node -> courseService.requireCourseEditable(node.getCourseId(), editor))
                .then();
    }
}
