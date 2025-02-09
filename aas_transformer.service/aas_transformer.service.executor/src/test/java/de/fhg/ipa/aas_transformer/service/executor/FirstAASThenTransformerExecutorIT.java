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
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.eclipse.digitaltwin.basyx.aasregistry.client.model.AssetAdministrationShellDescriptor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.FileNotFoundException;

import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.getAnsibleFactsSubmodel;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.getSimpleShell;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getDestinationSubmodelIdOfAnsibleFactsTransformer;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FirstAASThenTransformerExecutorIT {
    // Service Ports:
    static String aasRegistryPort = System.getProperty("aas.aas-registry.port");
    static String aasRepositoryPort = System.getProperty("aas.aas-repository.port");
    static String smRegistryPort = System.getProperty("aas.submodel-registry.port");
    static String smRepositoryPort = System.getProperty("aas.submodel-repository.port");
    String redisPort = System.getProperty("spring.data.redis.port");

    // AAS Service Clients:
    static AasRegistry aasRegistry;
    static AasRepository aasRepository;
    static SubmodelRegistry smRegistry;
    static SubmodelRepository smRepository;

    static Submodel factsSubmodel = getAnsibleFactsSubmodel();
    static Transformer factsTransformer = getAnsibleFactsTransformer();

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

    @Autowired
    TransformationExecutionServiceCache transformationExecutionServiceCache;

    @Autowired
    RedisJobReader redisJobReader;

    RedisClient redisClient;

    @BeforeAll
    static void beforeAll() throws ApiException {
        // Init AAS Clients:
        aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);

        // Create AAS and Facts Submodel

        aasRepository.createOrUpdateAas(shell);
        aasRepository.addSubmodelReferenceToAas(shell.getId(), factsSubmodel);
        smRepository.createOrUpdateSubmodel(factsSubmodel);
        aasRegistry.addSubmodelDescriptorToAas(
                shell.getId(),
                smRegistry.findSubmodelDescriptor(factsSubmodel.getId()).get()
        );
    }

    @PostConstruct
    public void init() {
        // Setup Redis Client:
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", Integer.parseInt(redisPort));
        connectionFactory.start();
        redisClient = new RedisClient(connectionFactory);
    }

    private void assertExpectedJobCount(int expectedJobCount) throws InterruptedException {
        int tryCount = 0;
        int maxTries = 1000;
        int sleepInMs = 100;

        while(tryCount <= maxTries && this.redisJobReader.getTotalJobCount() != expectedJobCount) {
            sleep(sleepInMs);
            tryCount++;
        }

        assertEquals(
                expectedJobCount,
                this.redisJobReader.getTotalJobCount()
        );
    }

    private void assertExpectedSubmodelCount(int expectedSubmodelCount) throws DeserializationException, InterruptedException, ApiException {
        int tryCount = 0;
        int maxTries = 1000;
        int sleepInMs = 100;

        while(tryCount <= maxTries && this.smRepository.getAllSubmodels().size() != expectedSubmodelCount) {
            sleep(sleepInMs);
            tryCount++;
        }

        assertEquals(
                expectedSubmodelCount,
                this.smRepository.getAllSubmodels().size()
        );

        // Assert Submodel References/Descriptors In Shell(descriptor):
        AssetAdministrationShell testShell = aasRepository.getAas(shell.getId());
        assertEquals(expectedSubmodelCount, testShell.getSubmodels().size());

        AssetAdministrationShellDescriptor testShellDescriptor = aasRegistry.getAasDescriptor(shell.getId()).get();
        assertEquals(expectedSubmodelCount, testShellDescriptor.getSubmodelDescriptors().size());
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
        TransformationJob createdJob = new TransformationJob(
                EXECUTE,
                factsTransformer.getId(),
                factsSubmodel.getId(),
                null
        );

        assertExpectedSubmodelCount(1);

        redisClient.leftPushJob(new RedisTransformationJob(createdJob));

        assertExpectedJobCount(0);
        assertExpectedSubmodelCount(2);
    }

    @Test
    @Order(30)
    public void testPushDeleteTransformationJobExpectOneSubmodel() throws DeserializationException, FileNotFoundException, SerializationException, InterruptedException, ApiException {
        String destinationSubmodelId = getDestinationSubmodelIdOfAnsibleFactsTransformer(
                factsTransformer,
                shell.getId()
        );

        TransformationJob deleteJob = new TransformationJob(
                TransformationJobAction.DELETE,
                null,
                destinationSubmodelId,
                null
        );

        assertEquals(
                2,
                this.smRepository.getAllSubmodels().size()
        );

        redisClient.leftPushJob(new RedisTransformationJob(deleteJob));

        assertExpectedJobCount(0);
        assertExpectedSubmodelCount(1);
    }
}
