package cn.edu.shmtu.eduvoyage.identity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * {@code sys_user} — platform account. The {@code id} is an application-generated
 * Snowflake (see {@code IdGenerator}); R2DBC treats a non-null id on save as an
 * update, so new rows must set {@code id} explicitly and use the repository's
 * insert path.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_user")
public class SysUser {

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_LOCKED = 2;

    @Id
    private Long id;

    private String username;
    private String password;

    @Column("real_name")
    private String realName;

    private String email;
    private String phone;

    @Column("avatar_url")
    private String avatarUrl;

    private Integer gender;
    private Integer status;

    @Column("class_id")
    private Long classId;

    @Column("last_login_at")
    private LocalDateTime lastLoginAt;

    @CreatedBy
    @Column("created_by")
    private Long createdBy;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    private Integer deleted;
}
