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
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.io.FileNotFoundException;
import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.RedisTestObjects.assertExpectedJobCount;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static org.junit.Assert.assertEquals;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(AasITExtension.class)
@ExtendWith(RedisExtension.class)
@ExtendWith(AasTransformerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class FirstTransformerThenAASSystemTest {
    // Service Addresses:
    String transformerManagementPort = System.getProperty("aas_transformer.services.management.port");
    String aasRegistryPort = System.getProperty("aas.aas-registry.port");
    String aasRepositoryPort = System.getProperty("aas.aas-repository.port");
    String smRegistryPort = System.getProperty("aas.submodel-registry.port");
    String smRepositoryPort = System.getProperty("aas.submodel-repository.port");

    // Clients:
    ManagementClient managementClient;
    AasRegistry aasRegistry;
    AasRepository aasRepository;
    SubmodelRegistry smRegistry;
    SubmodelRepository smRepository;
    RedisJobReader redisJobReader;

    // Test objects:
    static DefaultAssetAdministrationShell shell = getSimpleShell("", "");
    static Submodel factsSubmodel = getAnsibleFactsSubmodel();

    public FirstTransformerThenAASSystemTest() {
        this.managementClient = new ManagementClient("http://localhost:" + transformerManagementPort);
        this.aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        this.aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        this.smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        this.smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);
        LettuceConnectionFactory connFac = new LettuceConnectionFactory(
                System.getProperty("spring.data.redis.host"),
                Integer.parseInt(System.getProperty("spring.data.redis.port"))
        );
        connFac.start();
        this.redisJobReader = new RedisJobReader(connFac);
    }

    @Test
    @Order(10)
    public void getTransformerExpectNone() {
        List<Transformer> transformers = managementClient
                .getAllTransformer()
                .collectList()
                .block();

        assertEquals(
                0,
                transformers.size()
        );
    }

    @Test
    @Order(20)
    public void createAnsibleFactsTransformerExpectOne() throws InterruptedException {
        managementClient.createTransformer(
                getAnsibleFactsTransformer(),
                false
        ).block();

        List<Transformer> transformers = managementClient
                .getAllTransformer()
                .collectList()
                .block();

        assertEquals(
                1,
                transformers.size()
        );
    }

    @Test
    @Order(30)
    public void createShellAndFactsSubmodelExpectTwoSubmodels() throws FileNotFoundException, DeserializationException, InterruptedException, ApiException {
        // Assert job count is 0:
        assertEquals(
                0,
                redisJobReader.getTotalJobCount()
        );
        registerShellAndSubmodel(
                aasRegistry,
                aasRepository,
                smRegistry,
                smRepository,
                shell,
                factsSubmodel
        );

        assertExpectedJobCount(redisJobReader, 0);

        assertExpectedSubmodelCount(
                aasRegistry,
                aasRepository,
                smRepository,
                shell.getId(),
                2,
                2
        );
    }

    @Test
    @Order(40)
    public void deleteTransformerWithCleanupExpectOneSubmodel() {
        // TODO: implement
    }
}
