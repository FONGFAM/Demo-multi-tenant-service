package vn.crm.multitenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notes")
@Getter
@Setter
public class Note extends BaseEntity {

    @Column(name = "target_entity")
    private String targetEntity;

    @Column(name = "target_id")
    private java.util.UUID targetId;

    @Column(name = "content")
    private String content;

}
