package de.fhg.ipa.aas_transformer.service.listener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {
                "de.fhg.ipa.aas_transformer.aas",
                "de.fhg.ipa.aas_transformer.clients.management",
                "de.fhg.ipa.aas_transformer.clients.redis",
                "de.fhg.ipa.aas_transformer.service.listener",
                "de.fhg.ipa.aas_transformer.transformation",
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
