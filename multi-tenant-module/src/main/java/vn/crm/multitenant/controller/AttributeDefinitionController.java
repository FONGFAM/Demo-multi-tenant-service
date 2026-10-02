package vn.crm.multitenant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.crm.multitenant.context.TenantContext;
import vn.crm.multitenant.entity.AttributeDefinition;
import vn.crm.multitenant.repository.AttributeDefinitionRepository;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/attributes")
@RequiredArgsConstructor
public class AttributeDefinitionController {

    private final AttributeDefinitionRepository attributeDefinitionRepository;

    @PostMapping
    public ResponseEntity<AttributeDefinition> createAttribute(@RequestBody AttributeDefinition def) {
        log.info("Tenant {} đang tạo Attribute definition: {}", TenantContext.getTenantId(), def.getAttributeKey());
        AttributeDefinition savedDef = attributeDefinitionRepository.save(def);
        return ResponseEntity.ok(savedDef);
    }

    @GetMapping
    public ResponseEntity<List<AttributeDefinition>> getAttributes() {
        log.info("Tenant {} đang lấy danh sách Attributes", TenantContext.getTenantId());
        return ResponseEntity.ok(attributeDefinitionRepository.findAll());
    }
}
