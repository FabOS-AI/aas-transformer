package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.model.SubmodelId;
import de.fhg.ipa.aas_transformer.model.SubmodelIdType;
import de.fhg.ipa.aas_transformer.model.TransformerActionTsAvg;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionTsAvgService;
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;

@Disabled
public class TransformerActionTsAvgTest {

    // region Test Objects
    Submodel sourceSubmodel = getRandomTimeseriesSubmodel(5, 50);

    TransformerActionTsAvg transformerActionTsAvg = new TransformerActionTsAvg(
            List.of("sensor0", "sensor1"),
            5
    );

    TransformerActionTsAvgService transformerActionTsAvgService = new TransformerActionTsAvgService(
            transformerActionTsAvg
    );

    Map<String, Object> context = new HashMap<>();
    // endregion

    @Test
    public void testExecute() {

        DefaultSubmodel destinationSubmodel = new DefaultSubmodel();

        transformerActionTsAvgService.execute(
                sourceSubmodel,
                destinationSubmodel,
                context,
                true
        );
    }
}
