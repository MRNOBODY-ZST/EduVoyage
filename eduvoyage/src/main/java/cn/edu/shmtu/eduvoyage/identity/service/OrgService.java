package cn.edu.shmtu.eduvoyage.identity.service;

import cn.edu.shmtu.eduvoyage.identity.domain.OrgClass;
import cn.edu.shmtu.eduvoyage.identity.domain.OrgDepartment;
import cn.edu.shmtu.eduvoyage.identity.domain.OrgMajor;
import cn.edu.shmtu.eduvoyage.identity.dto.OrgDtos.ClassRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.OrgDtos.ClassResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.OrgDtos.DepartmentRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.OrgDtos.DepartmentResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.OrgDtos.MajorRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.OrgDtos.MajorResponse;
import cn.edu.shmtu.eduvoyage.identity.repository.OrgClassRepository;
import cn.edu.shmtu.eduvoyage.identity.repository.OrgDepartmentRepository;
import cn.edu.shmtu.eduvoyage.identity.repository.OrgMajorRepository;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Organization tree management: 院系 (department) → 专业 (major) → 班级 (class).
 * Each level validates its parent exists before insert and uses logical delete.
 */
@Service
public class OrgService {

    private final OrgDepartmentRepository departmentRepository;
    private final OrgMajorRepository majorRepository;
    private final OrgClassRepository classRepository;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;

    public OrgService(OrgDepartmentRepository departmentRepository,
                      OrgMajorRepository majorRepository,
                      OrgClassRepository classRepository,
                      R2dbcEntityTemplate entityTemplate,
                      IdGenerator idGenerator) {
        this.departmentRepository = departmentRepository;
        this.majorRepository = majorRepository;
        this.classRepository = classRepository;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
    }

    // ---------------------------------------------------------- department

    public Flux<DepartmentResponse> listDepartments() {
        return departmentRepository.findAllActive().map(DepartmentResponse::from);
    }

    public Mono<DepartmentResponse> createDepartment(DepartmentRequest req) {
        OrgDepartment dept = OrgDepartment.builder()
                .id(idGenerator.nextId())
                .name(req.name())
                .code(req.code())
                .deleted(0)
                .build();
        return entityTemplate.insert(OrgDepartment.class).using(dept).map(DepartmentResponse::from);
    }

    public Mono<DepartmentResponse> updateDepartment(Long id, DepartmentRequest req) {
        return departmentRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "院系不存在")))
                .flatMap(dept -> {
                    dept.setName(req.name());
                    dept.setCode(req.code());
                    return departmentRepository.save(dept);
                })
                .map(DepartmentResponse::from);
    }

    public Mono<Void> deleteDepartment(Long id) {
        return majorRepository.findByDepartmentId(id).hasElements()
                .flatMap(hasChildren -> {
                    if (Boolean.TRUE.equals(hasChildren)) {
                        return Mono.error(new BizException(
                                BizErrorCode.OPERATION_NOT_ALLOWED, "请先删除该院系下的专业"));
                    }
                    return departmentRepository.findActiveById(id)
                            .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "院系不存在")))
                            .flatMap(dept -> {
                                dept.setDeleted(1);
                                return departmentRepository.save(dept);
                            })
                            .then();
                });
    }

    // --------------------------------------------------------------- major

    public Flux<MajorResponse> listMajors(Long departmentId) {
        Flux<OrgMajor> source = departmentId == null
                ? majorRepository.findAllActive()
                : majorRepository.findByDepartmentId(departmentId);
        return source.map(MajorResponse::from);
    }

    public Mono<MajorResponse> createMajor(MajorRequest req) {
        return departmentRepository.findActiveById(req.departmentId())
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "所属院系不存在")))
                .then(Mono.defer(() -> {
                    OrgMajor major = OrgMajor.builder()
                            .id(idGenerator.nextId())
                            .departmentId(req.departmentId())
                            .name(req.name())
                            .code(req.code())
                            .deleted(0)
                            .build();
                    return entityTemplate.insert(OrgMajor.class).using(major);
                }))
                .map(MajorResponse::from);
    }

    public Mono<MajorResponse> updateMajor(Long id, MajorRequest req) {
        return majorRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "专业不存在")))
                .flatMap(major -> {
                    major.setDepartmentId(req.departmentId());
                    major.setName(req.name());
                    major.setCode(req.code());
                    return majorRepository.save(major);
                })
                .map(MajorResponse::from);
    }

    public Mono<Void> deleteMajor(Long id) {
        return classRepository.findByMajorId(id).hasElements()
                .flatMap(hasChildren -> {
                    if (Boolean.TRUE.equals(hasChildren)) {
                        return Mono.error(new BizException(
                                BizErrorCode.OPERATION_NOT_ALLOWED, "请先删除该专业下的班级"));
                    }
                    return majorRepository.findActiveById(id)
                            .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "专业不存在")))
                            .flatMap(major -> {
                                major.setDeleted(1);
                                return majorRepository.save(major);
                            })
                            .then();
                });
    }

    // --------------------------------------------------------------- class

    public Flux<ClassResponse> listClasses(Long majorId) {
        Flux<OrgClass> source = majorId == null
                ? classRepository.findAllActive()
                : classRepository.findByMajorId(majorId);
        return source.map(ClassResponse::from);
    }

    public Mono<ClassResponse> createClass(ClassRequest req) {
        return majorRepository.findActiveById(req.majorId())
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "所属专业不存在")))
                .then(Mono.defer(() -> {
                    OrgClass clazz = OrgClass.builder()
                            .id(idGenerator.nextId())
                            .majorId(req.majorId())
                            .name(req.name())
                            .grade(req.grade())
                            .deleted(0)
                            .build();
                    return entityTemplate.insert(OrgClass.class).using(clazz);
                }))
                .map(ClassResponse::from);
    }

    public Mono<ClassResponse> updateClass(Long id, ClassRequest req) {
        return classRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "班级不存在")))
                .flatMap(clazz -> {
                    clazz.setMajorId(req.majorId());
                    clazz.setName(req.name());
                    clazz.setGrade(req.grade());
                    return classRepository.save(clazz);
                })
                .map(ClassResponse::from);
    }

    public Mono<Void> deleteClass(Long id) {
        return classRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "班级不存在")))
                .flatMap(clazz -> {
                    clazz.setDeleted(1);
                    return classRepository.save(clazz);
                })
                .then();
    }
}
