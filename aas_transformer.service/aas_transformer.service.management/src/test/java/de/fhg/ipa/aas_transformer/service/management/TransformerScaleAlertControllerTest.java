package de.fhg.ipa.aas_transformer.service.management;

import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.model.alertmanager.AlertMessage;
import de.fhg.ipa.aas_transformer.service.management.converter.modelmapper.TransformerToTransformerDTOListenerConverter;
import de.fhg.ipa.aas_transformer.service.management.exceptions.WaitForScaleTimeoutException;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getSimpleTransformer;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;

@ExtendWith(MariaDbExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Disabled
public class TransformerScaleAlertControllerTest {
    // Service Addresses:
    @LocalServerPort
    private int transformerManagementPort;

    @MockBean
    TransformerServiceHandler transformerServiceHandler;

    private WebClient webclient;

    private String alertPaylod = """
            {"receiver":"webhook\\\\.site","status":"firing","alerts":[{"status":"firing","labels":{"action":"scale-down","alertname":"NoRunningTransformations"},"annotations":{"summary":"In-Queue and In-Progress jobs are empty while more than one executor is running"},"startsAt":"2025-02-26T20:30:24.421Z","endsAt":"0001-01-01T00:00:00Z","generatorURL":"http://caebcc63a343:9090/graph?g0.expr=%28sum%28redis_keys_count%7Bkey%3D%22proc_jobs%2A%22%7D%29+%3D%3D+0%29+and+%28sum%28redis_key_size%7Bkey%3D%22jobs%22%7D+%3D%3D+0%29%29+and+%28count%28container_last_seen%7Bimage%3D~%22.%2Aexecutor.%2A%22%7D+%3E+%28time%28%29+-+60%29%29+%3E+1%29\\u0026g0.tab=1","fingerprint":"000f86d24eb706d9"}],"groupLabels":{"alertname":"NoRunningTransformations"},"commonLabels":{"action":"scale-down","alertname":"NoRunningTransformations"},"commonAnnotations":{"summary":"In-Queue and In-Progress jobs are empty while more than one executor is running"},"externalURL":"http://097c8677ab3a:9093","version":"4","groupKey":"{}:{alertname=\\"NoRunningTransformations\\"}","truncatedAlerts":0}
            """;


    @BeforeEach
    public void setUp() {
        this.webclient = WebClient.create("http://localhost:" + transformerManagementPort+"/scaling/alert");
    }

    @Test
    @Order(10)
    public void sendAlertToEndpointExpectNoException() throws WaitForScaleTimeoutException {
        Mockito
                .doNothing()
                .when(transformerServiceHandler)
                .handleScaleAlert(Mockito.any(AlertMessage.class));

        String response = webclient.post()
                .uri("")
                .header("Content-Type", "application/json")
                .bodyValue(alertPaylod)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return;
    }
}
