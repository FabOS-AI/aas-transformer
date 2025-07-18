package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisTransformationJob;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.clients.redis.RedisTestObjects.assertExpectedJobCount;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getDestinationSubmodelIdOfAnsibleFactsTransformer;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FirstTransformerThenAASExecutorIT extends AbstractIT {
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
                TransformationJobAction.EXECUTE,
                factsTransformer.getId(),
                factsSubmodel.getId(),
                null,
                null
        );

        assertEquals(1, aasRepository.getAas(shell.getId()).getSubmodels().size());
        assertExpectedSubmodelCount(aasRegistry, aasRepository, smRepository, shell.getId(), 1, 1);

        redisJobClient.rightPushJob(new RedisTransformationJob(createdJob));

        int expectedSubmodelCount = 2;

        assertExpectedJobCount(redisJobReader,0);
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
            TransformationJobAction.DELETE,
            null,
                destinationSubmodelId,
            null,
                null
        );

        redisJobClient.rightPushJob(new RedisTransformationJob(deletedJob));

        int expectedSubmodelCount = 1;

        assertExpectedJobCount(redisJobReader,0);
        assertExpectedSubmodelCount(aasRegistry, aasRepository, smRepository, shell.getId(), expectedSubmodelCount, expectedSubmodelCount);
    }
}
