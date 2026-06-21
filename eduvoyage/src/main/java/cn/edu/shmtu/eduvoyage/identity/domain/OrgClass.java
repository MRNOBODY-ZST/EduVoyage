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

/** {@code org_class} — 班级, child of a major; students belong to a class. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("org_class")
public class OrgClass {

    @Id
    private Long id;

    @Column("major_id")
    private Long majorId;

    private String name;
    private Integer grade;

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
