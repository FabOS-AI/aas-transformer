package de.fhg.ipa.aas_transformer.test.utils.creator;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;

import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesTriples;

public class MachineDataCreator extends TimeSeriesSubmodelCreator {
    private static final int SENSOR_COUNT = 100;

    private List<Object> machineTriple = getRandomTimeseriesTriples(1, SENSOR_COUNT, 1).get(0);
    private boolean isMachineTripleInitialized = false;

    public MachineDataCreator() {super();}

    public MachineDataCreator(
            AasRegistry aasRegistry,
            AasRepository aasRepository,
            SubmodelRegistry smRegistry,
            SubmodelRepository smRepository,
            int submodelCount,
            int sleepInMs
    ) {
        super(aasRegistry, aasRepository, smRegistry, smRepository, submodelCount, sleepInMs);
    }

    protected void createShellWithTimeseriesSubmodel(int count) {
        for(int i = 0; i < count; i++)
            createShellWithTimeseriesSubmodel();
    }

    protected Submodel createShellWithTimeseriesSubmodel() {
        if(!isMachineTripleInitialized) {
            registerTriples(List.of(machineTriple));
            isMachineTripleInitialized = true;
        } else
            registerTriples(List.of(getMachineTripleWithNewSensorValues()));
        return (Submodel) machineTriple.get(1);
    }

    private List<Object> getMachineTripleWithNewSensorValues() {
        Submodel newTimeseriesSubmodel = (Submodel) getRandomTimeseriesTriples(1, SENSOR_COUNT, 1).get(0).get(1);
        newTimeseriesSubmodel.setId(((Submodel)machineTriple.get(1)).getId());
        machineTriple.set(1, newTimeseriesSubmodel);
        return machineTriple;
    }
}
