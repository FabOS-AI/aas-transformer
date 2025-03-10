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
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(AasITExtension.class)
@ExtendWith(RedisExtension.class)
@ExtendWith(AasTransformerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class FirstAASThenTransformerSystemTest extends AbstractSystemTest {

    // Test objects:
    static DefaultAssetAdministrationShell shell = getSimpleShell("", "");
    static Submodel factsSubmodel = getAnsibleFactsSubmodel();

    @Test
    @Order(10)
    public void createShellAndFactsSubmodelExpectOneSubmodel() throws DeserializationException, InterruptedException, ApiException {
        registerShellAndSubmodel(
                aasRegistry,
                aasRepository,
                smRegistry,
                smRepository,
                shell,
                factsSubmodel
        );
        int expectedSubmodelCount = 1;

        assertExpectedSubmodelCount(
                aasRegistry,
                aasRepository,
                smRepository,
                shell.getId(),
                expectedSubmodelCount,
                expectedSubmodelCount
        );
    }

    @Test
    @Order(20)
    public void createTransformerExecuteTrueExpectTwoSubmodels() throws DeserializationException, InterruptedException, ApiException {
        managementClient.createTransformer(
                getAnsibleFactsTransformer(false),
                true
        ).block();


        int expectedTransformerCount = 1;

        assertEquals(
                expectedTransformerCount,
                managementClient
                        .getAllTransformer()
                        .collectList()
                        .block()
                        .size()
        );

        int expectedSubmodelCount = 2;

        assertExpectedSubmodelCount(
                aasRegistry,
                aasRepository,
                smRepository,
                shell.getId(),
                expectedSubmodelCount,
                expectedSubmodelCount
        );
    }

    @Test
    @Order(30)
    public void deleteTransformerWithCleanupExpectOneSubmodel() throws DeserializationException, InterruptedException, ApiException {
        List<Transformer> transformerList = managementClient.getAllTransformer().collectList().block();
        int expectedTransformerCount = 1;

        assertEquals(
                expectedTransformerCount,
                transformerList.size()
        );

        managementClient.deleteTransformer(transformerList.get(0).getId(), true).block();

        int tryCount = 0;
        int tryLimit = 100;
        int sleepInMs = 100;
        expectedTransformerCount = 0;

        while(tryCount <= tryLimit || managementClient.getAllTransformer().collectList().block().size() != expectedTransformerCount) {
            tryCount++;
            sleep(sleepInMs);
        }

        assertEquals(
                expectedTransformerCount,
                managementClient.getAllTransformer().collectList().block().size()
        );

        int expectedSubmodelCount = 1;

        assertExpectedSubmodelCount(
                aasRegistry,
                aasRepository,
                smRepository,
                shell.getId(),
                expectedSubmodelCount,
                expectedSubmodelCount
        );
    }
}
