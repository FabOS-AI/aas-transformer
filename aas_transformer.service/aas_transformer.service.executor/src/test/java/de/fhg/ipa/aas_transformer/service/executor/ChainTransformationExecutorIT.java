package de.fhg.ipa.aas_transformer.service.executor;


import de.fhg.ipa.aas_transformer.clients.job_api.JobApiClient;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobConsumer;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.clients.redis.RedisTransformationJob;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.clients.redis.RedisTestObjects.assertExpectedJobCount;
import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.assertInternalSegmentsEqual;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChainTransformationExecutorIT extends AbstractIT {
    @MockBean
    MetricsClient metricsClient;
//    @MockBean
//    JobApiClient jobApiClient;
    @Autowired
    RedisJobProducer redisJobProducer;
    @Autowired
    RedisJobConsumer redisJobConsumer;

    // region Test vars
    // Test Transformer/AAS Objects:
    static AssetAdministrationShell testAas = getSimpleShell("", "");
    static Submodel timeseriesSubmodel = getRandomTimeseriesSubmodel(5, 50);
    static SubmodelId timeseriesSubmodelId = new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort());
    static TransformerActionTsAvg transformerActionTsAvg = new TransformerActionTsAvg(
            List.of("sensor0", "sensor1"),
            5
    );
    static TransformerActionTsReduceTakeEvery transformerActionTsReduceTakeEvery =
            new TransformerActionTsReduceTakeEvery(10);
    static Transformer oneStepTransformerAvg = new Transformer(
            UUID.randomUUID(),
            new Destination(new DestinationSubmodel(
                    "{{ submodel:idShort(SOURCE_SUBMODEL) }}_avg",
                    "{{ submodel:id(SOURCE_SUBMODEL) }}_avg")
            ),
            List.of(transformerActionTsAvg),
            List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, timeseriesSubmodelId)),
            false
    );
    static Transformer oneStepTransformerTakeEvery = new Transformer(
            UUID.randomUUID(),
            new Destination(new DestinationSubmodel(
                    timeseriesSubmodel.getIdShort()+"_take_every",
                    timeseriesSubmodel.getId()+"_take-every")
            ),
            List.of(transformerActionTsReduceTakeEvery),
            List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort()+"_avg"))),
            false
    );
    static Transformer twoStepTransformer = new Transformer(
            UUID.randomUUID(),
            new Destination(new DestinationSubmodel(
                    timeseriesSubmodel.getIdShort()+"_avg_take-every",
                    timeseriesSubmodel.getId()+"_avg_take-every")
            ),
            List.of(
                    new TransformerActionTsAvg(
                            List.of("sensor0", "sensor1"),
                            5
                    ),
                    new TransformerActionTsReduceTakeEvery(
                            10
                    )
            ),
            List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, timeseriesSubmodelId)),
            false
    );
    static List<TransformationJob> jobs = List.of(
            new TransformationJob(
                    EXECUTE,
                    oneStepTransformerAvg.getId(),
                    timeseriesSubmodel.getId(),
                    null,
                    null
            ),
            new TransformationJob(
                    EXECUTE,
                    oneStepTransformerTakeEvery.getId(),
                    timeseriesSubmodel.getId()+"_avg",
                    null,
                    null
            ),
            new TransformationJob(
                    EXECUTE,
                    twoStepTransformer.getId(),
                    timeseriesSubmodel.getId(),
                    null,
                    null
            )
    );
    private int currentJobReturnIndex = 0;
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
                    .thenReturn(Flux.just(oneStepTransformerAvg, oneStepTransformerTakeEvery, twoStepTransformer));
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

    @Test
    @Order(10)
    public void testExecute() throws InterruptedException, DeserializationException, ApiException {
        registerShellAndSubmodel(this.aasRegistry, this.aasRepository, this.smRegistry, this.smRepository, testAas, timeseriesSubmodel);

//        Mockito
//                .when(jobApiClient.getNextJob())
//                .thenAnswer(invocationOnMock -> this.returnNextJob());
        // Push transformation jobs to Redis:
        jobs.stream().forEach(redisJobProducer::pushJob);

        // Assert job count - make sure all transformations are done:
        assertExpectedJobCount(redisJobConsumer, 0);

        // Assert Submodel count:
        assertExpectedSubmodelCount(
                aasRegistry,
                aasRepository,
                smRepository,
                testAas.getId(),
                1+jobs.size(),
                1+jobs.size()
        );

        // Assert Submodel count - make sure all submodels are created:
        assertExpectedSubmodelCount(
                aasRegistry,
                aasRepository,
                smRepository,
                testAas.getId(),
                1+jobs.size(),
                1+jobs.size()
        );

        // assert 2 Transformations in one transformer produce same result as 2 separate transformers with same actions:
        Submodel submodelTakeEvery = smRepository.getSubmodel(oneStepTransformerTakeEvery.getDestination().getSubmodelDestination().getId());
        Submodel submodelAvgTakeEvery = smRepository.getSubmodel(twoStepTransformer.getDestination().getSubmodelDestination().getId());

        assertInternalSegmentsEqual(
                submodelTakeEvery,
                submodelAvgTakeEvery
        );
    }

//    private Mono<TransformationJob> returnNextJob() {
//        if( this.currentJobReturnIndex >= jobs.size())
//            return Mono.empty();
//        return Mono.just(jobs.get(this.currentJobReturnIndex++));
//    }
}
