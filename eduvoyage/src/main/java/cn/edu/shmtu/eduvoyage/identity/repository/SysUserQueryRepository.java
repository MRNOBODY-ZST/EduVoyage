package cn.edu.shmtu.eduvoyage.identity.repository;

import cn.edu.shmtu.eduvoyage.identity.domain.SysUser;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Dynamic, paginated user search for the admin console. Kept separate from the
 * derived-query {@link SysUserRepository} because the filter set is optional and
 * composed at runtime — awkward to express as {@code @Query} method derivation.
 */
@Repository
public class SysUserQueryRepository {

    private final DatabaseClient db;

    public SysUserQueryRepository(DatabaseClient db) {
        this.db = db;
    }

    /** A page of users matching the optional keyword / status / class filters. */
    public Flux<SysUser> search(String keyword, Integer status, Long classId, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE deleted = 0");
        appendFilters(sql, keyword, status, classId);
        sql.append(" ORDER BY id DESC LIMIT :limit OFFSET :offset");

        GenericExecuteSpec spec = db.sql(sql.toString());
        spec = bindFilters(spec, keyword, status, classId);
        spec = spec.bind("limit", limit).bind("offset", offset);
        return spec.map(SysUserQueryRepository::mapUser).all();
    }

    /** Total count for the same filter set (for {@code PageResult.total}). */
    public Mono<Long> count(String keyword, Integer status, Long classId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cnt FROM sys_user WHERE deleted = 0");
        appendFilters(sql, keyword, status, classId);

        GenericExecuteSpec spec = db.sql(sql.toString());
        spec = bindFilters(spec, keyword, status, classId);
        return spec.map((row, meta) -> row.get("cnt", Long.class)).one()
                .defaultIfEmpty(0L);
    }

    private static void appendFilters(StringBuilder sql, String keyword, Integer status, Long classId) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (username LIKE :kw OR real_name LIKE :kw OR email LIKE :kw OR phone LIKE :kw)");
        }
        if (status != null) {
            sql.append(" AND status = :status");
        }
        if (classId != null) {
            sql.append(" AND class_id = :classId");
        }
    }

    private static GenericExecuteSpec bindFilters(GenericExecuteSpec spec, String keyword, Integer status, Long classId) {
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.bind("kw", "%" + keyword.trim() + "%");
        }
        if (status != null) {
            spec = spec.bind("status", status);
        }
        if (classId != null) {
            spec = spec.bind("classId", classId);
        }
        return spec;
    }

    private static SysUser mapUser(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata meta) {
        return SysUser.builder()
                .id(row.get("id", Long.class))
                .username(row.get("username", String.class))
                .password(row.get("password", String.class))
                .realName(row.get("real_name", String.class))
                .email(row.get("email", String.class))
                .phone(row.get("phone", String.class))
                .avatarUrl(row.get("avatar_url", String.class))
                .gender(row.get("gender", Integer.class))
                .status(row.get("status", Integer.class))
                .classId(row.get("class_id", Long.class))
                .lastLoginAt(row.get("last_login_at", java.time.LocalDateTime.class))
                .createdBy(row.get("created_by", Long.class))
                .createdAt(row.get("created_at", java.time.LocalDateTime.class))
                .updatedAt(row.get("updated_at", java.time.LocalDateTime.class))
                .deleted(row.get("deleted", Integer.class))
                .build();
    }
}
