package vn.crm.multitenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "accounts")
@Getter
@Setter
public class Account extends BaseEntity {

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "industry")
    private String industry;

    @Column(name = "website")
    private String website;
}
