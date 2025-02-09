package de.fhg.ipa.aas_transformer.test.system;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobReader;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasTransformerExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.eclipse.digitaltwin.basyx.aasregistry.client.model.AssetAdministrationShellDescriptor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.RedisTestObjects.assertExpectedJobCount;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(AasITExtension.class)
@ExtendWith(RedisExtension.class)
@ExtendWith(AasTransformerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class MultiTransformationSystemTest {

    //region Test Vars
    // Service Ports:
    static String transformerManagementPort = System.getProperty("aas_transformer.services.management.port");
    static String aasRegistryPort = System.getProperty("aas.aas-registry.port");
    static String aasRepositoryPort = System.getProperty("aas.aas-repository.port");
    static String smRegistryPort = System.getProperty("aas.submodel-registry.port");
    static String smRepositoryPort = System.getProperty("aas.submodel-repository.port");
    static String redisPort = System.getProperty("spring.data.redis.port");

    // Service Clients:
    static ManagementClient managementClient;
    static AasRegistry aasRegistry;
    static AasRepository aasRepository;
    static SubmodelRegistry smRegistry;
    static SubmodelRepository smRepository;
    static RedisJobReader redisJobReader;

    // Test triples:
    static List<List<Object>> triples;
    // endregion

    @BeforeAll
    static void beforeAll() throws Exception {
        triples = getRandomAnsibleFactsTriples(5);
        managementClient = new ManagementClient("http://localhost:" + transformerManagementPort);
        aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);

        LettuceConnectionFactory connFac = new LettuceConnectionFactory("localhost", Integer.parseInt(redisPort));
        connFac.start();
        redisJobReader = new RedisJobReader(connFac);
    }

    @Test
    @Order(10)
    public void testCreateMultiShellsAndSubmodels() throws DeserializationException, InterruptedException, ApiException {

        //TODO Fix transformer definition
        managementClient.createTransformer(
                (Transformer) triples.get(0).get(2),
                false
        ).block();

        registerAasObjectsFromTriples(aasRegistry, aasRepository, smRegistry, smRepository, triples);

        // Assert job count:
        assertExpectedJobCount(redisJobReader, 0);

        for(List<Object> triple : triples) {
            assertExpectedSubmodelCount(
                    aasRegistry,
                    aasRepository,
                    smRepository,
                    ((DefaultAssetAdministrationShell)triple.get(0)).getId(),
                    triples.size() * 2,
                    2
            );
        }
    }

    @Test
    @Order(20)
    public void testDeleteMultiSubmodels() throws InterruptedException, DeserializationException, ApiException {
        deleteSourceSubmodelsFromTriples(aasRegistry, aasRepository, smRepository, triples);

        for(List<Object> triple : triples) {
            assertExpectedSubmodelCount(
                    aasRegistry,
                    aasRepository,
                    smRepository,
                    ((DefaultAssetAdministrationShell)triple.get(0)).getId(),
                    0,
                    0
            );
        }
    }
}
