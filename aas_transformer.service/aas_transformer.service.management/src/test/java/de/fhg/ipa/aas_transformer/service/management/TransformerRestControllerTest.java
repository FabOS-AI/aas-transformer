package de.fhg.ipa.aas_transformer.service.management;

import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.service.management.converter.modelmapper.TransformerToTransformerDTOListenerConverter;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
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
public class TransformerRestControllerTest {
    // Service Addresses:
    @LocalServerPort
    private  int transformerManagementPort;

    // Clients:
    private ManagementClient managementClient;

    // Converter:
    private ModelMapper modelMapper = new ModelMapper();

    // Objects used inside Tests:
    private final static Transformer newTransformer = getSimpleTransformer();

    public TransformerRestControllerTest() {
        modelMapper.addConverter( new TransformerToTransformerDTOListenerConverter() );
    }

    @BeforeEach
    public void setUp() {
        if(managementClient == null)
            this.managementClient = new ManagementClient("http://localhost:" + transformerManagementPort);
    }

    @Test
    @Order(10)
    public void getAllTransformerExpectNone() {
        Flux<Transformer> transformerFlux = this.managementClient.getAllTransformer();
        StepVerifier.create(transformerFlux)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @Order(20)
    public void createTransformerExpectValidChangeEventResponse() {
        // Create Expected Change Event Objects:
        TransformerChangeEventDTOListener expectedChangeEventDTOListener = new TransformerChangeEventDTOListener(
                TransformerChangeEventType.CREATE,
                modelMapper.map(newTransformer, TransformerDTOListener.class)
        );
        TransformerChangeEvent expectedChangeEvent = new TransformerChangeEvent(
                TransformerChangeEventType.CREATE,
                newTransformer
        );

        // Create Flux Objects for Stream Endpoints:
        this.managementClient
                .getTransformerChangeEventStream()
                .log()
                .subscribe(e -> assertEquals(e, expectedChangeEvent));
        this.managementClient
                .getTransformerChangeEventDTOListenerStream()
                .log()
                .subscribe(e -> assertEquals(e, expectedChangeEventDTOListener));

        // Create Transformer:
        this.managementClient
                .createTransformer(newTransformer, false)
                .block();
    }

    @Test
    @Order(30)
    public void getAllTransformerExpectOne() {
        List<Transformer> transformerFlux = this.managementClient.getAllTransformer().collectList().block();
        assertEquals(
                1,
                transformerFlux.size()
        );
    }

    @Test
    @Order(40)
    public void getTransformerByIdExpectOne() {
        Transformer transformer = this.managementClient.getTransformer(newTransformer.getId()).block();
        assertEquals(
                newTransformer,
                transformer
        );
    }

    @Test
    @Order(50)
    public void deleteTransformerExpectValidChangeEventResponse() throws InterruptedException {
        // Create Expected Change Event Objects:
        TransformerChangeEventDTOListener expectedChangeEventDTOListener = new TransformerChangeEventDTOListener(
                TransformerChangeEventType.DELETE,
                modelMapper.map(newTransformer, TransformerDTOListener.class)
        );
        TransformerChangeEvent expectedChangeEvent = new TransformerChangeEvent(
                TransformerChangeEventType.DELETE,
                newTransformer
        );

        // Create Flux Objects for Stream Endpoints:
        this.managementClient
                .getTransformerChangeEventDTOListenerStream()
                .log()
                .subscribe(e -> assertEquals(e, expectedChangeEventDTOListener));
        this.managementClient
                .getTransformerChangeEventStream()
                .log()
                .subscribe(e -> assertEquals(e, expectedChangeEvent));

        // Create Transformer:
        this.managementClient
                .deleteTransformer(newTransformer.getId(), false)
                .block();

        sleep(500);
    }

    @Test
    @Order(60)
    public void getAllTransformerAfterDeleteExpectNone() {
        Flux<Transformer> transformerFlux = this.managementClient.getAllTransformer();
        StepVerifier.create(transformerFlux)
                .expectNextCount(0)
                .verifyComplete();
    }
}
