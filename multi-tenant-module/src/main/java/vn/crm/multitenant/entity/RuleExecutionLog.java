package vn.crm.multitenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rule_execution_logs")
@Getter
@Setter
public class RuleExecutionLog extends BaseEntity {

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "rule_id")
    private NotificationRule rule;

    @Column(name = "status")
    private String status;

    @Column(name = "error_msg")
    private String errorMsg;

}
