package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.model.SubmodelId;
import de.fhg.ipa.aas_transformer.model.SubmodelIdType;
import de.fhg.ipa.aas_transformer.model.TransformerActionTsReduceDropEvery;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionTsReduceService;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;

@Disabled
public class TransformerActionTsReduceDropEveryTest {

    // region Test Objects
    Submodel sourceSubmodel = getRandomTimeseriesSubmodel(5, 50);

    TransformerActionTsReduceDropEvery transformerActionTsReduceDropEvery = new TransformerActionTsReduceDropEvery(
            5
    );

    TransformerActionTsReduceService transformerActionTsDropEveryService = new TransformerActionTsReduceService(
            transformerActionTsReduceDropEvery
    );

    Map<String, Object> context = new HashMap<>();
    // endregion

    @Test
    public void testExecute() {
        DefaultSubmodel destinationSubmodel = new DefaultSubmodel();
        try {
            transformerActionTsDropEveryService.execute(
                    sourceSubmodel,
                    destinationSubmodel,
                    context,
                    true
            );
            // TODO: Assert transformation result having reduced record count
        // because smRepo is null:
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
