package vn.crm.multitenant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.crm.multitenant.context.TenantContext;
import vn.crm.multitenant.entity.Opportunity;
import vn.crm.multitenant.repository.OpportunityRepository;
import vn.crm.multitenant.service.AttributeValidationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityRepository repository;
    private final AttributeValidationService validationService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Opportunity entity) {
        log.info("Tenant {} đang tạo Opportunity mới", TenantContext.getTenantId());
        try {
            validationService.validateCustomProperties("OPPORTUNITY", entity.getCustomProperties());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        Opportunity saved = repository.save(entity);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Opportunity>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Opportunity>> searchByCustomProperty(@RequestParam String key, @RequestParam String value) {
        return ResponseEntity.ok(repository.findByCustomProperty(key, value));
    }
}
