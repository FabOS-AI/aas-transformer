package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static de.fhg.ipa.aas_transformer.transformation.TransformationDetectionUtils.isSubmodelSourceOfTransformer;
import static de.fhg.ipa.aas_transformer.transformation.TransformationExecutionService.lookupDestinationShells;

public class TransformationDetectionService {
    private static final Logger LOG = LoggerFactory.getLogger(TransformationDetectionService.class);

    private final TransformerDTOListener transformerDTOListener;
    private final TemplateRenderer templateRenderer;
    private final AasRegistry aasRegistry;
    private final AasRepository aasRepository;

    public TransformationDetectionService(
            TransformerDTOListener transformerDTOListener,
            TemplateRenderer templateRenderer,
            AasRegistry aasRegistry,
            AasRepository aasRepository
    ) {
        this.transformerDTOListener = transformerDTOListener;
        this.templateRenderer = templateRenderer;
        this.aasRegistry = aasRegistry;
        this.aasRepository = aasRepository;
    }
    public boolean isSubmodelSourceOfTransformerActions(Submodel submodel) {
        return isSubmodelSourceOfTransformer(
                submodel,
                transformerDTOListener.getSourceSubmodelIdRules()
        );
    }

    public boolean isSubmodelDestinationOfTransformerAction(Submodel submodel) {
        List<AssetAdministrationShell> destinationShells = lookupDestinationShells(
                aasRegistry,
                aasRepository,
                submodel.getId(),
                transformerDTOListener.getDestination().getAasDestination(),
                templateRenderer.getTemplateContext(transformerDTOListener.getId(),List.of(), submodel)
        );
        Map<String, Object> context = templateRenderer.getTemplateContext(
                transformerDTOListener.getId(),
                destinationShells,
                submodel
        );
        String destinationId = templateRenderer.render(
                transformerDTOListener.getDestination().getSubmodelDestination().getId(),
                context
        );

        return (destinationId != null && destinationId == submodel.getId());
    }

    public TransformerDTOListener getTransformerDTOListener() {
        return transformerDTOListener;
    }
}
