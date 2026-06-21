package cn.edu.shmtu.eduvoyage.drive.repository;

import cn.edu.shmtu.eduvoyage.drive.domain.DriveNode;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * SQL that benefits from explicit statements: recursive tree reads, conditional
 * quota reservation, duplicate-name checks and counters.
 */
@Repository
public class DriveQueryRepository {

    private final DatabaseClient db;

    public DriveQueryRepository(DatabaseClient db) {
        this.db = db;
    }

    public Mono<Boolean> reserveQuota(Long userId, long bytes) {
        if (bytes <= 0) {
            return Mono.just(true);
        }
        return db.sql("""
                        UPDATE drive_quota
                        SET used_bytes = used_bytes + :bytes
                        WHERE user_id = :userId AND used_bytes + :bytes <= total_bytes
                        """)
                .bind("userId", userId)
                .bind("bytes", bytes)
                .fetch()
                .rowsUpdated()
                .map(count -> count > 0);
    }

    public Mono<Void> releaseQuota(Long userId, long bytes) {
        if (bytes <= 0) {
            return Mono.empty();
        }
        return db.sql("""
                        UPDATE drive_quota
                        SET used_bytes = GREATEST(used_bytes - :bytes, 0)
                        WHERE user_id = :userId
                        """)
                .bind("userId", userId)
                .bind("bytes", bytes)
                .then();
    }

    public Mono<Void> incrementFileRef(Long fileId) {
        return db.sql("UPDATE drive_file SET ref_count = ref_count + 1 WHERE id = :id")
                .bind("id", fileId)
                .then();
    }

    public Mono<Void> decrementFileRef(Long fileId) {
        return db.sql("UPDATE drive_file SET ref_count = GREATEST(ref_count - 1, 0) WHERE id = :id")
                .bind("id", fileId)
                .then();
    }

    public Mono<Void> deletePhysicalFileRow(Long fileId) {
        return db.sql("DELETE FROM drive_file WHERE id = :id AND ref_count = 0")
                .bind("id", fileId)
                .then();
    }

    public Mono<Void> incrementShareView(Long shareId) {
        return db.sql("UPDATE drive_share SET view_count = view_count + 1 WHERE id = :id")
                .bind("id", shareId)
                .then();
    }

    public Mono<Boolean> personalRootNameExists(Long ownerId, String name, Long excludeId) {
        String sql = """
                SELECT COUNT(*) AS cnt FROM drive_node
                WHERE owner_id = :ownerId AND space_type = 1 AND parent_id = 0
                  AND name = :name AND deleted = 0
                """;
        if (excludeId != null) {
            sql += " AND id <> :excludeId";
        }
        DatabaseClient.GenericExecuteSpec spec = db.sql(sql)
                .bind("ownerId", ownerId)
                .bind("name", name);
        if (excludeId != null) {
            spec = spec.bind("excludeId", excludeId);
        }
        return countExists(spec);
    }

    public Mono<Boolean> courseRootNameExists(Long courseId, String name, Long excludeId) {
        String sql = """
                SELECT COUNT(*) AS cnt FROM drive_node
                WHERE space_type = 2 AND course_id = :courseId AND parent_id = 0
                  AND name = :name AND deleted = 0
                """;
        if (excludeId != null) {
            sql += " AND id <> :excludeId";
        }
        DatabaseClient.GenericExecuteSpec spec = db.sql(sql)
                .bind("courseId", courseId)
                .bind("name", name);
        if (excludeId != null) {
            spec = spec.bind("excludeId", excludeId);
        }
        return countExists(spec);
    }

    public Mono<Boolean> childNameExists(Long parentId, String name, Long excludeId) {
        String sql = """
                SELECT COUNT(*) AS cnt FROM drive_node
                WHERE parent_id = :parentId AND name = :name AND deleted = 0
                """;
        if (excludeId != null) {
            sql += " AND id <> :excludeId";
        }
        DatabaseClient.GenericExecuteSpec spec = db.sql(sql)
                .bind("parentId", parentId)
                .bind("name", name);
        if (excludeId != null) {
            spec = spec.bind("excludeId", excludeId);
        }
        return countExists(spec);
    }

    private static Mono<Boolean> countExists(DatabaseClient.GenericExecuteSpec spec) {
        return spec.map((row, meta) -> {
                    Number n = row.get("cnt", Number.class);
                    return n != null && n.longValue() > 0;
                })
                .one()
                .defaultIfEmpty(false);
    }

    public Flux<DriveNode> findBreadcrumb(Long nodeId) {
        return db.sql("""
                        WITH RECURSIVE ancestors AS (
                          SELECT dn.*, 0 AS depth
                          FROM drive_node dn
                          WHERE dn.id = :id AND dn.deleted = 0
                          UNION ALL
                          SELECT p.*, a.depth + 1 AS depth
                          FROM drive_node p
                          JOIN ancestors a ON a.parent_id = p.id
                          WHERE p.deleted = 0
                        )
                        SELECT * FROM ancestors ORDER BY depth DESC
                        """)
                .bind("id", nodeId)
                .map(DriveQueryRepository::mapNode)
                .all();
    }

    public Flux<DriveNode> findSubtree(Long rootId) {
        return db.sql("""
                        WITH RECURSIVE subtree AS (
                          SELECT * FROM drive_node WHERE id = :id AND deleted = 0
                          UNION ALL
                          SELECT child.*
                          FROM drive_node child
                          JOIN subtree parent ON child.parent_id = parent.id
                          WHERE child.deleted = 0
                        )
                        SELECT * FROM subtree ORDER BY parent_id ASC, is_dir DESC, name ASC, id ASC
                        """)
                .bind("id", rootId)
                .map(DriveQueryRepository::mapNode)
                .all();
    }

    public Flux<DriveNode> findPersonalTree(Long ownerId) {
        return db.sql("""
                        SELECT * FROM drive_node
                        WHERE owner_id = :ownerId AND space_type = 1 AND deleted = 0
                        ORDER BY parent_id ASC, is_dir DESC, name ASC, id ASC
                        """)
                .bind("ownerId", ownerId)
                .map(DriveQueryRepository::mapNode)
                .all();
    }

    public Flux<DriveNode> findCourseTree(Long courseId) {
        return db.sql("""
                        SELECT * FROM drive_node
                        WHERE space_type = 2 AND course_id = :courseId AND deleted = 0
                        ORDER BY parent_id ASC, is_dir DESC, name ASC, id ASC
                        """)
                .bind("courseId", courseId)
                .map(DriveQueryRepository::mapNode)
                .all();
    }

    private static DriveNode mapNode(Row row, RowMetadata meta) {
        return DriveNode.builder()
                .id(row.get("id", Long.class))
                .ownerId(row.get("owner_id", Long.class))
                .spaceType(row.get("space_type", Integer.class))
                .courseId(row.get("course_id", Long.class))
                .parentId(row.get("parent_id", Long.class))
                .name(row.get("name", String.class))
                .isDir(row.get("is_dir", Integer.class))
                .fileId(row.get("file_id", Long.class))
                .createdBy(row.get("created_by", Long.class))
                .createdAt(row.get("created_at", LocalDateTime.class))
                .updatedAt(row.get("updated_at", LocalDateTime.class))
                .deleted(row.get("deleted", Integer.class))
                .build();
    }
}
