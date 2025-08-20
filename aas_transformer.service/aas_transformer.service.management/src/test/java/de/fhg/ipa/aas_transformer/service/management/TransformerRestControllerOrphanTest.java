package de.fhg.ipa.aas_transformer.service.management;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.aas.AasUtils.createClone;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getDestinationSubmodelIdOfAnsibleFactsTransformer;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class TransformerRestControllerOrphanTest {
    // region Test Vars
    // Service Addresses:
    @LocalServerPort
    private  int transformerManagementPort;

    // Clients:
    private static ManagementClient managementClient;
    @Autowired
    private AasRegistry aasRegistry;
    @Autowired
    private AasRepository aasRepository;
    @Autowired
    private SubmodelRegistry submodelRegistry;
    @Autowired
    private SubmodelRepository submodelRepository;

    // Objects used inside Tests:
    private final static Transformer factsTransformer = getAnsibleFactsTransformer(false);
    private final static AssetAdministrationShell shell = getSimpleShell("", "");
    private final static Submodel factsSubmodel = getAnsibleFactsSubmodel();
    private final static Submodel operatingSystemSubmodel;
    private final static Submodel operatingSystemSubmodelMod;
    // endregion

    static {
        try {
            operatingSystemSubmodel = getDestinationSubmodel(factsSubmodel, factsTransformer);
            operatingSystemSubmodel.setId(
                    getDestinationSubmodelIdOfAnsibleFactsTransformer(
                            factsTransformer,
                            shell.getId()
                    )
            );
            operatingSystemSubmodelMod = createClone(operatingSystemSubmodel);
            operatingSystemSubmodelMod.setId(UUID.randomUUID().toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void setUp() {
        if(managementClient == null) {
            this.managementClient = new ManagementClient("http://localhost:" + transformerManagementPort);
            this.managementClient.createTransformer(factsTransformer, false).block();
            registerShellAndSubmodel(
                    aasRegistry,
                    aasRepository,
                    submodelRegistry,
                    submodelRepository,
                    shell,
                    operatingSystemSubmodel
            );
        }

    }

    @Test
    @Order(10)
    public void testSendRequestToGetOrphansEndpointExpectOperatingSystemSubmodelId() {
        List<String> result = managementClient.getOrphanedDestinationSubmodels(
                factsTransformer.getId(),
                factsSubmodel
        ).block();

        assertTrue(
            result.contains(operatingSystemSubmodel.getId())
        );
    }

    @Test
    @Order(20)
    public void testGetOrphansWithSubmodelsHavingSameIdShortExpectEmptyList() {
        registerShellAndSubmodel(
                aasRegistry,
                aasRepository,
                submodelRegistry,
                submodelRepository,
                shell,
                operatingSystemSubmodelMod
        );

        List<String> result = managementClient.getOrphanedDestinationSubmodels(
                factsTransformer.getId(),
                factsSubmodel
        ).block();

        assertTrue(
            result.isEmpty()
        );
    }
}
