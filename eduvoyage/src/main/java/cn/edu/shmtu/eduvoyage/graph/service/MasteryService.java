package cn.edu.shmtu.eduvoyage.graph.service;

import cn.edu.shmtu.eduvoyage.course.repository.KnowledgeNodeRepository;
import cn.edu.shmtu.eduvoyage.graph.domain.KnowledgeMastery;
import cn.edu.shmtu.eduvoyage.graph.dto.MasteryDtos.MasteryRequest;
import cn.edu.shmtu.eduvoyage.graph.dto.MasteryDtos.MasteryResponse;
import cn.edu.shmtu.eduvoyage.graph.repository.KnowledgeMasteryRepository;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Per-student mastery tracking over knowledge nodes. Each {@code (student, node)}
 * pairing is upserted in place (the unique key guarantees one row), so reporting
 * progress repeatedly just updates the existing record. These rows feed
 * {@link KnowledgeGraphService} learning-path recommendation.
 */
@Service
public class MasteryService {

    private final KnowledgeMasteryRepository masteryRepository;
    private final KnowledgeNodeRepository nodeRepository;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;

    public MasteryService(KnowledgeMasteryRepository masteryRepository,
                          KnowledgeNodeRepository nodeRepository,
                          R2dbcEntityTemplate entityTemplate,
                          IdGenerator idGenerator) {
        this.masteryRepository = masteryRepository;
        this.nodeRepository = nodeRepository;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
    }

    /** Upserts a student's mastery of a single node. */
    @Transactional
    public Mono<MasteryResponse> report(Long nodeId, MasteryRequest req, Long studentId) {
        return requireNode(nodeId)
                .then(masteryRepository.findByStudentAndNode(studentId, nodeId))
                .flatMap(existing -> {
                    existing.setMasteryLevel(req.masteryLevel());
                    existing.setScore(req.score() == null ? existing.getScore() : req.score());
                    existing.setLearnProgress(
                            req.learnProgress() == null ? existing.getLearnProgress() : req.learnProgress());
                    return masteryRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    KnowledgeMastery mastery = KnowledgeMastery.builder()
                            .id(idGenerator.nextId())
                            .studentId(studentId)
                            .nodeId(nodeId)
                            .masteryLevel(req.masteryLevel())
                            .score(req.score() == null ? BigDecimal.ZERO : req.score())
                            .learnProgress(req.learnProgress() == null ? BigDecimal.ZERO : req.learnProgress())
                            .deleted(0)
                            .build();
                    return entityTemplate.insert(KnowledgeMastery.class).using(mastery);
                }))
                .map(MasteryResponse::from);
    }

    public Mono<MasteryResponse> get(Long nodeId, Long studentId) {
        return masteryRepository.findByStudentAndNode(studentId, nodeId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "暂无掌握度记录")))
                .map(MasteryResponse::from);
    }

    /** A student's mastery rows across an entire graph (one course). */
    public Mono<List<MasteryResponse>> listByGraph(Long graphId, Long studentId) {
        return masteryRepository.findByStudentAndGraph(studentId, graphId)
                .map(MasteryResponse::from)
                .collectList();
    }

    private Mono<Void> requireNode(Long nodeId) {
        return nodeRepository.findActiveById(nodeId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.GRAPH_NODE_NOT_FOUND)))
                .then();
    }
}
