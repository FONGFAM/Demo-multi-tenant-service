package vn.crm.multitenant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.crm.multitenant.context.TenantContext;
import vn.crm.multitenant.entity.Ticket;
import vn.crm.multitenant.repository.TicketRepository;
import vn.crm.multitenant.service.AttributeValidationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketRepository repository;
    private final AttributeValidationService validationService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Ticket entity) {
        log.info("Tenant {} đang tạo Ticket mới", TenantContext.getTenantId());
        try {
            validationService.validateCustomProperties("TICKET", entity.getCustomProperties());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        Ticket saved = repository.save(entity);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Ticket>> searchByCustomProperty(@RequestParam String key, @RequestParam String value) {
        return ResponseEntity.ok(repository.findByCustomProperty(key, value));
    }
}
