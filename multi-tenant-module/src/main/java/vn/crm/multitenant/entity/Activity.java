package vn.crm.multitenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "activities")
@Getter
@Setter
public class Activity extends BaseEntity {

    @Column(name = "activity_type", nullable = false)
    private String activityType;

    @Column(name = "subject")
    private String subject;

    @Column(name = "target_entity")
    private String targetEntity;

    @Column(name = "target_id")
    private java.util.UUID targetId;

    @Column(name = "status")
    private String status;

}
