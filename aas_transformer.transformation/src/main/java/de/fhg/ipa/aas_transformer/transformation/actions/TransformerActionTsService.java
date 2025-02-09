package de.fhg.ipa.aas_transformer.transformation.actions;

import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionTsAvg;
import de.fhg.ipa.aas_transformer.model.TransformerActionTsFilterAbstract;
import org.eclipse.digitaltwin.aas4j.v3.model.DataTypeDefXsd;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.fhg.ipa.aas_transformer.aas.AasUtils.createClone;
import static de.fhg.ipa.aas_transformer.aas.TimeseriesUtils.*;

public abstract class TransformerActionTsService extends TransformerActionService {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionTsService.class);

    protected final TransformerActionTsFilterAbstract transformerAction;

    protected TransformerActionTsService(
            TransformerActionTsFilterAbstract transformerAction
    ) {
        this.transformerAction = transformerAction;
    }

    @Override
    public Submodel execute(Submodel sourceSubmodel, Submodel intermediateResult, Map<String, Object> context, boolean isFirstAction) {
        // TODO: check if decision based on isFirstAction is necessary / makes sense
        // Use source submodel if intermediate result is not a timeseries submodel
        intermediateResult = getWorkingSubmodel(sourceSubmodel, intermediateResult);

        Optional<SubmodelElement> optionalInternalSegment = getInternalSegmentSubmodelElement(intermediateResult);

        if(optionalInternalSegment.isEmpty()) {
            LOG.error(
                    "Can not run execute of {}, because internal segment was not found",
                    this.transformerAction.getActionType().name()
            );
            return intermediateResult;
        }

        SubmodelElement internalSegment = optionalInternalSegment.get();
        Map<String, List<Object>> recordMap = getRecordMap(
                this.transformerAction.getRecordNames(),
                (DefaultSubmodelElementCollection) internalSegment
        );
        Map<String, List<Object>> resultMap = getResultMap(recordMap);

        writeResultMapIntoInternalSegment(internalSegment, resultMap, DataTypeDefXsd.DOUBLE);

        LOG.info(
                "Transformer Action '{}' executed | Source [SM='{}'] | Destination [SM='{}']",
                this.transformerAction.getActionType().name(),
                sourceSubmodel.getId(),
                intermediateResult.getId()
        );

        return intermediateResult;
    }

    private Submodel getWorkingSubmodel(Submodel sourceSubmodel, Submodel intermediateResult) {
        if(isTimeseriesSubmodel(intermediateResult)) {
            return intermediateResult;
        } else if(isTimeseriesSubmodel(sourceSubmodel)) {
            return createClone(sourceSubmodel);
        } else {
            LOG.error(
                    "Can not run execute of {}, because neither source nor intermediate result is a timeseries submodel",
                    this.transformerAction.getActionType().name()
            );
            return null;

        }
    }

    private Map<String, List<Object>> getResultMap(Map<String, List<Object>> recordMap) {
        Map<String, List<Object>> resultMap = new HashMap<>();

        recordMap.forEach((key, value) ->{
            List<Object> filteredValues = runFilter(value);
            resultMap.put(key, filteredValues);
        });

        return resultMap;
    }

    protected abstract List<Object> runFilter(List<Object> values);

    protected void writeResultMapIntoInternalSegment(
            SubmodelElement internalSegment,
            Map<String, List<Object>> resultMap,
            DataTypeDefXsd newValueType
    ) {
        Optional<SubmodelElement> optionalInternalSegmentRecords =
                getRecordsSubmodelElementByInternalSegment((DefaultSubmodelElementCollection) internalSegment);

        if(optionalInternalSegmentRecords.isEmpty()) {
            return;
        }

        DefaultSubmodelElementCollection internalSegmentRecords = (DefaultSubmodelElementCollection) optionalInternalSegmentRecords.get();

        for(int i = 0; i < internalSegmentRecords.getValue().size(); i++) {
            List<SubmodelElement> records = ((DefaultSubmodelElementCollection) internalSegmentRecords.getValue().get(i)).getValue();

            int index = i;
            records.forEach(record -> {
                String recordName = record.getIdShort().toLowerCase();
                if(resultMap.containsKey(recordName)) {
                    Object newValue = resultMap.get(recordName).get(index);
                    ((DefaultProperty)record).setValue(newValue.toString());
                    if(newValueType != null)
                        ((DefaultProperty)record).setValueType(newValueType);
                }
            });
        }
    }



    protected Map<String, List<Object>> getRecordMap(
            List<String> recordNames,
            DefaultSubmodelElementCollection internalSegment
    ) {
        // TODO get all records if recordNames is empty
        HashMap<String, List<Object>> recordMap = new HashMap<>();
        Optional<SubmodelElement> optionalRecordsSubmodelElement = getRecordsSubmodelElementByInternalSegment(internalSegment);

        if(optionalRecordsSubmodelElement.isEmpty()) {
            return recordMap;
        }

        DefaultSubmodelElementCollection recordsSubmodelElement =
                (DefaultSubmodelElementCollection) optionalRecordsSubmodelElement.get();

        for(String recordName : recordNames) {
            List<Object> recordValues = recordsSubmodelElement.getValue()
                    .stream()
                    .flatMap(se -> ((DefaultSubmodelElementCollection)se).getValue().stream())
                    .filter(prop -> prop.getIdShort().toLowerCase().equals(recordName.toLowerCase()))
                    .map(prop -> ((DefaultProperty)prop).getValue())
                    .collect(Collectors.toList());

            recordMap.put(recordName,recordValues);
        }

        return recordMap;
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
