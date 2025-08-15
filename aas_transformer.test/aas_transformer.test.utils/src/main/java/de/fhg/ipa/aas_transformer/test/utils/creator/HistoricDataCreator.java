package de.fhg.ipa.aas_transformer.test.utils.creator;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;

import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesTriples;

public class HistoricDataCreator extends TimeSeriesSubmodelCreator {
    private static final int SENSOR_COUNT = 10;
    private static final int SENSOR_VALUE_COUNT = 100;

    public HistoricDataCreator() {}

    public HistoricDataCreator(AasRegistry aasRegistry, AasRepository aasRepository, SubmodelRegistry smRegistry, SubmodelRepository smRepository, int submodelCount, int sleepInMs) {
        super(aasRegistry, aasRepository, smRegistry, smRepository, submodelCount, sleepInMs);
    }

    protected Submodel createShellWithTimeseriesSubmodel() {
        List<List<Object>> triple = getRandomTimeseriesTriples(1, SENSOR_COUNT, SENSOR_VALUE_COUNT);
        registerTriples(triple);
        return (Submodel) triple.get(0).get(1);
    }

    protected void createShellWithTimeseriesSubmodel(int count) {
        System.out.println("Start registering "+count+" submodels...");
        for(int i = 0; i < count; i++)
            createShellWithTimeseriesSubmodel();
    }
}
