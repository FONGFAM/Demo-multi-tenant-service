package vn.crm.multitenant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.crm.multitenant.context.TenantContext;
import vn.crm.multitenant.entity.Lead;
import vn.crm.multitenant.repository.LeadRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadRepository leadRepository;
    private final vn.crm.multitenant.service.AttributeValidationService validationService;

    @PostMapping
    public ResponseEntity<?> createLead(@RequestBody Lead lead) {
        log.info("Tenant {} đang tạo Lead mới: {}", TenantContext.getTenantId(), lead.getFullName());
        try {
            validationService.validateCustomProperties("LEAD", lead.getCustomProperties());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        Lead savedLead = leadRepository.save(lead);
        return ResponseEntity.ok(savedLead);
    }

    @GetMapping
    public ResponseEntity<List<Lead>> getAllLeads() {
        log.info("Tenant {} đang lấy danh sách Lead", TenantContext.getTenantId());
        return ResponseEntity.ok(leadRepository.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Lead>> searchByCustomProperty(@RequestParam String key, @RequestParam String value) {
        log.info("Tenant {} tìm kiếm JSONB với {} = {}", TenantContext.getTenantId(), key, value);
        return ResponseEntity.ok(leadRepository.findByCustomProperty(key, value));
    }
}
