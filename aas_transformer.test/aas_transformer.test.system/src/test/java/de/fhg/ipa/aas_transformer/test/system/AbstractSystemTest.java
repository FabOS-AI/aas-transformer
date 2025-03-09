package de.fhg.ipa.aas_transformer.test.system;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.JobsClient;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobReader;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

public class AbstractSystemTest {
    // Service Addresses:
    String transformerManagementPort = System.getProperty("aas_transformer.services.management.port");
    String aasRegistryPort = System.getProperty("aas.aas-registry.port");
    String aasRepositoryPort = System.getProperty("aas.aas-repository.port");
    String smRegistryPort = System.getProperty("aas.submodel-registry.port");
    String smRepositoryPort = System.getProperty("aas.submodel-repository.port");

    // Clients:
    ManagementClient managementClient;
    MetricsClient metricsClient;
    JobsClient jobsClient;
    AasRegistry aasRegistry;
    AasRepository aasRepository;
    SubmodelRegistry smRegistry;
    SubmodelRepository smRepository;
    RedisJobReader redisJobReader;

    public AbstractSystemTest() {
        this.managementClient = new ManagementClient("http://localhost:" + transformerManagementPort);
        this.metricsClient = new MetricsClient("http://localhost:" + transformerManagementPort);
        this.jobsClient = new JobsClient("http://localhost:" + transformerManagementPort);
        this.aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        this.aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        this.smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        this.smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);
        LettuceConnectionFactory connFac = new LettuceConnectionFactory(
                System.getProperty("spring.data.redis.host"),
                Integer.parseInt(System.getProperty("spring.data.redis.port"))
        );
        connFac.start();
        this.redisJobReader = new RedisJobReader(connFac);
    }
}
