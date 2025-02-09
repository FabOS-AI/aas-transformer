package de.fhg.ipa.aas_transformer.test.system;

import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.JobsClient;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobReader;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.test.utils.extentions.*;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.getSimpleShell;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(AasITExtension.class)
@ExtendWith(RedisExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class MonitoringSystemTest {
    @RegisterExtension
    static PrometheusExtension prometheusExtension = new PrometheusExtension();

    @RegisterExtension
    static AasTransformerExtension aasTransformerExtension = new AasTransformerExtension(
            true,
            true,
            true
    );

    // region Variables
    // Service Addresses:
    String transformerManagementPort = System.getProperty("aas_transformer.services.management.port");
    String aasRepositoryPort = System.getProperty("aas.aas-repository.port");
    String aasRepositoryPath = System.getProperty("aas.aas-repository.path");
    String smRepositoryPort = System.getProperty("aas.submodel-repository.port");
    String smRepositoryPath = System.getProperty("aas.submodel-repository.path");

    // Clients:
    ManagementClient managementClient;
    MetricsClient metricsClient;
    JobsClient jobsClient;
    AasRepository aasRepository;
    SubmodelRepository smRepository;
    RedisJobReader redisJobReader;

    // Test Objects:
    DefaultAssetAdministrationShell shell = getSimpleShell("", "");
    String timeseriesIdShort = "timeseries";
    TransformerActionTsAvg timeseriesTransformationAction = new TransformerActionTsAvg(
            List.of("sensor0", "sensor1"),
            5
    );
    Transformer testTransformer = new Transformer(
            UUID.randomUUID(),
            new Destination(new DestinationSubmodel(
                "{{ submodel:idShort(SOURCE_SUBMODEL) }}_avg",
                "{{ submodel:id(SOURCE_SUBMODEL) }}_avg")
            ),
            List.of(timeseriesTransformationAction),
            List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesIdShort)))
    );
    // endregion

    public MonitoringSystemTest() {
        this.managementClient = new ManagementClient("http://localhost:" + transformerManagementPort);
        this.metricsClient = new MetricsClient("http://localhost:" + transformerManagementPort);
        this.jobsClient = new JobsClient("http://localhost:" + transformerManagementPort);
        this.aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort+aasRepositoryPath);
        this.smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort+smRepositoryPath);
        LettuceConnectionFactory connFac = new LettuceConnectionFactory(
                System.getProperty("spring.data.redis.host"),
                Integer.parseInt(System.getProperty("spring.data.redis.port"))
        );
        connFac.start();
        this.redisJobReader = new RedisJobReader(connFac);
    }

    @Test
    @Order(10)
    public void testRedisListMonitoringAndTransformationLog() throws InterruptedException {
        managementClient.createTransformer(
                testTransformer,
                false
        ).block();

        Instant startSmCreate = Instant.now();

        // Create shell:
        this.aasRepository.createOrUpdateAas(shell);

        int submodelCount = 100;

        for(int i = 0; i < submodelCount; i++) {
            Submodel submodel = getRandomTimeseriesSubmodel(5, 50, timeseriesIdShort);
            this.aasRepository.addSubmodelReferenceToAas(shell.getId(), submodel);
            this.smRepository.createOrUpdateSubmodel(submodel);
        }

        Instant endSmCreate = Instant.now();

        prometheusExtension.createAnnotation(
                startSmCreate,
                endSmCreate,
                List.of("test", "create_submodels"),
                "Create Submodels"
        );

        int tryCount = 0;
        int tryLimit = 100;
        while(jobsClient.getTotalJobCount().block() != 0) {
            if(tryCount > tryLimit)
                break;
            tryCount++;
            sleep(1000);
        }
        // Wait for last transformation to finish
        tryCount = 0;
        while(metricsClient.getTransformationLogs().collectList().block().size() != 0) {
            if(tryCount > tryLimit)
                break;
            tryCount++;
            sleep(1000);
        }

        List<TransformationLog> logs = metricsClient.getTransformationLogs().collectList().block();

        // TODO: check flakey
        assertTrue(logs.size() == submodelCount);
    }
}
