package vn.crm.multitenant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.crm.multitenant.context.TenantContext;
import vn.crm.multitenant.entity.Note;
import vn.crm.multitenant.repository.NoteRepository;
import vn.crm.multitenant.service.AttributeValidationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteRepository repository;
    private final AttributeValidationService validationService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Note entity) {
        log.info("Tenant {} đang tạo Note mới", TenantContext.getTenantId());
        try {
            validationService.validateCustomProperties("NOTE", entity.getCustomProperties());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        Note saved = repository.save(entity);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Note>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Note>> searchByCustomProperty(@RequestParam String key, @RequestParam String value) {
        return ResponseEntity.ok(repository.findByCustomProperty(key, value));
    }
}
