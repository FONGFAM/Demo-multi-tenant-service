package vn.crm.multitenant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.crm.multitenant.entity.AttributeDefinition;
import vn.crm.multitenant.repository.AttributeDefinitionRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttributeValidationService {

    private final AttributeDefinitionRepository attributeDefinitionRepository;

    public void validateCustomProperties(String entityType, Map<String, Object> customProperties) {
        if (customProperties == null || customProperties.isEmpty()) {
            return;
        }

        List<AttributeDefinition> definitions = attributeDefinitionRepository.findByEntityType(entityType);
        
        // Check for undefined attributes
        for (String key : customProperties.keySet()) {
            AttributeDefinition def = definitions.stream()
                    .filter(d -> d.getAttributeKey().equals(key))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Thuộc tính không hợp lệ: " + key + " chưa được định nghĩa trong hệ thống"));
            
            // Validate data type
            Object value = customProperties.get(key);
            if (value != null) {
                switch (def.getDataType()) {
                    case "NUMBER":
                        if (!(value instanceof Number)) {
                            throw new IllegalArgumentException("Thuộc tính " + key + " phải là kiểu số (NUMBER)");
                        }
                        break;
                    case "BOOLEAN":
                        if (!(value instanceof Boolean)) {
                            throw new IllegalArgumentException("Thuộc tính " + key + " phải là kiểu đúng/sai (BOOLEAN)");
                        }
                        break;
                    case "STRING":
                    default:
                        if (!(value instanceof String)) {
                            throw new IllegalArgumentException("Thuộc tính " + key + " phải là kiểu chuỗi (STRING)");
                        }
                        break;
                }
            }
        }

        // Check for missing required attributes
        for (AttributeDefinition def : definitions) {
            if (Boolean.TRUE.equals(def.getIsRequired()) && !customProperties.containsKey(def.getAttributeKey())) {
                throw new IllegalArgumentException("Thiếu thuộc tính bắt buộc: " + def.getAttributeKey());
            }
        }
    }
}
