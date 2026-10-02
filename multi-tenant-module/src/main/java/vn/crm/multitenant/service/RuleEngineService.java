package vn.crm.multitenant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.crm.multitenant.context.TenantContext;
import vn.crm.multitenant.entity.NotificationRule;
import vn.crm.multitenant.entity.RuleExecutionLog;
import vn.crm.multitenant.entity.TenantRegistry;
import vn.crm.multitenant.repository.NotificationRuleRepository;
import vn.crm.multitenant.repository.RuleExecutionLogRepository;
import vn.crm.multitenant.repository.TenantRegistryRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final TenantRegistryRepository tenantRegistryRepository;
    private final NotificationRuleRepository ruleRepository;
    private final RuleExecutionLogRepository logRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Chạy mỗi 15 giây để Demo (Thực tế nên là @Scheduled(cron = "0 0 2 * * ?"))
    @Scheduled(fixedDelay = 15000)
    public void runAutomationRules() {
        log.info("--- Bắt đầu kích hoạt Cỗ máy Tự động hóa (Rule Engine) ---");

        List<TenantRegistry> tenants = tenantRegistryRepository.findAll();
        for (TenantRegistry tenant : tenants) {
            try {
                // Chuyển Context sang Tenant hiện tại
                TenantContext.setTenantId(tenant.getTenantId());
                processRulesForCurrentTenant(tenant.getTenantId());
            } catch (Exception e) {
                log.error("Lỗi khi chạy Rule cho tenant: " + tenant.getTenantId(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Transactional
    public void processRulesForCurrentTenant(String tenantId) {
        List<NotificationRule> rules = ruleRepository.findAll();
        if (rules.isEmpty()) return;

        log.info("Tenant [{}] có {} rules cần quét.", tenantId, rules.size());

        for (NotificationRule rule : rules) {
            try {
                // Chạy lệnh SQL của Rule (Phải trả về cột 'id')
                List<UUID> results = entityManager.createNativeQuery(rule.getSqlFormula(), UUID.class).getResultList();
                
                for (UUID targetId : results) {

                    // Kiểm tra xem đã bắn log cho target này chưa (Tìm trong JSONB custom_properties)
                    // Dùng Native Query hoặc logic code để check. Ở đây demo check bằng code cho dễ.
                    boolean alreadyExecuted = logRepository.findAll().stream()
                            .anyMatch(l -> l.getRule().getId().equals(rule.getId()) 
                                        && l.getCustomProperties().containsKey("target_id")
                                        && l.getCustomProperties().get("target_id").equals(targetId.toString()));

                    if (!alreadyExecuted) {
                        log.info(">> TỰ ĐỘNG HÓA KÍCH HOẠT: Tenant [{}] - Rule: [{}] áp dụng lên ID: {}", 
                                tenantId, rule.getRuleType(), targetId);

                        // TODO: Gửi Email / Bắn thông báo Zalo tại đây...
                        
                        // Ghi Log lại để không chạy trùng
                        RuleExecutionLog executionLog = new RuleExecutionLog();
                        executionLog.setRule(rule);
                        executionLog.setStatus("SUCCESS");
                        
                        Map<String, Object> customProps = executionLog.getCustomProperties();
                        customProps.put("target_id", targetId.toString());
                        executionLog.setCustomProperties(customProps);

                        logRepository.save(executionLog);
                    }
                }
            } catch (Exception e) {
                log.error("Lỗi thực thi SQL của Rule " + rule.getId(), e);
            }
        }
    }
}
