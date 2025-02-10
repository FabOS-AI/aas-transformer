package de.fhg.ipa.aas_transformer.service.management;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobReader;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.aas_transformer.model.TransformerActionType;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.RedisTestObjects.assertExpectedJobCount;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class JobPushTest {
    static AasRegistry aasRegistry;
    static AasRepository aasRepository;
    static SubmodelRegistry smRegistry;
    static SubmodelRepository smRepository;
    @Autowired
    private RedisJobReader redisJobReader;

    private static List<List<Object>> triple = getRandomAnsibleFactsTriples(1);

    @Autowired
    private TransformerHandler transformerHandler;

    @BeforeAll
    static void beforeAll() throws FileNotFoundException, DeserializationException {
        // Create Submodel that shall be effected by transformer created during test:
        String aasRegistryPort = System.getProperty("aas.aas-registry.port");
        String aasRepositoryPort = System.getProperty("aas.aas-repository.port");
        String smRegistryPort = System.getProperty("aas.submodel-registry.port");
        String smRepositoryPort = System.getProperty("aas.submodel-repository.port");
        aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);

        registerShellAndSubmodel(
            aasRegistry,
            aasRepository,
            smRegistry,
            smRepository,
            (AssetAdministrationShell) triple.get(0).get(0),
            (Submodel) triple.get(0).get(1)
        );
        registerShellAndSubmodel(
                aasRegistry,
                aasRepository,
                smRegistry,
                smRepository,
                (AssetAdministrationShell) triple.get(0).get(0),
                (Submodel) triple.get(0).get(3)
        );
    }

    @Test
    @Order(10)
    public void createTransformerExpectJobPush() throws InterruptedException {
        transformerHandler
                .createOrUpdateTransformer(getAnsibleFactsTransformer(), true)
                .block();

        assertEquals(1, redisJobReader.getWaitingJobCount());
    }

    @Test
    @Order(20)
    public void getDestinationSubmodelIdsOfTransformerExpectOne() {
        Transformer t = transformerHandler.getAllTransformer().blockFirst();
        List<String> destinationSubmodelIds = transformerHandler.getDestinationSubmodelIds(t.getId());

        assertEquals(1, destinationSubmodelIds.size());
    }

    @Test
    @Order(30)
    public void modifyTransformerExpectJobPush() throws InterruptedException {
        int waitingJobCount0 = redisJobReader.getWaitingJobCount();
        Transformer t = transformerHandler.getAllTransformer().blockFirst();

        t.setTransformerActions(
                t.getTransformerActions()
                        .stream()
                        .filter(a -> a.getActionType().equals(TransformerActionType.COPY))
                        .map(a -> {
                            ((TransformerActionCopy) a).setDestinationSubmodelElement(((TransformerActionCopy) a).getDestinationSubmodelElement() + "_new");
                            return a;
                        })
                        .collect(Collectors.toList())
        );
        transformerHandler.createOrUpdateTransformer(t, true).block();

        assertExpectedJobCount(redisJobReader, waitingJobCount0);
    }

    @Test
    @Order(40)
    public void deleteTransformerExpectJobPush() throws InterruptedException {
        int waitingJobCount0 = redisJobReader.getWaitingJobCount();

        transformerHandler.deleteTransformer(
                transformerHandler
                        .getAllTransformer()
                        .blockFirst()
                        .getId(),
                true
        );

        assertExpectedJobCount(redisJobReader, waitingJobCount0+1);
    }
}
