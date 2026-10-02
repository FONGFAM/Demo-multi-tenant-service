package vn.crm.multitenant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.crm.multitenant.context.TenantContext;
import vn.crm.multitenant.entity.Product;
import vn.crm.multitenant.repository.ProductRepository;
import vn.crm.multitenant.service.AttributeValidationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository repository;
    private final AttributeValidationService validationService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Product entity) {
        log.info("Tenant {} đang tạo Product mới", TenantContext.getTenantId());
        try {
            validationService.validateCustomProperties("PRODUCT", entity.getCustomProperties());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        Product saved = repository.save(entity);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchByCustomProperty(@RequestParam String key, @RequestParam String value) {
        return ResponseEntity.ok(repository.findByCustomProperty(key, value));
    }
}
