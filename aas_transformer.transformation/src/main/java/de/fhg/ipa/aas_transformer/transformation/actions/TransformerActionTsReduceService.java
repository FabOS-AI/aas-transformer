package de.fhg.ipa.aas_transformer.transformation.actions;

import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionTsReduce;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.fhg.ipa.aas_transformer.aas.AasUtils.createClone;
import static de.fhg.ipa.aas_transformer.aas.TimeseriesUtils.*;

public class TransformerActionTsReduceService extends TransformerActionService {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionTsReduceService.class);

    private final TransformerActionTsReduce transformerAction;

    public TransformerActionTsReduceService(
            TransformerActionTsReduce transformerAction
    ) {
        this.transformerAction = transformerAction;
    }

    @Override
    public Submodel execute(
            Submodel sourceSubmodel, 
            Submodel intermediateResult,
            Map<String, Object> context,
            boolean isFirstAction
    ) {
        intermediateResult = getWorkingSubmodel(sourceSubmodel, intermediateResult);

        Optional<SubmodelElement> optionalInternalSegment = getInternalSegmentSubmodelElement(intermediateResult);

        if(optionalInternalSegment.isEmpty()) {
            LOG.error(
                    "Can not run execute of {}, because internal segment was not found",
                    this.transformerAction.getActionType().name()
            );
            return intermediateResult;
        }

        DefaultSubmodelElementCollection internalSegment =  (DefaultSubmodelElementCollection) optionalInternalSegment.get();
        Optional<SubmodelElement> optionalRecords = getRecordsSubmodelElementByInternalSegment(internalSegment);

        if(optionalRecords.isEmpty()) {
            LOG.error(
                    "Can not run execute of {}, because records was not found",
                    this.transformerAction.getActionType().name()
            );
            return intermediateResult;
        }

        DefaultSubmodelElementCollection records = (DefaultSubmodelElementCollection) optionalRecords.get();

        List<SubmodelElement> originalRecordValues = records.getValue();
        List<SubmodelElement> newRecordValues = new ArrayList<>();

        for(int i = 0; i < originalRecordValues.size(); i++) {

            switch (this.transformerAction.getActionType()) {
                case TS_TAKE_EVERY:
                    if(i % this.transformerAction.getN() == 0)
                        newRecordValues.add(originalRecordValues.get(i));
                    break;
                case TS_DROP_EVERY:
                    if((i+1) % this.transformerAction.getN() != 0)
                        newRecordValues.add(originalRecordValues.get(i));
                default:
                    LOG.error("Unknown TS Reduce action type: {}", this.transformerAction.getActionType().name());
                    break;
            }
        }

        records.setValue(newRecordValues);

        updateRecordCount(internalSegment, newRecordValues.size());

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

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
