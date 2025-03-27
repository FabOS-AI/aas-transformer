package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.SubmodelDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static de.fhg.ipa.aas_transformer.transformation.TransformationDetectionUtils.isSubmodelSourceOfTransformer;

public class TransformationDetectionService {
    private static final Logger LOG = LoggerFactory.getLogger(TransformationDetectionService.class);

    private final TransformerDTOListener transformerDTOListener;
    private final TemplateRenderer templateRenderer;
    private final TransformationUtils transformationUtils;
    private final AasRegistry aasRegistry;
    private final SubmodelRegistry submodelRegistry;

    public TransformationDetectionService(
            TransformerDTOListener transformerDTOListener,
            TemplateRenderer templateRenderer,
            TransformationUtils transformationUtils,
            AasRegistry aasRegistry,
            SubmodelRegistry submodelRegistry
    ) {
        this.transformerDTOListener = transformerDTOListener;
        this.templateRenderer = templateRenderer;
        this.transformationUtils = transformationUtils;
        this.aasRegistry = aasRegistry;
        this.submodelRegistry = submodelRegistry;
    }
    public boolean isSubmodelSourceOfTransformerActions(Submodel submodel) {
        return isSubmodelSourceOfTransformer(
                submodel,
                transformerDTOListener.getSourceSubmodelIdRules()
        );
    }

    public boolean isSubmodelDestinationOfTransformerAction(Submodel sourceSubmodel) {
        String destinationId = transformerDTOListener.getDestination().getSubmodelDestination().getId();

        if(!TemplateRenderer.hasTemplate(destinationId))
            return (destinationId != null && destinationId == sourceSubmodel.getId());

        for(SubmodelDescriptor smd : submodelRegistry.getSubmodelDescriptors()) {
            Submodel potentialSourceSubmodel = SubmodelRepository.getExtSubmodel(submodelRegistry, smd.getId());
            if(isSubmodelTransformationSourceOfSubmodel(potentialSourceSubmodel, sourceSubmodel)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSubmodelTransformationSourceOfSubmodel(Submodel sourceSubmodel, Submodel destinationSubmodel) {
        List<AssetAdministrationShell> destinationShells = transformationUtils.lookupDestinationShells(
                sourceSubmodel.getId(),
                transformerDTOListener.getDestination().getAasDestination(),
                templateRenderer.getTemplateContext(transformerDTOListener.getId(),List.of(), sourceSubmodel)
        );
        Map<String, Object> context = templateRenderer.getTemplateContext(
                transformerDTOListener.getId(),
                destinationShells,
                sourceSubmodel
        );
        String potentialDestinationId = templateRenderer.render(
                transformerDTOListener.getDestination().getSubmodelDestination().getId(),
                context
        );

        return potentialDestinationId.equals(destinationSubmodel.getId());
    }

    public TransformerDTOListener getTransformerDTOListener() {
        return transformerDTOListener;
    }
}
