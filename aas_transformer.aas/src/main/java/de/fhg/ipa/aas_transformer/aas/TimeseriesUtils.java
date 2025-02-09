package de.fhg.ipa.aas_transformer.aas;

import org.eclipse.digitaltwin.aas4j.v3.model.DataTypeDefXsd;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TimeseriesUtils {
    private static final String SEMANTIC_ID_TIMESERIES_SUBMODEL = "https://admin-shell.io/idta/TimeSeries/1/1";

    private static final String ID_SHORT_SEGMENTS = "segments";
    private static final String ID_SHORT_INTERNAL_SEGMENT = "internalsegment";
    private static final String ID_SHORT_INTERNAL_SEGMENT_RECORDS = "records";
    private static final String ID_SHORT_INTERNAL_SEGMENT_RECORD_COUNT = "RecordCount";

    public static boolean isTimeseriesSubmodel(Submodel submodel) {
        try {
            return submodel
                    .getSemanticId()
                    .getKeys()
                    .stream()
                    .filter(key -> key.getValue().toLowerCase().equals(SEMANTIC_ID_TIMESERIES_SUBMODEL.toLowerCase()))
                    .findFirst()
                    .isPresent();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static Optional<SubmodelElement> getInternalSegmentSubmodelElement(Submodel timeseriesSubmodel) {
        return timeseriesSubmodel
                .getSubmodelElements()
                .stream()
                .filter(se -> se.getIdShort().toLowerCase().equals(ID_SHORT_SEGMENTS))
                .flatMap(se -> ((DefaultSubmodelElementCollection)se).getValue().stream())
                .filter(value -> value.getIdShort().toLowerCase().equals(ID_SHORT_INTERNAL_SEGMENT))
                .findFirst();
    }

    public static Optional<SubmodelElement> getRecordsSubmodelElementByInternalSegment(DefaultSubmodelElementCollection internalSegment) {
        return internalSegment.getValue()
                .stream()
                .filter(se -> se.getIdShort().toLowerCase().equals(ID_SHORT_INTERNAL_SEGMENT_RECORDS))
                .findFirst();
    }

    public static void updateRecordCount(DefaultSubmodelElementCollection internalSegment, int newCount) {
        Optional<SubmodelElement> optionalRecordCountProperty = internalSegment.getValue()
                .stream()
                .filter(se -> se.getIdShort().toLowerCase().equals(ID_SHORT_INTERNAL_SEGMENT_RECORD_COUNT))
                .findFirst();

        if(optionalRecordCountProperty.isEmpty()) {
            createRecordCount(internalSegment, newCount);
        } else {
            ((DefaultProperty)optionalRecordCountProperty.get()).setValue(String.valueOf(newCount));
        }
    }

    private static void createRecordCount(DefaultSubmodelElementCollection internalSegment, int newCount) {
        DefaultProperty recordCountProperty = new DefaultProperty();
        recordCountProperty.setIdShort(ID_SHORT_INTERNAL_SEGMENT_RECORD_COUNT);
        recordCountProperty.setValue(String.valueOf(newCount));
        recordCountProperty.setValueType(DataTypeDefXsd.LONG);

        List<SubmodelElement> values = new ArrayList<>(internalSegment.getValue());
        values.add(recordCountProperty);
        internalSegment.setValue(values);
    }
}
