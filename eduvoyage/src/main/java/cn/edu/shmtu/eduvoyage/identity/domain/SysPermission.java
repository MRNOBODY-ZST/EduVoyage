package cn.edu.shmtu.eduvoyage.identity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * {@code sys_permission} — a permission code (e.g. {@code course:create}) used for
 * method-level authorization and dynamic menu rendering on the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_permission")
public class SysPermission {

    public static final int TYPE_MENU = 1;
    public static final int TYPE_BUTTON = 2;
    public static final int TYPE_API = 3;

    @Id
    private Long id;

    private String code;
    private String name;
    private Integer type;

    @Column("parent_id")
    private Long parentId;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    private Integer deleted;
}
