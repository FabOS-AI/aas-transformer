package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.clients.job_api.JobApiClient;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import de.fhg.ipa.aas_transformer.transformation.TransformationExecutionService;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getDestinationSubmodelIdOfAnsibleFactsTransformer;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FirstTransformerThenAASExecutorIT extends AbstractIT {
    @MockBean
    MetricsClient metricsClient;
//    @MockBean
//    JobApiClient jobApiClient;
    @Autowired
    RedisJobProducer redisJobProducer;

    // Test Objects:
    static Transformer factsTransformer = getAnsibleFactsTransformer(false);
    static DefaultAssetAdministrationShell shell = getSimpleShell("", "");
    static Submodel factsSubmodel = getAnsibleFactsSubmodel();

    // Mocks ManagementClient; Client return factsTransformer
    @TestConfiguration
    public static class TestConfig {
        @MockBean
        ManagementClient managementClient;

        @PostConstruct
        public void initMock(){
            Sinks.Many<TransformerChangeEvent> changeEventSink =
                    Sinks.many().unicast().onBackpressureBuffer();
            Sinks.Many<TransformerChangeEventDTOListener> changeEventDtoListenerSink =
                    Sinks.many().unicast().onBackpressureBuffer();
            Mockito
                    .when(managementClient.getAllTransformer())
                    .thenReturn(Flux.just(factsTransformer));
            Mockito
                    .when(managementClient.getAllTransformerDTOListener())
                    .thenReturn(Flux.empty());
            Mockito
                    .when(managementClient.getTransformerChangeEventStream())
                    .thenReturn(changeEventSink.asFlux());
            Mockito
                    .when(managementClient.getTransformerChangeEventDTOListenerStream())
                    .thenReturn(changeEventDtoListenerSink.asFlux());
        }
    }

    @PostConstruct
    public void init() {
        // Log Transformation:
        Mockito
                .when(metricsClient.addTransformationLog(Mockito.any()))
                .thenReturn(Mono.just(new TransformationLog(
                        "destinationAasId",
                        "destinationSubmodelId",
                        "sourceSubmodelId",
                        null,
                        null,
                        null,
                        null
                )));
    }

    @BeforeEach
    void setUp() throws ApiException {
        this.aasRepository.createOrUpdateAas(shell);
        this.aasRepository.addSubmodelReferenceToAas(shell.getId(), factsSubmodel);
        this.smRepository.createOrUpdateSubmodel(factsSubmodel);
        this.aasRegistry.addSubmodelDescriptorToAas(
                shell.getId(),
                this.smRegistry.findSubmodelDescriptor(factsSubmodel.getId()).get()
        );
    }

    @Test
    @Order(10)
    public void testCountOfExecutionServicesExpectOne() {
        List<TransformationExecutionService> services =
                transformationExecutionServiceCache.transformationExecutionServices;

        assertEquals(1, services.size());
    }

    @Test
    @Order(20)
    public void testCreateTransformationJobExpectTwoSubmodelsAndNoJobs() throws DeserializationException, SerializationException, InterruptedException, ApiException {
        TransformationJob createdJob = new TransformationJob(
                UUID.randomUUID(),
                TransformationJobAction.EXECUTE,
                factsTransformer.getId(),
                factsSubmodel.getId(),
                null,
                null
        );

        assertEquals(1, aasRepository.getAas(shell.getId()).getSubmodels().size());
        assertExpectedSubmodelCount(aasRegistry, aasRepository, smRepository, shell.getId(), 1, 1);

//        Mockito
//                .when(jobApiClient.getNextJob())
//                .thenReturn(Mono.just(createdJob))
//                .thenReturn(Mono.empty());
        redisJobProducer.pushJob(createdJob);

        int expectedSubmodelCount = 2;

        assertExpectedSubmodelCount(aasRegistry, aasRepository, smRepository, shell.getId(), expectedSubmodelCount, expectedSubmodelCount);
    }

    @Test
    @Order(30)
    public void testDeleteTransformationJobExpectOneSubmodelAndNoJobs() throws SerializationException, InterruptedException, DeserializationException, ApiException {
        String destinationSubmodelId = getDestinationSubmodelIdOfAnsibleFactsTransformer(
                factsTransformer,
                shell.getId()
        );

        TransformationJob deletedJob = new TransformationJob(
                UUID.randomUUID(),
            TransformationJobAction.DELETE,
                factsTransformer.getId(),
                destinationSubmodelId,
                null,
                null
        );

//        Mockito
//                .when(jobApiClient.getNextJob())
//                .thenReturn(Mono.just(deletedJob))
//                .thenReturn(Mono.empty());
        redisJobProducer.pushJob(deletedJob);

        int expectedSubmodelCount = 1;
        assertExpectedSubmodelCount(aasRegistry, aasRepository, smRepository, shell.getId(), expectedSubmodelCount, expectedSubmodelCount);
    }
}
