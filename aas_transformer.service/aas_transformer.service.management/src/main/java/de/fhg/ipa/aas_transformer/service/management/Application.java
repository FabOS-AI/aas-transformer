package de.fhg.ipa.aas_transformer.service.management;

import io.r2dbc.spi.ConnectionFactory;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

@SpringBootApplication(
        scanBasePackages = {
                "de.fhg.ipa.aas_transformer.aas",
                "de.fhg.ipa.aas_transformer.clients.redis",
                "de.fhg.ipa.aas_transformer.open_api",
                "de.fhg.ipa.aas_transformer.service.management",
                "de.fhg.ipa.aas_transformer.transformation",
        },
        exclude = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class,
                ReactiveSecurityAutoConfiguration.class,
                ReactiveManagementWebSecurityAutoConfiguration.class
        }
)
@EnableR2dbcRepositories(basePackages = {
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
    public ConnectedSubmodelRepository getConnectedSubmodelRepository(
            @Value("${aas.submodel-repository.url}") String submodelRepositoryUrl
    ) {
        return new ConnectedSubmodelRepository(submodelRepositoryUrl);
    }

    @Bean
    ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        ResourceDatabasePopulator resource = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
        initializer.setDatabasePopulator(resource);
        return initializer;
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
