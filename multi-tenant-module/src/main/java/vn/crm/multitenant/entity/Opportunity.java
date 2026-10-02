package vn.crm.multitenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "opportunities")
@Getter
@Setter
public class Opportunity extends BaseEntity {

    @Column(name = "opportunity_name", nullable = false)
    private String opportunityName;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "contact_id")
    private Contact contact;

    @Column(name = "amount")
    private java.math.BigDecimal amount;

    @Column(name = "stage")
    private String stage;

    @Column(name = "expected_close_date")
    private java.time.LocalDate expectedCloseDate;

}
