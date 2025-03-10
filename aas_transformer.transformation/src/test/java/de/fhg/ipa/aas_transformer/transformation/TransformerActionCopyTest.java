package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionCopyService;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.getRandomAnsibleFactsSubmodel;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;

@Disabled
public class TransformerActionCopyTest {
    Submodel sourceSubmodel = getRandomAnsibleFactsSubmodel();
    Transformer transformer = getAnsibleFactsTransformer(true);

    TransformerActionCopyService transformerActionCopyService = new TransformerActionCopyService(
            (TransformerActionCopy) transformer.getTransformerActions().get(0)
    );

    Map<String, Object> context = new HashMap<>();    @Test
    public void testExecute() {

        DefaultSubmodel destinationSubmodel = new DefaultSubmodel();

        transformerActionCopyService.execute(
                sourceSubmodel,
                destinationSubmodel,
                context,
                true
        );
    }


}
