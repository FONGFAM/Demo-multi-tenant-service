package vn.crm.multitenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_rules")
@Getter
@Setter
public class NotificationRule extends BaseEntity {

    @Column(name = "target_table", nullable = false)
    private String targetTable;

    @Column(name = "rule_type", nullable = false)
    private String ruleType;

    @Column(name = "sql_formula", nullable = false)
    private String sqlFormula;

}
