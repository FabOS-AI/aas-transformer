package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.model.SubmodelId;
import de.fhg.ipa.aas_transformer.model.SubmodelIdType;
import de.fhg.ipa.aas_transformer.model.TransformerActionTsReduceTakeEvery;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionTsReduceService;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;

@Disabled
public class TransformerActionTsReduceTakeEveryTest {

    // region Test Objects
    Submodel sourceSubmodel = getRandomTimeseriesSubmodel(5, 50);

    TransformerActionTsReduceTakeEvery transformerActionTsReduceTakeEvery = new TransformerActionTsReduceTakeEvery(
            5
    );

    TransformerActionTsReduceService transformerActionTsTakeEveryService = new TransformerActionTsReduceService(
            transformerActionTsReduceTakeEvery
    );

    Map<String, Object> context = new HashMap<>();
    // endregion

    @Test
    public void testExecute() {
        DefaultSubmodel destinationSubmodel = new DefaultSubmodel();
        try {
            transformerActionTsTakeEveryService.execute(
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
