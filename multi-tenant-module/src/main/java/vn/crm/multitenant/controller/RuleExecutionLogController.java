package vn.crm.multitenant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.crm.multitenant.context.TenantContext;
import vn.crm.multitenant.entity.RuleExecutionLog;
import vn.crm.multitenant.repository.RuleExecutionLogRepository;
import vn.crm.multitenant.service.AttributeValidationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/rule_execution_logs")
@RequiredArgsConstructor
public class RuleExecutionLogController {

    private final RuleExecutionLogRepository repository;
    private final AttributeValidationService validationService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody RuleExecutionLog entity) {
        log.info("Tenant {} đang tạo RuleExecutionLog mới", TenantContext.getTenantId());
        try {
            validationService.validateCustomProperties("RULEEXECUTIONLOG", entity.getCustomProperties());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        RuleExecutionLog saved = repository.save(entity);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<RuleExecutionLog>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<RuleExecutionLog>> searchByCustomProperty(@RequestParam String key, @RequestParam String value) {
        return ResponseEntity.ok(repository.findByCustomProperty(key, value));
    }
}
