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
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
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

import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.RedisTestObjects.assertExpectedJobCount;

@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultiTransformationExecutorIT {

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

    // Test triples:
    static List<List<Object>> triples;

    static {
        try {
            triples = getRandomAnsibleFactsTriples(5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Transformer testTransformer = (Transformer)triples.get(0).get(2);

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

    // Mocks ManagementClient; Client return factsTransformer
    @TestConfiguration
    public static class TestConfig {
        @MockBean
        ManagementClient managementClient;

        @PostConstruct
        public void initMock() {
            Mockito
                    .when(managementClient.getAllTransformer())
                    .thenReturn(Flux.just(testTransformer));
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

    public MultiTransformationExecutorIT() {
        this.aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        this.aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        this.smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        this.smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);
    }

    @Test
    @Order(10)
    public void testMultiTransformation() throws SerializationException, InterruptedException, DeserializationException, ApiException {
        // Register AAS objects from triples:
        registerAasObjectsFromTriples(aasRegistry, aasRepository, smRegistry, smRepository, triples);

        // Register jobs for transformation of triples:
        for(List<Object>triple : triples) {
            TransformationJob job = new TransformationJob(
                EXECUTE,
                testTransformer.getId(),
                ((Submodel)triple.get(1)).getId(),
                null
            );
            redisClient.leftPushJob(new RedisTransformationJob(job));
        }

        // Assert job count:
        assertExpectedJobCount(redisJobReader, 0);

        // Assert submodel count:
        for(List<Object>triple : triples) {
            assertExpectedSubmodelCount(
                    aasRegistry,
                    aasRepository,
                    smRepository,
                    ((AssetAdministrationShell) triple.get(0)).getId(),
                    triples.size()*2,
                    2
            );
        }
    }
}
