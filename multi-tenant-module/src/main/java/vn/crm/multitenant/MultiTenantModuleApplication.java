package vn.crm.multitenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MultiTenantModuleApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultiTenantModuleApplication.class, args);
    }
}
