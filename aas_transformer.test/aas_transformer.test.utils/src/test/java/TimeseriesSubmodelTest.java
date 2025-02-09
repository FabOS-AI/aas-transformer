import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;


@Disabled
public class TimeseriesSubmodelTest {
    @Test
    void testInitOfTimeseriesSubmodel() {
        Submodel tsSubmodel = getRandomTimeseriesSubmodel(10, 500);

        return;
    }
}
