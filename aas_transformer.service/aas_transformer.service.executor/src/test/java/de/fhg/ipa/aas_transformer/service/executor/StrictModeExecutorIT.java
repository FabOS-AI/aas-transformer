package de.fhg.ipa.aas_transformer.service.executor;

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
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;
import static de.fhg.ipa.aas_transformer.test.utils.RedisTestObjects.assertExpectedJobCount;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StrictModeExecutorIT {

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
    static boolean isInitialAasSetupDone = false;
    static AssetAdministrationShell testAas = getSimpleShell("", "");
    static Submodel ansibleFactsSubmodel = getAnsibleFactsSubmodel();
    static List<Submodel> testSubmodels = List.of(ansibleFactsSubmodel);
    static Transformer factsTransformerCopy = getAnsibleFactsTransformer();
    static List<Transformer> testTransformers = List.of(factsTransformerCopy);

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
                    .thenReturn(Flux.fromStream(testTransformers.stream()));
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

    public StrictModeExecutorIT() {
        this.aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        this.aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        this.smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        this.smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);
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

    @ParameterizedTest
    @MethodSource("getTestTransformers")
    @Order(10)
    public void testExecute(Transformer transformer) throws InterruptedException, DeserializationException, ApiException {
        // if submodel in job is not null, executor will take submodel instead of looking it up (strict-mode)
        TransformationJob job = new TransformationJob(
                EXECUTE,
                transformer.getId(),
                getTestSubmodel(transformer).getId(),
                getTestSubmodel(transformer)
        );
        RedisTransformationJob redisJob = new RedisTransformationJob(job);

        assertNotEquals("", redisJob.sourceSubmodel);

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

        redisClient.leftPushJob(redisJob);

        // Assert job count:
        assertExpectedJobCount(redisJobReader, 0);

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
