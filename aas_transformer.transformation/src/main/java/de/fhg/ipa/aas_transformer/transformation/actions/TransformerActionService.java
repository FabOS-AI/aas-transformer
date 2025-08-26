package de.fhg.ipa.aas_transformer.transformation.actions;

import de.fhg.ipa.aas_transformer.model.actions.TransformerAction;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionService.class);

    protected TransformerActionService() {}

    public abstract Submodel execute(
            Submodel sourceSubmodel,
            Submodel intermediateResult,
            Map<String, Object> context,
            boolean isFirstAction
    );

    public abstract TransformerAction getTransformerAction();
}
