package vn.crm.multitenant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.crm.multitenant.context.TenantContext;
import vn.crm.multitenant.entity.Customer;
import vn.crm.multitenant.repository.CustomerRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final vn.crm.multitenant.service.AttributeValidationService validationService;

    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody Customer customer) {
        log.info("Tenant {} đang tạo Customer mới: {}", TenantContext.getTenantId(), customer.getFullName());
        try {
            validationService.validateCustomProperties("CUSTOMER", customer.getCustomProperties());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        Customer savedCustomer = customerRepository.save(customer);
        return ResponseEntity.ok(savedCustomer);
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        log.info("Tenant {} đang lấy danh sách Customer", TenantContext.getTenantId());
        return ResponseEntity.ok(customerRepository.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Customer>> searchByCustomProperty(@RequestParam String key, @RequestParam String value) {
        log.info("Tenant {} tìm kiếm JSONB với {} = {}", TenantContext.getTenantId(), key, value);
        return ResponseEntity.ok(customerRepository.findByCustomProperty(key, value));
    }
}
