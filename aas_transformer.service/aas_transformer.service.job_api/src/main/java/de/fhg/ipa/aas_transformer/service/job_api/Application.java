package de.fhg.ipa.aas_transformer.service.job_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {
                "de.fhg.ipa.aas_transformer.clients.redis",
                "de.fhg.ipa.aas_transformer.service.job_api",
                "de.fhg.ipa.aas_transformer.open_api",
        },
        exclude = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class
        }
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
