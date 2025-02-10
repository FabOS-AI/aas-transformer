package de.fhg.ipa.aas_transformer.service.management;

import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import com.hubspot.jinjava.interpret.InterpretException;
import com.hubspot.jinjava.interpret.TemplateSyntaxException;
import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class GetOrphanDestinationSubmodelTest {
    // region Testvars
    // Service Addresses:
    @LocalServerPort
    private  int transformerManagementPort;

    // Objects used inside Tests:
    private static Transformer factsTransformer = getAnsibleFactsTransformer();
    private static AssetAdministrationShell shell = getSimpleShell("","");
    private static Submodel factsSubmodel = getAnsibleFactsSubmodel();
    private static Submodel destinationSubmodel;
    static {
        try {
            destinationSubmodel = getDestinationSubmodel(factsSubmodel, factsTransformer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Autowired
    TransformerHandler transformerHandler;

    // Service Clients:
    @Autowired
    AasRegistry aasRegistry;
    @Autowired
    AasRepository aasRepository;
    @Autowired
    SubmodelRegistry smRegistry;
    @Autowired
    SubmodelRepository smRepository;

    // Mocks
    @MockBean
    TransformerJpaRepository transformerJpaRepository;
    @Autowired
    private TemplateRenderer templateRenderer;
    //endregion

    @BeforeEach
    void setUp() {
        for(Submodel submodel : List.of(destinationSubmodel)) {
            registerShellAndSubmodel(
                    aasRegistry,
                    aasRepository,
                    smRegistry,
                    smRepository,
                    shell,
                    submodel
            );
        }
    }

    @Test
    @Order(10)
    public void testRenderTemplateWithDestinationShellsMissingInContext() {
        Map<String, Object> context = templateRenderer.getTemplateContext(
                factsTransformer.getId(),
                List.of(),
                factsSubmodel
        );

        assertThrows(FatalTemplateErrorsException.class, () -> {
            templateRenderer.render(
                    "{{destinationShells:shell_id(DESTINATION_SHELLS, 0)}}/operating_system_copy",
                    context
            );
        });
    }


    @Test
    @Order(20)
    public void testGetOrphanSubmodelIdBasedOnAnsibleFactsTransformer() throws DeserializationException {
        Mockito
                .when(transformerJpaRepository.findById(Mockito.any(UUID.class)))
                .thenReturn(Mono.just(factsTransformer));

        List<String> orphanDestinationSubmodelIds = transformerHandler.getOrphanedDestinationSubmodelIds(
                factsTransformer.getId(),
                factsSubmodel
        );

        assertTrue(orphanDestinationSubmodelIds.size() == 1);


        assertTrue(
                orphanDestinationSubmodelIds.get(0).equals(destinationSubmodel.getId())
        );

        return;
    }
}
