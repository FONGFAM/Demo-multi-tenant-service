package vn.crm.multitenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Bảng Leads (Khách hàng tiềm năng) - 1 trong 9 Bảng Cốt lõi của CRM.
 * Được kế thừa 7 trường mặc định từ BaseEntity.
 */
@Entity
@Table(name = "leads")
@Getter
@Setter
public class Lead extends BaseEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "source")
    private String source;

    @Column(name = "status", nullable = false)
    private String status = "NEW";
}
