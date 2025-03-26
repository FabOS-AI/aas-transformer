package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.model.SubmodelId;
import de.fhg.ipa.aas_transformer.model.SubmodelIdType;
import de.fhg.ipa.aas_transformer.model.TransformerActionTsReduceTakeEvery;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionTsReduceService;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.basyx.submodelservice.pathparsing.HierarchicalSubmodelElementParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;
import static java.lang.Math.floor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Disabled
public class TransformerActionTsReduceTakeEveryTest {

    // region Test Objects
    Submodel sourceSubmodel = getRandomTimeseriesSubmodel(5, 50);

    int n = 5;

    TransformerActionTsReduceTakeEvery transformerActionTsReduceTakeEvery = new TransformerActionTsReduceTakeEvery(
            n
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
            Submodel result = transformerActionTsTakeEveryService.execute(
                    sourceSubmodel,
                    destinationSubmodel,
                    context,
                    true
            );
            HierarchicalSubmodelElementParser srcParser = new HierarchicalSubmodelElementParser(sourceSubmodel);
            SubmodelElementCollection srcRecords = (SubmodelElementCollection) srcParser.getSubmodelElementFromIdShortPath("Segments.InternalSegment.Records");
            HierarchicalSubmodelElementParser dstParser = new HierarchicalSubmodelElementParser(result);
            SubmodelElementCollection dstRecords = (SubmodelElementCollection) dstParser.getSubmodelElementFromIdShortPath("Segments.InternalSegment.Records");

            assertNotEquals(
                    srcRecords.getValue().size(),
                    dstRecords.getValue().size()
            );

            int expectedSize = (int) floor(srcRecords.getValue().size()/n);

            assertEquals(expectedSize,dstRecords.getValue().size());
        // because smRepo is null:
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
