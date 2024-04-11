package de.fhg.ipa.aas_transformer.service.actions;

import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionService.class);

    protected final SubmodelRegistry submodelRegistry;

    protected final SubmodelRepository submodelRepository;

    protected TransformerActionService(SubmodelRegistry submodelRegistry, SubmodelRepository submodelRepository) {

        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;
    }

    public abstract void execute(Submodel sourceSubmodel, Submodel destinationSubmodel, Map<String, Object> context);

    public abstract TransformerAction getTransformerAction();

    public boolean isSubmodelSourceOfTransformerAction(Submodel submodel) {
        var transformerAction = this.getTransformerAction();
        var submodelId = transformerAction.getSourceSubmodelId();

        switch (submodelId.getType()) {
            case ID -> {
                return submodel.getId().equals(submodelId.getId());
            }

            case ID_SHORT -> {
                return submodel.getIdShort().equals(submodelId.getId());
            }

            case SEMANTIC_ID -> {
                if (submodel.getSemanticId() != null){
                    return submodel
                            .getSemanticId()
                            .getKeys()
                            .stream()
                            .filter(k -> k.getValue().equals(submodelId.getType()))
                            .findFirst()
                            .isPresent();
                } else {
                    return false;
                }
            }

            default -> {
                return false;
            }
        }
    }

}
