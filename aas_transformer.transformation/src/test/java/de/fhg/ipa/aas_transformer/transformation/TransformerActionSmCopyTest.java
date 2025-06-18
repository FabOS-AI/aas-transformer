package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.model.TransformerActionSmCopy;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionSmCopyService;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

@Disabled
@SpringBootTest(classes = {
        SubmodelRegistry.class,
        SubmodelRepository.class,
        TemplateRenderer.class
})
@Profile("test")
public class TransformerActionSmCopyTest {
    @MockBean
    SubmodelRegistry submodelRegistry;

    @MockBean
    SubmodelRepository submodelRepository;

    @MockBean
    TemplateRenderer templateRenderer;

    @Test
    public void testExecute() {
        String smIdOfToBeCopiedSubmodel = "testSmId";
        TransformerActionSmCopyService transformerActionSmCopyService = new TransformerActionSmCopyService(
                submodelRegistry,
                submodelRepository,
                templateRenderer,
                new TransformerActionSmCopy("testSmId")
        );

        Mockito
                .when(templateRenderer.render(Mockito.anyString(), Mockito.any()))
                .thenReturn(smIdOfToBeCopiedSubmodel);

        Submodel transformationResult;
        try (MockedStatic<SubmodelRepository> mockedStatic = mockStatic(SubmodelRepository.class)) {
            mockedStatic
                    .when(() -> SubmodelRepository.getExtSubmodel((SubmodelRegistry) Mockito.any(), Mockito.anyString()))
                    .thenReturn(getSubmodelToBeCopied());

            transformationResult = transformerActionSmCopyService.execute(
                    new DefaultSubmodel(), // sourceSubmodel
                    getInputSubmodel(), // intermediateResult
                    new HashMap<>(),
                    true
            );
        }

        // Verify id and idShort of the transformation result
        assertEquals(getInputSubmodel().getId(), transformationResult.getId());
        assertEquals(getInputSubmodel().getIdShort(), transformationResult.getIdShort());

        // Verify that the transformation result contains the properties from the submodel to be copied
        assertEquals(
                getSubmodelToBeCopied().getSubmodelElements(),
                transformationResult.getSubmodelElements()
        );
    }

    private Submodel getInputSubmodel() {
        Submodel submodel = new DefaultSubmodel();
        submodel.setId("inputSubmodelId");
        submodel.setIdShort("inputSubmodelIdShort");
        return submodel;
    }

    private Submodel getSubmodelToBeCopied() {
        Submodel submodel = new DefaultSubmodel();
        submodel.setId("submodelToBeCopiedId");
        submodel.setIdShort("submodelToBeCopiedId");

        submodel.setSubmodelElements(List.of(
                getProperty("prop_a"),
                getProperty("prop_b"),
                getProperty("prop_c")
        ));

        return submodel;
    }

    private DefaultProperty getProperty(String idShort) {
        DefaultProperty property = new DefaultProperty();
        property.setIdShort(idShort);
        property.setValue(idShort + "_value");
        return property;
    }
}
