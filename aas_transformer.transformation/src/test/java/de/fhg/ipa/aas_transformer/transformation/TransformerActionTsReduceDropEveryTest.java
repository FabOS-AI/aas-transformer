package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.model.actions.TransformerActionTsReduceDropEvery;
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
public class TransformerActionTsReduceDropEveryTest {

    // region Test Objects
    Submodel sourceSubmodel = getRandomTimeseriesSubmodel(5, 50);

    int n = 5;

    TransformerActionTsReduceDropEvery transformerActionTsReduceDropEvery = new TransformerActionTsReduceDropEvery(n);

    TransformerActionTsReduceService transformerActionTsDropEveryService = new TransformerActionTsReduceService(
            transformerActionTsReduceDropEvery
    );

    Map<String, Object> context = new HashMap<>();
    // endregion

    @Test
    public void testExecute() {
        DefaultSubmodel destinationSubmodel = new DefaultSubmodel();
        try {
            Submodel result = transformerActionTsDropEveryService.execute(
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

            int expectedSize = (int) (srcRecords.getValue().size()-(floor(srcRecords.getValue().size()/n)));

            assertEquals(expectedSize,dstRecords.getValue().size());
        // because smRepo is null:
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
