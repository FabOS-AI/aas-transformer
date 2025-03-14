package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionCopyService;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.basyx.submodelservice.pathparsing.HierarchicalSubmodelElementParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static de.fhg.ipa.aas_transformer.aas.AasUtils.createClone;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.getRandomAnsibleFactsSubmodel;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class TransformerActionCopyTest {
    Submodel sourceSubmodel = getRandomAnsibleFactsSubmodel();
    Submodel sourceSubmodelClone = createClone(sourceSubmodel);
    Transformer transformer = getAnsibleFactsTransformer(true);
    TransformerActionCopyService transformerActionCopyService = new TransformerActionCopyService(
            (TransformerActionCopy) transformer.getTransformerActions().get(0)
    );
    Map<String, String> submodelElementIdMapping = Map.of("distribution","distribution_new");
    Map<String, Object> context = new HashMap<>();

    @Test
    public void testExecute() {
        DefaultSubmodel destinationSubmodel = new DefaultSubmodel();
        Submodel transformationResult = transformerActionCopyService.execute(
                sourceSubmodel,
                destinationSubmodel,
                context,
                true
        );
        HierarchicalSubmodelElementParser sourceParser = new HierarchicalSubmodelElementParser(sourceSubmodelClone);
        HierarchicalSubmodelElementParser destinationParser = new HierarchicalSubmodelElementParser(transformationResult);
        submodelElementIdMapping.forEach((sourceSmeIdShort, destinationSmeIdShort) -> {
            String sourceValue = ((DefaultProperty) sourceParser.getSubmodelElementFromIdShortPath(sourceSmeIdShort)).getValue();
            String destinationValue = ((DefaultProperty) destinationParser.getSubmodelElementFromIdShortPath(destinationSmeIdShort)).getValue();
            assertEquals(sourceValue, destinationValue);
        });
    }


}
