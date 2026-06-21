package cn.edu.shmtu.eduvoyage.analytics.repository;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cross-table MySQL aggregations used by dashboards.
 */
@Repository
public class AnalyticsQueryRepository {

    private final DatabaseClient db;

    public AnalyticsQueryRepository(DatabaseClient db) {
        this.db = db;
    }

    public Mono<Long> countEnrolledCourses(Long studentId) {
        return scalar("""
                SELECT COUNT(*) AS cnt FROM course_enrollment
                WHERE student_id = :studentId AND status = 1 AND deleted = 0
                """, spec -> spec.bind("studentId", studentId));
    }

    public Mono<Long> countTodoHomeworks(Long studentId) {
        return scalar("""
                SELECT COUNT(*) AS cnt
                FROM homework h
                JOIN course_enrollment ce ON ce.course_id = h.course_id
                WHERE ce.student_id = :studentId AND ce.status = 1 AND ce.deleted = 0
                  AND h.status = 1 AND h.deleted = 0
                  AND (h.deadline IS NULL OR h.deadline > NOW())
                  AND NOT EXISTS (
                    SELECT 1 FROM submission s
                    WHERE s.homework_id = h.id AND s.student_id = :studentId AND s.deleted = 0
                      AND s.status IN (1, 2)
                  )
                """, spec -> spec.bind("studentId", studentId));
    }

    public Mono<BigDecimal> averageStudentScore(Long studentId) {
        return decimal("""
                SELECT AVG(total_score) AS avg_score
                FROM submission
                WHERE student_id = :studentId AND status = 2 AND deleted = 0
                """, spec -> spec.bind("studentId", studentId));
    }

    public Mono<BigDecimal> masteryPercent(Long studentId) {
        return decimal("""
                SELECT AVG(learn_progress) AS avg_score
                FROM knowledge_mastery
                WHERE student_id = :studentId AND deleted = 0
                """, spec -> spec.bind("studentId", studentId));
    }

    public Flux<GradeTrendRow> gradeTrend(Long studentId, int limit) {
        return db.sql("""
                        SELECT h.id AS homework_id, h.title, s.total_score, s.submitted_at
                        FROM submission s
                        JOIN homework h ON h.id = s.homework_id
                        WHERE s.student_id = :studentId AND s.status = 2 AND s.deleted = 0
                        ORDER BY s.submitted_at DESC, s.id DESC
                        LIMIT :limit
                        """)
                .bind("studentId", studentId)
                .bind("limit", limit)
                .map((row, meta) -> new GradeTrendRow(
                        row.get("homework_id", Long.class),
                        row.get("title", String.class),
                        row.get("total_score", BigDecimal.class),
                        row.get("submitted_at", LocalDateTime.class)))
                .all();
    }

    public Mono<Long> countActiveEnrollments(Long courseId) {
        return scalar("""
                SELECT COUNT(*) AS cnt FROM course_enrollment
                WHERE course_id = :courseId AND status = 1 AND deleted = 0
                """, spec -> spec.bind("courseId", courseId));
    }

    public Flux<HomeworkStatRow> homeworkStats(Long courseId) {
        return db.sql("""
                        SELECT h.id AS homework_id, h.title,
                               COUNT(DISTINCT CASE WHEN s.status IN (1, 2) THEN s.student_id END) AS submitted_count,
                               AVG(CASE WHEN s.status = 2 THEN s.total_score END) AS avg_score
                        FROM homework h
                        LEFT JOIN submission s ON s.homework_id = h.id AND s.deleted = 0
                        WHERE h.course_id = :courseId AND h.deleted = 0
                        GROUP BY h.id, h.title
                        ORDER BY h.id DESC
                        """)
                .bind("courseId", courseId)
                .map((row, meta) -> new HomeworkStatRow(
                        row.get("homework_id", Long.class),
                        row.get("title", String.class),
                        number(row.get("submitted_count", Number.class)),
                        row.get("avg_score", BigDecimal.class)))
                .all();
    }

    public Flux<StudentRankingRow> studentRanking(Long courseId, int limit) {
        return db.sql("""
                        SELECT u.id AS student_id, COALESCE(u.real_name, u.username) AS student_name,
                               COUNT(CASE WHEN s.status IN (1, 2) THEN s.id END) AS submitted_count,
                               AVG(CASE WHEN s.status = 2 THEN s.total_score END) AS avg_score
                        FROM course_enrollment ce
                        JOIN sys_user u ON u.id = ce.student_id AND u.deleted = 0
                        LEFT JOIN homework h ON h.course_id = ce.course_id AND h.deleted = 0
                        LEFT JOIN submission s ON s.homework_id = h.id
                            AND s.student_id = ce.student_id AND s.deleted = 0
                        WHERE ce.course_id = :courseId AND ce.status = 1 AND ce.deleted = 0
                        GROUP BY u.id, student_name
                        ORDER BY avg_score DESC, submitted_count DESC, u.id ASC
                        LIMIT :limit
                        """)
                .bind("courseId", courseId)
                .bind("limit", limit)
                .map((row, meta) -> new StudentRankingRow(
                        row.get("student_id", Long.class),
                        row.get("student_name", String.class),
                        row.get("avg_score", BigDecimal.class),
                        number(row.get("submitted_count", Number.class))))
                .all();
    }

    public Flux<NodeMasteryRow> nodeMastery(Long courseId) {
        return db.sql("""
                        SELECT n.id AS node_id, n.name,
                               AVG(m.learn_progress) AS avg_progress,
                               AVG(CASE WHEN m.mastery_level = 2 THEN 1 ELSE 0 END) AS mastery_rate
                        FROM knowledge_node n
                        LEFT JOIN knowledge_mastery m ON m.node_id = n.id AND m.deleted = 0
                        WHERE n.course_id = :courseId AND n.deleted = 0
                        GROUP BY n.id, n.name
                        ORDER BY avg_progress ASC, n.id ASC
                        """)
                .bind("courseId", courseId)
                .map((row, meta) -> new NodeMasteryRow(
                        row.get("node_id", Long.class),
                        row.get("name", String.class),
                        row.get("avg_progress", BigDecimal.class),
                        row.get("mastery_rate", BigDecimal.class)))
                .all();
    }

    public Mono<Long> totalUsers() {
        return scalar("SELECT COUNT(*) AS cnt FROM sys_user WHERE deleted = 0", spec -> spec);
    }

    public Mono<Long> totalCourses() {
        return scalar("SELECT COUNT(*) AS cnt FROM course WHERE deleted = 0", spec -> spec);
    }

    public Mono<Long> totalHomeworks() {
        return scalar("SELECT COUNT(*) AS cnt FROM homework WHERE deleted = 0", spec -> spec);
    }

    public Mono<Long> totalSubmissions() {
        return scalar("SELECT COUNT(*) AS cnt FROM submission WHERE deleted = 0", spec -> spec);
    }

    public Mono<Long> storageUsedBytes() {
        return scalar("SELECT COALESCE(SUM(used_bytes), 0) AS cnt FROM drive_quota", spec -> spec);
    }

    public Mono<Long> newUsersSince(LocalDateTime since) {
        return scalar("""
                SELECT COUNT(*) AS cnt FROM sys_user
                WHERE deleted = 0 AND created_at >= :since
                """, spec -> spec.bind("since", since));
    }

    private Mono<Long> scalar(String sql, Binder binder) {
        return binder.bind(db.sql(sql))
                .map((row, meta) -> number(row.get("cnt", Number.class)))
                .one()
                .defaultIfEmpty(0L);
    }

    private Mono<BigDecimal> decimal(String sql, Binder binder) {
        return binder.bind(db.sql(sql))
                .map((row, meta) -> {
                    BigDecimal value = row.get("avg_score", BigDecimal.class);
                    return value == null ? BigDecimal.ZERO : value;
                })
                .one()
                .defaultIfEmpty(BigDecimal.ZERO);
    }

    private static long number(Number number) {
        return number == null ? 0L : number.longValue();
    }

    private interface Binder {
        DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec);
    }

    public record GradeTrendRow(Long homeworkId, String title, BigDecimal score, LocalDateTime submittedAt) {
    }

    public record HomeworkStatRow(Long homeworkId, String title, long submittedCount, BigDecimal averageScore) {
    }

    public record StudentRankingRow(Long studentId, String studentName, BigDecimal averageScore, long submittedCount) {
    }

    public record NodeMasteryRow(Long nodeId, String nodeName, BigDecimal averageProgress, BigDecimal masteryRate) {
    }
}
