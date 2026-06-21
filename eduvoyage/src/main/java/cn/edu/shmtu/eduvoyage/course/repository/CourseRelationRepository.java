package cn.edu.shmtu.eduvoyage.course.repository;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Hand-written access to the course's composite-key join tables
 * ({@code course_teacher}, {@code course_class_scope}, {@code course_favorite})
 * via {@link DatabaseClient} — full control over batch inserts and {@code IN}
 * sets while staying reactive.
 */
@Repository
public class CourseRelationRepository {

    private final DatabaseClient db;

    public CourseRelationRepository(DatabaseClient db) {
        this.db = db;
    }

    // ----------------------------------------------------- course_teacher

    /** Teacher ids assisting on a course (excludes the owning teacher unless added). */
    public Flux<Long> findTeacherIds(Long courseId) {
        return db.sql("SELECT teacher_id FROM course_teacher WHERE course_id = :cid")
                .bind("cid", courseId)
                .map((row, meta) -> row.get("teacher_id", Long.class))
                .all();
    }

    public Mono<Void> addTeacher(Long courseId, Long teacherId, int role) {
        return db.sql("""
                        INSERT IGNORE INTO course_teacher (course_id, teacher_id, role)
                        VALUES (:cid, :tid, :role)
                        """)
                .bind("cid", courseId)
                .bind("tid", teacherId)
                .bind("role", role)
                .then();
    }

    public Mono<Void> deleteTeachers(Long courseId) {
        return db.sql("DELETE FROM course_teacher WHERE course_id = :cid")
                .bind("cid", courseId)
                .then();
    }

    /** True if the user owns the course or is listed as a co-teacher. */
    public Mono<Boolean> isCourseTeacher(Long courseId, Long userId) {
        return db.sql("""
                        SELECT (
                          EXISTS(SELECT 1 FROM course WHERE id = :cid AND teacher_id = :uid AND deleted = 0)
                          OR EXISTS(SELECT 1 FROM course_teacher WHERE course_id = :cid AND teacher_id = :uid)
                        ) AS allowed
                        """)
                .bind("cid", courseId)
                .bind("uid", userId)
                .map((row, meta) -> {
                    Number n = row.get("allowed", Number.class);
                    return n != null && n.intValue() == 1;
                })
                .one()
                .defaultIfEmpty(false);
    }

    // ------------------------------------------------- course_class_scope

    public Flux<Long> findScopeClassIds(Long courseId) {
        return db.sql("SELECT class_id FROM course_class_scope WHERE course_id = :cid")
                .bind("cid", courseId)
                .map((row, meta) -> row.get("class_id", Long.class))
                .all();
    }

    public Mono<Void> deleteScope(Long courseId) {
        return db.sql("DELETE FROM course_class_scope WHERE course_id = :cid")
                .bind("cid", courseId)
                .then();
    }

    public Mono<Void> addScopeClass(Long courseId, Long classId) {
        return db.sql("""
                        INSERT IGNORE INTO course_class_scope (course_id, class_id)
                        VALUES (:cid, :clsId)
                        """)
                .bind("cid", courseId)
                .bind("clsId", classId)
                .then();
    }

    /** Replaces a course's class-scope set in one logical operation. */
    public Mono<Void> replaceScope(Long courseId, Collection<Long> classIds) {
        Mono<Void> clear = deleteScope(courseId);
        if (classIds == null || classIds.isEmpty()) {
            return clear;
        }
        return clear.thenMany(Flux.fromIterable(classIds)
                        .flatMap(cid -> addScopeClass(courseId, cid)))
                .then();
    }

    // --------------------------------------------------- course_favorite

    public Mono<Void> addFavorite(Long studentId, Long courseId) {
        return db.sql("""
                        INSERT IGNORE INTO course_favorite (student_id, course_id)
                        VALUES (:sid, :cid)
                        """)
                .bind("sid", studentId)
                .bind("cid", courseId)
                .then();
    }

    public Mono<Void> removeFavorite(Long studentId, Long courseId) {
        return db.sql("DELETE FROM course_favorite WHERE student_id = :sid AND course_id = :cid")
                .bind("sid", studentId)
                .bind("cid", courseId)
                .then();
    }

    public Mono<Boolean> isFavorite(Long studentId, Long courseId) {
        return db.sql("SELECT 1 FROM course_favorite WHERE student_id = :sid AND course_id = :cid")
                .bind("sid", studentId)
                .bind("cid", courseId)
                .map((row, meta) -> true)
                .one()
                .defaultIfEmpty(false);
    }
}
