package de.fhg.ipa.aas_transformer.service;

import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
        "de.fhg.ipa.aas_transformer.persistence"
})
@EntityScan(basePackages = {
        "de.fhg.ipa.aas_transformer.model"
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ConnectedSubmodelRepository getConnectedSubmodelRepository(@Value("${aas.submodel-repository.url}") String submodelRepositoryUrl) {
        return new ConnectedSubmodelRepository(submodelRepositoryUrl);
    }
}
