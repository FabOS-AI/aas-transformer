package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobReader;
import de.fhg.ipa.aas_transformer.clients.redis.RedisTransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJobAction;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import de.fhg.ipa.aas_transformer.transformation.TransformationExecutionService;
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

import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.RedisTestObjects.assertExpectedJobCount;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getDestinationSubmodelIdOfAnsibleFactsTransformer;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FirstTransformerThenAASExecutorIT {
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

    static Transformer factsTransformer = getAnsibleFactsTransformer();
    static DefaultAssetAdministrationShell shell = getSimpleShell("", "");
    static Submodel factsSubmodel = getAnsibleFactsSubmodel();

    // Mocks ManagementClient; Client return factsTransformer
    @TestConfiguration
    public static class TestConfig {
        @MockBean
        ManagementClient managementClient;

        @PostConstruct
        public void initMock(){
            Mockito
                    .when(managementClient.getAllTransformer())
                    .thenReturn(Flux.just(factsTransformer));
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

    public FirstTransformerThenAASExecutorIT() {
        this.aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        this.aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        this.smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        this.smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);
    }

    @Autowired
    TransformationExecutionServiceCache transformationExecutionServiceCache;

    RedisClient redisClient;

    @Autowired
    RedisJobReader redisJobReader;

    @PostConstruct
    public void init() {
        // Setup Redis Client:
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", Integer.parseInt(redisPort));
        connectionFactory.start();
        redisClient = new RedisClient(connectionFactory);
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
                null
        );

        assertEquals(1, aasRepository.getAas(shell.getId()).getSubmodels().size());
        assertExpectedSubmodelCount(aasRegistry, aasRepository, smRepository, shell.getId(), 1, 1);

        redisClient.leftPushJob(new RedisTransformationJob(createdJob));

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
            null
        );

        redisClient.leftPushJob(new RedisTransformationJob(deletedJob));

        int expectedSubmodelCount = 1;

        assertExpectedJobCount(redisJobReader,0);
        assertExpectedSubmodelCount(aasRegistry, aasRepository, smRepository, shell.getId(), expectedSubmodelCount, expectedSubmodelCount);
    }
}
