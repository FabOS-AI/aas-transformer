package de.fhg.ipa.aas_transformer.test.system;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.JobsClient;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobReader;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.utils.extentions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.getRandomAnsibleFactsTriples;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.registerAasObjectsFromTriples;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(AasITExtension.class)
@ExtendWith(RedisExtension.class)
@ExtendWith(PrometheusExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class MultiExecutorSystemTest {
    @RegisterExtension
    static AasTransformerExtension aasTransformerExtension = new AasTransformerExtension(
            1,
            2
    );

    //region Test Vars
    // Service Ports:
    String transformerManagementPort = System.getProperty("aas_transformer.services.management.port");
    String aasRegistryPort = System.getProperty("aas.aas-registry.port");
    String aasRepositoryPort = System.getProperty("aas.aas-repository.port");
    String smRegistryPort = System.getProperty("aas.submodel-registry.port");
    String smRepositoryPort = System.getProperty("aas.submodel-repository.port");

    // Service Clients:
    static ManagementClient managementClient;
    static MetricsClient metricsClient;
    static JobsClient jobsClient;
    static AasRegistry aasRegistry;
    static AasRepository aasRepository;
    static SubmodelRegistry smRegistry;
    static SubmodelRepository smRepository;

    // Test triples:
    static List<List<Object>> triples;

    // Setup:
    static boolean isTransformerCreated = false;
    // endregion

    @BeforeAll
    static void beforeAll() throws Exception {
        triples = getRandomAnsibleFactsTriples(1000);
    }

    @BeforeEach
    void setUp() {
        managementClient = new ManagementClient("http://localhost:" + transformerManagementPort);
        metricsClient = new MetricsClient("http://localhost:" + transformerManagementPort);
        jobsClient = new JobsClient("http://localhost:" + transformerManagementPort);

        aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);

        if(!isTransformerCreated) {
            managementClient.createTransformer(
                    (Transformer) triples.get(0).get(2),
                    false
            ).block();
            isTransformerCreated = true;
        }
    }

    @Test
    @Order(10)
    public void testMultiExecutorSetup() {
        registerAasObjectsFromTriples(aasRegistry, aasRepository, smRegistry, smRepository, triples);

        Long inProgressJobCount = jobsClient.getInProgressJobCount().block();

        return;
    }
}
