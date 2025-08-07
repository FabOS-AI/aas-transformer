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
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StrictModeExecutorIT extends AbstractIT {
    @MockBean
    MetricsClient metricsClient;
//    @MockBean
//    JobApiClient jobApiClient;
    @Autowired
    RedisJobProducer redisJobProducer;

    // region Test vars
    // Test Transformer/AAS Objects:
    static boolean isInitialAasSetupDone = false;
    static AssetAdministrationShell testAas = getSimpleShell("", "");
    static Submodel ansibleFactsSubmodel = getAnsibleFactsSubmodel();
    static List<Submodel> testSubmodels = List.of(ansibleFactsSubmodel);
    static Transformer factsTransformerCopy = getAnsibleFactsTransformer(false);
    static List<Transformer> testTransformers = List.of(factsTransformerCopy);
    // endregion

    // Mocks ManagementClient; Client return testTransformer
    @TestConfiguration
    public static class TestConfig {
        @MockBean
        ManagementClient managementClient;
        static Sinks.Many<TransformerChangeEvent> changeEventSink =
                Sinks.many().unicast().onBackpressureBuffer();
        static Sinks.Many<TransformerChangeEventDTOListener> changeEventDtoListenerSink =
                Sinks.many().unicast().onBackpressureBuffer();

        @PostConstruct
        public void initMock() {
            Mockito
                    .when(managementClient.getAllTransformer())
                    .thenReturn(Flux.fromStream(testTransformers.stream()));
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

    private static Stream<Arguments> getTestTransformers() {

        return testTransformers
                .stream()
                .map(transformer -> Arguments.of(Named.of(
                        transformer.getTransformerActions().get(0).getActionType().toString(),
                        transformer))
                );
    }

    private static Submodel getTestSubmodel(Transformer transformer) {
        if(transformer.equals(factsTransformerCopy))
            return ansibleFactsSubmodel;
        return null;
    }

    @BeforeEach
    public void setup() {
        if(!isInitialAasSetupDone) {
            for(Submodel submodel : testSubmodels) {
                registerShellAndSubmodel(
                        this.aasRegistry,
                        this.aasRepository,
                        this.smRegistry,
                        this.smRepository,
                        testAas,
                        submodel
                );
            }
            isInitialAasSetupDone = true;
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

    @ParameterizedTest
    @MethodSource("getTestTransformers")
    @Order(10)
    public void testExecute(Transformer transformer) throws InterruptedException, DeserializationException, ApiException {
        // if submodel in job is not null, executor will take submodel instead of looking it up (strict-mode)
        TransformationJob job = new TransformationJob(
                UUID.randomUUID(),
                EXECUTE,
                transformer.getId(),
                getTestSubmodel(transformer).getId(),
                getTestSubmodel(transformer),
                null
        );
//        RedisTransformationJob redisJob = new RedisTransformationJob(job);

        assertNotEquals("", job.getSubmodel());

        // Assert Submodel count before pushing job:
        int expectedSubmodelCount = 1+testTransformers.indexOf(transformer);
        assertExpectedSubmodelCount(
                aasRegistry,
                aasRepository,
                smRepository,
                testAas.getId(),
                expectedSubmodelCount,
                expectedSubmodelCount
        );

//        Mockito
//                .when(jobApiClient.getNextJob())
//                .thenReturn(Mono.just(job))
//                .thenReturn(Mono.empty());
        redisJobProducer.pushJob(job);

//        redisJobClient.rightPushJob(redisJob);

        // Assert job count:
//        assertExpectedJobCount(redisJobConsumer, 0);

        // Assert Submodel count:
        expectedSubmodelCount++;
        assertExpectedSubmodelCount(
                aasRegistry,
                aasRepository,
                smRepository,
                testAas.getId(),
                expectedSubmodelCount,
                expectedSubmodelCount
        );
    }
}
