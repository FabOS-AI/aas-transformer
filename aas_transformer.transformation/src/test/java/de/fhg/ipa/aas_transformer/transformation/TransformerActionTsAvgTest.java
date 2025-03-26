package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.model.SubmodelId;
import de.fhg.ipa.aas_transformer.model.SubmodelIdType;
import de.fhg.ipa.aas_transformer.model.TransformerActionTsAvg;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionTsAvgService;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.basyx.submodelservice.pathparsing.HierarchicalSubmodelElementParser;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;
import static java.lang.Math.ceil;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransformerActionTsAvgTest {

    JsonDeserializer jsonDeserializer = new JsonDeserializer();

    // region Test Objects
    static int filterSize = 5;
    static Submodel sourceSubmodel = getRandomTimeseriesSubmodel(5, 50);
    static SubmodelElementCollection srcRecords;
    static {
        HierarchicalSubmodelElementParser parser = new HierarchicalSubmodelElementParser(sourceSubmodel);
        SubmodelElementCollection srcRecords = (SubmodelElementCollection) parser.getSubmodelElementFromIdShortPath("Segments.InternalSegment.Records");

    }

    Map<String, Object> context = new HashMap<>();
    // endregion

    @Test
    @Order(10)
    public void testExecute() {
        List<String> sensorList = List.of("sensor0", "sensor1");
        List<String> sensorListNotTransformed = List.of("sensor2", "sensor3", "sensor4");
        TransformerActionTsAvg transformerActionTsAvg = new TransformerActionTsAvg(sensorList,filterSize);

        TransformerActionTsAvgService transformerActionTsAvgService = new TransformerActionTsAvgService(
                transformerActionTsAvg
        );

        DefaultSubmodel destinationSubmodel = new DefaultSubmodel();

        destinationSubmodel = (DefaultSubmodel) transformerActionTsAvgService.execute(
                sourceSubmodel,
                destinationSubmodel,
                context,
                true
        );

        assertNotEqual(sourceSubmodel, destinationSubmodel, sensorList);
        assertEqual(sourceSubmodel, destinationSubmodel, sensorListNotTransformed);
    }

    @Test
    @Order(15)
    public void testExecuteEmptyRecordList() {
        List<String> sensorList = List.of("sensor0", "sensor1", "sensor2", "sensor3", "sensor4");
        TransformerActionTsAvg transformerActionTsAvg = new TransformerActionTsAvg(List.of(),filterSize);

        TransformerActionTsAvgService transformerActionTsAvgService = new TransformerActionTsAvgService(
                transformerActionTsAvg
        );

        DefaultSubmodel destinationSubmodel = new DefaultSubmodel();

        destinationSubmodel = (DefaultSubmodel) transformerActionTsAvgService.execute(
                sourceSubmodel,
                destinationSubmodel,
                context,
                true
        );

        assertNotEqual(sourceSubmodel, destinationSubmodel, sensorList);
    }

    @Test
    @Order(20)
    public void testExecuteEolTsOneSensor() throws FileNotFoundException, DeserializationException {
        File submodelFile = new File("src/test/resources/eol_test_ts.json");
        Submodel sourceSubmodel = jsonDeserializer.read(new FileInputStream(submodelFile), Submodel.class);
        List<String> sensorList = List.of("temp_cathode_heattape_A");
        List<String> sensorListNotTransformed = getInvertedSensorList(sourceSubmodel, sensorList);
        TransformerActionTsAvg transformerActionTsAvg = new TransformerActionTsAvg(sensorList,filterSize);

        TransformerActionTsAvgService transformerActionTsAvgService = new TransformerActionTsAvgService(
                transformerActionTsAvg
        );

        DefaultSubmodel destinationSubmodel = new DefaultSubmodel();

        destinationSubmodel = (DefaultSubmodel) transformerActionTsAvgService.execute(
                sourceSubmodel,
                destinationSubmodel,
                context,
                true
        );

        assertNotEqual(sourceSubmodel, destinationSubmodel, sensorList);
        assertEqual(sourceSubmodel, destinationSubmodel, sensorListNotTransformed);
    }

    @Test
    @Order(30)
    public void testExecuteEolTsAllSensor() throws FileNotFoundException, DeserializationException {
        File submodelFile = new File("src/test/resources/eol_test_ts.json");
        Submodel sourceSubmodel = jsonDeserializer.read(new FileInputStream(submodelFile), Submodel.class);
        List<String> sensorList = getSensorNames((DefaultSubmodel) sourceSubmodel);
        List.of("voltage_set", "pressure_anode_set", "pressure_cathode_set", "temp_anode_inlet_set", "temp_cathode_inlet_set", "signal_in_h2_in_o2_sensor", "signal_in_o2_in_h2_sensor", "flow_anode_di_water_supply_set", "pump_di_booster", "signal_in_anode_mass_flow_meter")
                .forEach(sensor -> sensorList.remove(sensor));
        TransformerActionTsAvg transformerActionTsAvg = new TransformerActionTsAvg(List.of(),filterSize);

        TransformerActionTsAvgService transformerActionTsAvgService = new TransformerActionTsAvgService(
                transformerActionTsAvg
        );

        DefaultSubmodel destinationSubmodel = new DefaultSubmodel();

        destinationSubmodel = (DefaultSubmodel) transformerActionTsAvgService.execute(
                sourceSubmodel,
                destinationSubmodel,
                context,
                true
        );

        assertNotEqual(sourceSubmodel, destinationSubmodel, sensorList);
    }

    private void assertEqual(Submodel srcSubmodel, Submodel dstSubmodel, List<String> sensorList) {
        for (String s : sensorList) {
            List<String> srcSensorValues = getSensorValues((DefaultSubmodel) srcSubmodel, s);
            List<String> dstSensorValues = getSensorValues((DefaultSubmodel) dstSubmodel, s);
            assertEquals(srcSensorValues, dstSensorValues);
        }
    }

    private void assertNotEqual(Submodel srcSubmodel, Submodel dstSubmodel, List<String> sensorList) {
        for (String s : sensorList) {
            System.out.println("Check not equal for \""+s+"\".");
            List<String> srcSensorValues = getSensorValues((DefaultSubmodel) srcSubmodel, s);
            List<String> dstSensorValues = getSensorValues((DefaultSubmodel) dstSubmodel, s);

            assertNotEquals(srcSensorValues, dstSensorValues);

            double avg = srcSensorValues.stream().limit(filterSize).mapToDouble(v -> Double.valueOf(v)).average().orElse(0.0);

            assertEquals(avg, Double.valueOf(dstSensorValues.get((int) ceil(filterSize/2))));
        }
    }

    private List<String> getInvertedSensorList(Submodel sourceSubmodel, List<String> sensorList) {
        HierarchicalSubmodelElementParser parser = new HierarchicalSubmodelElementParser(sourceSubmodel);
        SubmodelElementCollection record = (SubmodelElementCollection) parser.getSubmodelElementFromIdShortPath("Metadata.Record");

        return record.getValue()
                .stream()
                .filter(r -> !sensorList.contains(r.getIdShort()))
                .map(r -> r.getIdShort())
                .toList();
    }

    private List<String> getSensorNames(DefaultSubmodel tsSubmodel) {
        HierarchicalSubmodelElementParser parser = new HierarchicalSubmodelElementParser(tsSubmodel);
        SubmodelElementCollection record = (SubmodelElementCollection) parser.getSubmodelElementFromIdShortPath("Metadata.Record");

        return record.getValue()
                .stream()
                .filter(r -> !r.getIdShort().equals("time"))
                .map(r -> r.getIdShort())
                .collect(Collectors.toList());
    }

    private List<String> getSensorValues(DefaultSubmodel tsSubmodel, String sensorId) {
        HierarchicalSubmodelElementParser parser = new HierarchicalSubmodelElementParser(tsSubmodel);
        SubmodelElementCollection records = (SubmodelElementCollection) parser.getSubmodelElementFromIdShortPath("Segments.InternalSegment.Records");

        return records.getValue()
                .stream()
                .map(record -> ((DefaultProperty) ((SubmodelElementCollection) record)
                        .getValue()
                        .stream()
                        .filter(v -> v.getIdShort().equals(sensorId))
                        .findFirst()
                        .get())
                        .getValue())
                .toList();
    }
}
