package de.fhg.ipa.aas_transformer.service.management;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.service.management.converter.modelmapper.TransformerToTransformerDTOListenerConverter;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.aas.AasUtils.createClone;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.registerShellAndSubmodel;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.*;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class TransformerRestControllerOrphanTest {
    @RegisterExtension
    static AasITExtension aasITExtension = new AasITExtension(false);
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
    private final static Transformer factsTransformer = getAnsibleFactsTransformer();
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
