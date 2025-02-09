package de.fhg.ipa.aas_transformer.service.management;

import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.getAnsibleFactsSubmodel;
import static de.fhg.ipa.aas_transformer.test.utils.GenericTestConfig.getSimpleSubmodel;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MariaDbExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class MetricsRestControllerIT {
    @RegisterExtension
    static AasITExtension aasITExtension = new AasITExtension(false);

    // Service Addresses:
    @LocalServerPort
    private  int transformerManagementPort;
    String smRepositoryPort = System.getProperty("aas.submodel-repository.port");

    // Clients:
    MetricsClient metricsClient = null;
    SubmodelRepository smRepository = null;

    @BeforeEach
    public void setUp() {
        if(metricsClient == null)
            this.metricsClient = new MetricsClient("http://localhost:" + transformerManagementPort);

        if(smRepository == null)
            this.smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);
    }

    @Test
    public void testCreateSubmodelAndConfirmSubmodelChangeBeingProcessed() throws IOException, DeserializationException {
        // Assert initial counts are 0:
        assertMetricCounts(0, 0);

        // Create new submodel assert submodelChangeCount is now 1:
        smRepository.createOrUpdateSubmodel(getAnsibleFactsSubmodel());
        assertMetricCounts(1, 0);

        // Update processed submodel change count and assert processedSubmodelChangeCount is now 1:
        metricsClient.addOneToProcessedSubmodelChangeCount().block();
        assertMetricCounts(1, 1);

        return;
    }

    private void assertMetricCounts(int expectedSubmodelChangeCount, int expectedProcessedSubmodelChangeCount) {
        double submodelChangeCount = metricsClient.getSubmodelChangeCount().block();
        double processedSubmodelChangeCount = metricsClient.getProcessedSubmodelChangeCount().block();

        assertEquals(expectedSubmodelChangeCount, submodelChangeCount);
        assertEquals(expectedProcessedSubmodelChangeCount, processedSubmodelChangeCount);
    }
}
