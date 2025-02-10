package de.fhg.ipa.aas_transformer.service.executor;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobReader;
import de.fhg.ipa.aas_transformer.clients.redis.RedisTransformationJob;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.aas.TimeseriesUtils.getInternalSegmentSubmodelElement;
import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.assertExpectedSubmodelCount;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.assertInternalSegmentsEqual;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;
import static de.fhg.ipa.aas_transformer.test.utils.RedisTestObjects.assertExpectedJobCount;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChainTransformationExecutorIT {
    // region Test vars
    // Service Ports:
    String aasRegistryPort = System.getProperty("aas.aas-registry.port");
    String aasRepositoryPort = System.getProperty("aas.aas-repository.port");
    String smRegistryPort = System.getProperty("aas.submodel-registry.port");
    String smRepositoryPort = System.getProperty("aas.submodel-repository.port");
    String redisPort = System.getProperty("spring.data.redis.port");

    // AAS Service Clients:
    AasRegistry aasRegistry;
    AasRepository aasRepository;
    SubmodelRegistry smRegistry;
    SubmodelRepository smRepository;

    // Test Transformer/AAS Objects:
    static AssetAdministrationShell testAas = getSimpleShell("", "");
    static Submodel timeseriesSubmodel = getRandomTimeseriesSubmodel(5, 50);
    static SubmodelId timeseriesSubmodelId = new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort());
    static TransformerActionTsAvg transformerActionTsAvg = new TransformerActionTsAvg(
//            timeseriesSubmodelId,
            List.of("sensor0", "sensor1"),
            5
    );
    static TransformerActionTsReduceTakeEvery transformerActionTsReduceTakeEvery = new TransformerActionTsReduceTakeEvery(
//            new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort()+"_avg"),
            10
    );
    static Transformer oneStepTransformerAvg = new Transformer(
            UUID.randomUUID(),
            new Destination(new DestinationSubmodel(
                    "{{ submodel:idShort(SOURCE_SUBMODEL) }}_avg",
                    "{{ submodel:id(SOURCE_SUBMODEL) }}_avg")
            ),
            List.of(transformerActionTsAvg),
            List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, timeseriesSubmodelId))
    );
    static Transformer oneStepTransformerTakeEvery = new Transformer(
            UUID.randomUUID(),
            new Destination(new DestinationSubmodel(
                    timeseriesSubmodel.getIdShort()+"_take_every",
                    timeseriesSubmodel.getId()+"_take-every")
            ),
            List.of(transformerActionTsReduceTakeEvery),
            List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort()+"_avg")))
    );
    static Transformer twoStepTransformer = new Transformer(
            UUID.randomUUID(),
            new Destination(new DestinationSubmodel(
                    timeseriesSubmodel.getIdShort()+"_avg_take-every",
                    timeseriesSubmodel.getId()+"_avg_take-every")
            ),
            List.of(
                    new TransformerActionTsAvg(
//                            timeseriesSubmodelId,
                            List.of("sensor0", "sensor1"),
                            5
                    ),
                    new TransformerActionTsReduceTakeEvery(
//                            timeseriesSubmodelId,
                            10
                    )
            ),
            List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, timeseriesSubmodelId))
    );
    static List<TransformationJob> jobs = List.of(
            new TransformationJob(
                    EXECUTE,
                    oneStepTransformerAvg.getId(),
                    timeseriesSubmodel.getId(),
                    null
            ),
            new TransformationJob(
                    EXECUTE,
                    oneStepTransformerTakeEvery.getId(),
                    timeseriesSubmodel.getId()+"_avg",
                    null
            ),
            new TransformationJob(
                    EXECUTE,
                    twoStepTransformer.getId(),
                    timeseriesSubmodel.getId(),
                    null
            )
    );

    @Autowired
    RedisJobReader redisJobReader;

    RedisClient redisClient;
    // endregion

    @PostConstruct
    public void init() {
        // Setup Redis Client:
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", Integer.parseInt(redisPort));
        connectionFactory.start();
        redisClient = new RedisClient(connectionFactory);
    }

    // Mocks ManagementClient; Client return testTransformer
    @TestConfiguration
    public static class TestConfig {
        @MockBean
        ManagementClient managementClient;

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
                    .thenReturn(Flux.empty());
            Mockito
                    .when(managementClient.getTransformerChangeEventDTOListenerStream())
                    .thenReturn(Flux.empty());
        }
    }

    public ChainTransformationExecutorIT() {
        this.aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        this.aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        this.smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        this.smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);
    }

    @Test
    @Order(10)
    public void testExecute() throws InterruptedException, DeserializationException, ApiException {
        registerShellAndSubmodel(this.aasRegistry, this.aasRepository, this.smRegistry, this.smRepository, testAas, timeseriesSubmodel);

        // Assert Submodel count:
        assertExpectedSubmodelCount(
                aasRegistry,
                aasRepository,
                smRepository,
                testAas.getId(),
                1,
                1
        );

        // Push transformation jobs to Redis:
        jobs.stream().forEach(job -> {
            redisClient.leftPushJob(new RedisTransformationJob(job));
        });

        // Assert job count - make sure all transformations are done:
        assertExpectedJobCount(redisJobReader, 0);

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
}
