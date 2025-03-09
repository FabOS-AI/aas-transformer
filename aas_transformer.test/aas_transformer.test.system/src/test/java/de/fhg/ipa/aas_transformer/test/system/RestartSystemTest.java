package de.fhg.ipa.aas_transformer.test.system;

import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasTransformerExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getTimeseriesTransformer;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(AasITExtension.class)
@ExtendWith(RedisExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class RestartSystemTest {
    @RegisterExtension
    static AasTransformerExtension aasTransformerExtension = new AasTransformerExtension(
            true, true, true
    );

    // Service Addresses:
    String transformerManagementPort = System.getProperty("aas_transformer.services.management.port");
    String transformerListenerPort = System.getProperty("aas_transformer.services.listener.port");
    String transformerExecutorPort = System.getProperty("aas_transformer.services.executor.port");

    // Clients:
    ManagementClient managementClient;
    WebClient listenerClient;
    WebClient executorClient;

    // Test objects:
    Transformer timeseriesTransformer = getTimeseriesTransformer("test_timeseries");

    @BeforeEach
    public  void beforeEach() {
        managementClient = new ManagementClient("http://localhost:" + transformerManagementPort);
        listenerClient = WebClient.create("http://localhost:" + transformerListenerPort);
        executorClient = WebClient.create("http://localhost:" + transformerExecutorPort);
        // create Transformer
        managementClient.createTransformer(timeseriesTransformer, false).block();
    }

    @Test
    @Order(10)
    public void testRestartManagementExpectExecutorListenerReconnect() throws InterruptedException {
        List<Transformer> transformer = managementClient.getAllTransformer().collectList().block();
        assertTrue( transformer.size() == 1 );

        // check transformer available @ Listener/Executor
        aasTransformerExtension.restartManagementContainer();

        // delete transformer
        transformer.forEach(
                t -> managementClient.deleteTransformer(t.getId(), false).block()
        );

        sleep(5000);

        transformer = managementClient.getAllTransformer().collectList().block();
        assertTrue( transformer.size() == 0 );

        // check transformer not available @ Listener anymore
        // => reconnect after restart successful
        sleep(15000);
        List<TransformerDTOListener> transformerDtoListener = listenerClient.get()
                .uri("/transformer-cache")
                .retrieve()
                .bodyToFlux(TransformerDTOListener.class)
                .collectList()
                .block();

         assertTrue(transformerDtoListener.size() == 0);

        // check transformer not available @ Executor anymore
        // => reconnect after restart successful
        List<UUID> transformerIds = executorClient.get()
                .uri("/transformer-id-list")
                .retrieve()
                .bodyToFlux(UUID.class)
                .collectList()
                .block();

        assertTrue(transformerIds.size() == 0);
    }
}
