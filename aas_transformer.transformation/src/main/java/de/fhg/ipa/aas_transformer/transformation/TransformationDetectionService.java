package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.fhg.ipa.aas_transformer.transformation.TransformationDetectionUtils.isSubmodelSourceOfTransformer;

public class TransformationDetectionService {
    private static final Logger LOG = LoggerFactory.getLogger(TransformationDetectionService.class);

    private final TransformerDTOListener transformerDTOListener;

    public TransformationDetectionService(TransformerDTOListener transformerDTOListener) {
        this.transformerDTOListener = transformerDTOListener;
    }
    public boolean isSubmodelSourceOfTransformerActions(Submodel submodel) {
        return isSubmodelSourceOfTransformer(
                submodel,
                transformerDTOListener.getSourceSubmodelIdRules()
        );
    }

    public TransformerDTOListener getTransformerDTOListener() {
        return transformerDTOListener;
    }
}
