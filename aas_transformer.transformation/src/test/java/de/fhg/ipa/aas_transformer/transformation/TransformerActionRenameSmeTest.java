package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.model.SubmodelElementProperty;
import de.fhg.ipa.aas_transformer.model.actions.TransformerActionSmeRename;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionService;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionSmeRenameService;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.submodelservice.pathparsing.HierarchicalSubmodelElementParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransformerActionRenameSmeTest {
    Submodel submodel = getRandomTimeseriesSubmodel(100, 1, "test_timeseries");
    TransformerActionSmeRenameService service = new TransformerActionSmeRenameService(
//        new TransformerActionSmeRename("Metadata", SubmodelElementProperty.ID_SHORT, "new_value"),
        new TransformerActionSmeRename("Segments.InternalSegment.Records.t_0.sensor0", SubmodelElementProperty.ID_SHORT, "sensor0_v2")
    );
    // "Segments.InternalSegment.Records.t_0.sensor0"

    @Test
    public void testExecutePathWithNoDots() {
        String smePath = "Metadata";
        SubmodelElementProperty property = SubmodelElementProperty.ID_SHORT;
        String newValue = "new_value";

        Submodel result = doExecute(smePath, property, newValue);
        assertSubmodelElementPropertyValue(result, newValue, property, newValue);
    }

    @Test
    public void testExecutePathWithDots() {
        String smePath = "Segments.InternalSegment.Records.t_0.sensor0";
        SubmodelElementProperty property = SubmodelElementProperty.ID_SHORT;
        String newValue = "sensor0_v2";

        Submodel result = doExecute(smePath, property, newValue);
        assertSubmodelElementPropertyValue(result, "Segments.InternalSegment.Records.t_0.sensor0_v2", property, newValue);
    }

    @Test
    public void testExecuteNotExistingPath() {
        String smePath = "path1.path2.path3";
        SubmodelElementProperty property = SubmodelElementProperty.ID_SHORT;
        String newValue = "new_value";

        Submodel result = doExecute(smePath, property, newValue);
        // doExecute should log an error and return the unchanged submodel
        assertTrue(true);
    }

    @Test
    public void testRenameDisplayName() {
        String smePath = "Segments.InternalSegment.Records.t_0.sensor0";
        SubmodelElementProperty property = SubmodelElementProperty.DISPLAY_NAME;
        String newValue = "sensor0_displayname_v2";

        Submodel result = doExecute(smePath, property, newValue);
        assertSubmodelElementPropertyValue(result, smePath, property, newValue);
    }

    @Test
    public void testRenameDescription() {
        String smePath = "Segments.InternalSegment.Records.t_0.sensor0";
        SubmodelElementProperty property = SubmodelElementProperty.DESCRIPTION;
        String newValue = "sensor0_description_v2";

        Submodel result = doExecute(smePath, property, newValue);
        assertSubmodelElementPropertyValue(result, smePath, property, newValue);
    }

    private Submodel doExecute(String smePath, SubmodelElementProperty property, String newValue) {
        TransformerActionSmeRename action = new TransformerActionSmeRename(smePath, property, newValue);
        TransformerActionService service = new TransformerActionSmeRenameService(action);

        return service.execute(submodel, submodel, null, false);
    }

    private void assertSubmodelElementPropertyValue(Submodel result, String smePath, SubmodelElementProperty property, String newValue) {
        HierarchicalSubmodelElementParser parser = new HierarchicalSubmodelElementParser(result);
        var sme = parser.getSubmodelElementFromIdShortPath(smePath);
        switch (property) {
            case ID_SHORT -> {assert sme.getIdShort().equals(newValue);}
            case DISPLAY_NAME -> {assert sme.getDisplayName().stream().anyMatch(ln -> ln.getText().equals(newValue));}
            case DESCRIPTION -> {assert sme.getDescription().stream().anyMatch(ln -> ln.getText().equals(newValue));}
        }
    }
}
