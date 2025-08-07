package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.clients.job_api.JobApiClient;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
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

import java.io.FileNotFoundException;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getDestinationSubmodelIdOfAnsibleFactsTransformer;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FirstAASThenTransformerExecutorIT extends AbstractIT {
    @MockBean
    MetricsClient metricsClient;
//    @MockBean
//    JobApiClient jobApiClient;
    @Autowired
    RedisJobProducer redisJobProducer;

    // Test Objects:
    static Submodel factsSubmodel = getAnsibleFactsSubmodel();
    static Transformer factsTransformer = getAnsibleFactsTransformer(false);
    static DefaultAssetAdministrationShell shell = getSimpleShell("", "");

    // Mocks ManagementClient; Client return factsTransformer
    @TestConfiguration
    public static class TestConfig {
        @MockBean
        static ManagementClient managementClient;
        static Sinks.Many<TransformerChangeEvent> changeEventSink =
                Sinks.many().unicast().onBackpressureBuffer();
        static Flux<TransformerChangeEvent> changeEventFlux = changeEventSink.asFlux();

        @PostConstruct
        public void initMock(){
            Mockito
                    .when(managementClient.getAllTransformer())
                    .thenReturn(Flux.empty());
            Mockito
                    .when(managementClient.getAllTransformerDTOListener())
                    .thenReturn(Flux.empty());
            Mockito
                    .when(managementClient.getTransformerChangeEventStream())
                    .thenReturn(changeEventFlux);
            Mockito
                    .when(managementClient.getTransformerChangeEventDTOListenerStream())
                    .thenReturn(Flux.empty());
            Mockito
                    .when(managementClient.getTransformer(Mockito.any()))
                    .thenReturn(Mono.just(factsTransformer));
        }
    }

    @BeforeAll
    static void beforeAll() throws ApiException {
        // Create AAS and Facts Submodel
        aasRepository.createOrUpdateAas(shell);
        aasRepository.addSubmodelReferenceToAas(shell.getId(), factsSubmodel);
        smRepository.createOrUpdateSubmodel(factsSubmodel);
        aasRegistry.addSubmodelDescriptorToAas(
                shell.getId(),
                smRegistry.findSubmodelDescriptor(factsSubmodel.getId()).get()
        );
    }

    @Test
    @Order(10)
    public void testEmitTransformerChangeEventExpectOneTransformationExecutionService() {
        assertEquals(
                0,
                transformationExecutionServiceCache.transformationExecutionServices.size()
        );

        TestConfig.changeEventSink.tryEmitNext(
                new TransformerChangeEvent(
                        TransformerChangeEventType.CREATE,
                        factsTransformer
                )
        );

        assertEquals(
                1,
                transformationExecutionServiceCache.transformationExecutionServices.size()
        );
    }

    @Test
    @Order(20)
    public void testPushCreateTransformationJobExpectTwoSubmodels() throws DeserializationException, FileNotFoundException, SerializationException, InterruptedException, ApiException {
        Mockito
                .when(metricsClient.addTransformationLog(Mockito.any()))
                .thenReturn(Mono.empty());

        TransformationJob createdJob = new TransformationJob(
                UUID.randomUUID(),
                EXECUTE,
                factsTransformer.getId(),
                factsSubmodel.getId(),
                null,
                null
        );

        assertExpectedSubmodelCount(aasRegistry, aasRepository, smRepository, shell.getId(), 1, 1);

//        Mockito
//                .when(jobApiClient.getNextJob())
//                .thenReturn(Mono.just(createdJob))
//                .thenReturn(Mono.empty());
//        redisJobClient.rightPushJob(new RedisTransformationJob(createdJob));
//        assertExpectedJobCount(redisJobConsumer, 0);
        redisJobProducer.pushJob(createdJob);

        assertExpectedSubmodelCount(aasRegistry, aasRepository, smRepository, shell.getId(), 2, 2);
    }

    @Test
    @Order(30)
    public void testPushDeleteTransformationJobExpectOneSubmodel() throws DeserializationException, FileNotFoundException, SerializationException, InterruptedException, ApiException {
        String destinationSubmodelId = getDestinationSubmodelIdOfAnsibleFactsTransformer(
                factsTransformer,
                shell.getId()
        );

        TransformationJob deleteJob = new TransformationJob(
                UUID.randomUUID(),
                TransformationJobAction.DELETE,
                factsTransformer.getId(),
                destinationSubmodelId,
                null,
                null
        );

        assertEquals(
                2,
                this.smRepository.getAllSubmodels().size()
        );

//        Mockito
//                .when(jobApiClient.getNextJob())
//                .thenReturn(Mono.just(deleteJob))
//                .thenReturn(Mono.empty());

        redisJobProducer.pushJob(deleteJob);
//        redisJobClient.rightPushJob(new RedisTransformationJob(deleteJob));
//
//        assertExpectedJobCount(redisJobConsumer, 0);
        assertExpectedSubmodelCount(
                aasRegistry,
                aasRepository,
                smRepository,
                shell.getId(),
                1,
                1
        );
    }
}
