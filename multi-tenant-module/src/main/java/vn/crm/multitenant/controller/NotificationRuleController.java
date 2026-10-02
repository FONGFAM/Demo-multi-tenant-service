package vn.crm.multitenant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.crm.multitenant.context.TenantContext;
import vn.crm.multitenant.entity.NotificationRule;
import vn.crm.multitenant.repository.NotificationRuleRepository;
import vn.crm.multitenant.service.AttributeValidationService;

import java.util.List;

import vn.crm.multitenant.validation.SqlFormulaValidator;

@Slf4j
@RestController
@RequestMapping("/api/v1/notification_rules")
@RequiredArgsConstructor
public class NotificationRuleController {

    private final NotificationRuleRepository repository;
    private final AttributeValidationService validationService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody NotificationRule entity) {
        log.info("Tenant {} đang tạo NotificationRule mới", TenantContext.getTenantId());
        try {
            validationService.validateCustomProperties("NOTIFICATIONRULE", entity.getCustomProperties());
            // V5 FIX: validate sql_formula để chống SQL injection
            SqlFormulaValidator.validate(entity.getSqlFormula());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        NotificationRule saved = repository.save(entity);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<NotificationRule>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<NotificationRule>> searchByCustomProperty(@RequestParam String key, @RequestParam String value) {
        return ResponseEntity.ok(repository.findByCustomProperty(key, value));
    }
}
