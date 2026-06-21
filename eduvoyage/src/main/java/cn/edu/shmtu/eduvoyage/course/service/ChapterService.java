package cn.edu.shmtu.eduvoyage.course.service;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.domain.CourseChapter;
import cn.edu.shmtu.eduvoyage.course.dto.ChapterDtos.ChapterNode;
import cn.edu.shmtu.eduvoyage.course.dto.ChapterDtos.ChapterRequest;
import cn.edu.shmtu.eduvoyage.course.repository.CourseChapterRepository;
import cn.edu.shmtu.eduvoyage.course.repository.KnowledgeNodeRepository;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chapter-tree management for a course. Chapters form a tree via {@code parentId}
 * ({@code 0} = root). All mutations require the caller to be able to edit the
 * owning course (delegated to {@link CourseService#requireCourseEditable}).
 */
@Service
public class ChapterService {

    private final CourseChapterRepository chapterRepository;
    private final KnowledgeNodeRepository nodeRepository;
    private final CourseService courseService;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;

    public ChapterService(CourseChapterRepository chapterRepository,
                          KnowledgeNodeRepository nodeRepository,
                          CourseService courseService,
                          R2dbcEntityTemplate entityTemplate,
                          IdGenerator idGenerator) {
        this.chapterRepository = chapterRepository;
        this.nodeRepository = nodeRepository;
        this.courseService = courseService;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
    }

    /** The course's chapters assembled into a nested tree (roots first, sorted). */
    public Mono<List<ChapterNode>> tree(Long courseId) {
        return chapterRepository.findByCourseId(courseId)
                .collectList()
                .map(ChapterService::assembleTree);
    }

    @Transactional
    public Mono<ChapterNode> create(Long courseId, ChapterRequest req, AuthUser editor) {
        long parentId = req.parentId() == null ? CourseChapter.ROOT_PARENT : req.parentId();
        return courseService.requireCourseEditable(courseId, editor)
                .then(validateParent(courseId, parentId))
                .then(Mono.defer(() -> {
                    CourseChapter chapter = CourseChapter.builder()
                            .id(idGenerator.nextId())
                            .courseId(courseId)
                            .parentId(parentId)
                            .title(req.title())
                            .sortNo(req.sortNo() == null ? 0 : req.sortNo())
                            .deleted(0)
                            .build();
                    return entityTemplate.insert(CourseChapter.class).using(chapter);
                }))
                .map(ChapterNode::of);
    }

    @Transactional
    public Mono<ChapterNode> update(Long chapterId, ChapterRequest req, AuthUser editor) {
        return requireChapter(chapterId)
                .flatMap(chapter -> courseService.requireCourseEditable(chapter.getCourseId(), editor)
                        .thenReturn(chapter))
                .flatMap(chapter -> {
                    long newParent = req.parentId() == null ? chapter.getParentId() : req.parentId();
                    if (newParent == chapter.getId()) {
                        return Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "章节不能以自身为父节点"));
                    }
                    Mono<Void> parentCheck = newParent == CourseChapter.ROOT_PARENT
                            ? Mono.empty()
                            : validateParent(chapter.getCourseId(), newParent);
                    return parentCheck.then(Mono.defer(() -> {
                        chapter.setTitle(req.title());
                        chapter.setParentId(newParent);
                        if (req.sortNo() != null) {
                            chapter.setSortNo(req.sortNo());
                        }
                        return chapterRepository.save(chapter);
                    }));
                })
                .map(ChapterNode::of);
    }

    /** Logical delete; refuses if the chapter still has sub-chapters or bound nodes. */
    @Transactional
    public Mono<Void> delete(Long chapterId, AuthUser editor) {
        return requireChapter(chapterId)
                .flatMap(chapter -> courseService.requireCourseEditable(chapter.getCourseId(), editor)
                        .thenReturn(chapter))
                .flatMap(chapter -> chapterRepository.countByParentId(chapterId)
                        .flatMap(childCount -> {
                            if (childCount > 0) {
                                return Mono.error(new BizException(
                                        BizErrorCode.OPERATION_NOT_ALLOWED, "请先删除子章节"));
                            }
                            return nodeRepository.countByChapterId(chapterId);
                        })
                        .flatMap(nodeCount -> {
                            if (nodeCount > 0) {
                                return Mono.error(new BizException(
                                        BizErrorCode.OPERATION_NOT_ALLOWED, "请先移除该章节下的知识点"));
                            }
                            chapter.setDeleted(1);
                            return chapterRepository.save(chapter);
                        }))
                .then();
    }

    // ------------------------------------------------------------ helpers

    private Mono<CourseChapter> requireChapter(Long chapterId) {
        return chapterRepository.findActiveById(chapterId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "章节不存在")));
    }

    /** A non-root parent must exist and belong to the same course. */
    private Mono<Void> validateParent(Long courseId, long parentId) {
        if (parentId == CourseChapter.ROOT_PARENT) {
            return Mono.empty();
        }
        return chapterRepository.findActiveById(parentId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "父章节不存在")))
                .flatMap(parent -> parent.getCourseId().equals(courseId)
                        ? Mono.empty()
                        : Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "父章节不属于该课程")));
    }

    private static List<ChapterNode> assembleTree(List<CourseChapter> chapters) {
        Map<Long, ChapterNode> byId = new LinkedHashMap<>();
        for (CourseChapter c : chapters) {
            byId.put(c.getId(), ChapterNode.of(c));
        }
        List<ChapterNode> roots = new ArrayList<>();
        for (CourseChapter c : chapters) {
            ChapterNode node = byId.get(c.getId());
            Long parentId = c.getParentId();
            ChapterNode parent = (parentId == null || parentId == CourseChapter.ROOT_PARENT)
                    ? null : byId.get(parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }
}
