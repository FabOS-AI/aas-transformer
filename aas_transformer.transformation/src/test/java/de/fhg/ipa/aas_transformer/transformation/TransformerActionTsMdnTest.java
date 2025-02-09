package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.model.SubmodelId;
import de.fhg.ipa.aas_transformer.model.SubmodelIdType;
import de.fhg.ipa.aas_transformer.model.TransformerActionTsAvg;
import de.fhg.ipa.aas_transformer.model.TransformerActionTsMdn;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionTsAvgService;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionTsMdnService;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;

@Disabled
public class TransformerActionTsMdnTest {

    // region Test Objects
    Submodel sourceSubmodel = getRandomTimeseriesSubmodel(5, 50);

    TransformerActionTsMdn transformerActionTsMdn = new TransformerActionTsMdn(
            List.of("sensor0", "sensor1"),
            5
    );

    TransformerActionTsMdnService transformerActionTsMdnService = new TransformerActionTsMdnService(
            transformerActionTsMdn
    );

    Map<String, Object> context = new HashMap<>();
    // endregion

    @Test
    public void testExecute() {
        DefaultSubmodel destinationSubmodel = new DefaultSubmodel();
        try {
            transformerActionTsMdnService.execute(
                    sourceSubmodel,
                    destinationSubmodel,
                    context,
                    true
            );

            // TODO: Assert transformation result being the median of the values
        // because smRepo is null:
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
