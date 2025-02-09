package de.fhg.ipa.aas_transformer.service.management;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.model.TransformationLog;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.aas.AasUtils.createClone;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getDestinationSubmodelIdOfAnsibleFactsTransformer;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class TransformationLogRestControllerTest {
    // region Test Vars
    // Service Addresses:
    @LocalServerPort
    private  int transformerManagementPort;

    // Clients:
    private static ManagementClient managementClient;
    private static MetricsClient metricsClient;

    // endregion


    @BeforeEach
    public void setUp() {
        if(managementClient == null) {
            this.managementClient = new ManagementClient("http://localhost:" + transformerManagementPort);
            this.metricsClient = new MetricsClient("http://localhost:" + transformerManagementPort);
        }

    }

    @Test
    @Order(10)
    public void testGetTransformationLogsExpectNone() {
        List<TransformationLog> transformationLogs = metricsClient.getTransformationLogs()
                .collectList()
                .block();

        assertTrue(transformationLogs.isEmpty());
    }

    @Test
    @Order(20)
    public void testCreateTransformationLogExpectOne() {
        metricsClient.addTransformationLog(
                new TransformationLog(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID(),
                        Duration.ofSeconds(5).toMillis(),
                        Duration.ofSeconds(5).toMillis(),
                        Duration.ofSeconds(5).toMillis()
                )
        ).block();

        List<TransformationLog> transformationLogs = metricsClient.getTransformationLogs()
                .collectList()
                .block();

        assertTrue(transformationLogs.size() == 1);
    }

    @Test
    @Order(30)
    public void testClearTransformationLogsExpectNone() {
        metricsClient.clearTransformationLog().block();
        List<TransformationLog> transformationLogs = metricsClient.getTransformationLogs()
                .collectList()
                .block();

        assertTrue(transformationLogs.isEmpty());
    }
}
