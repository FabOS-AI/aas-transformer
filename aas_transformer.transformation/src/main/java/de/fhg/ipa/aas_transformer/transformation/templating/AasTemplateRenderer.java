package de.fhg.ipa.aas_transformer.transformation.templating;

import de.fhg.ipa.aas_transformer.model.Destination;
import de.fhg.ipa.aas_transformer.transformation.TransformationUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AasTemplateRenderer {

    private final TransformationUtils transformationUtils;
    private TemplateRenderer templateRenderer;

    public AasTemplateRenderer(
            TemplateRenderer templateRenderer,
            TransformationUtils transformationUtils
    ) {
        this.templateRenderer = templateRenderer;
        this.transformationUtils = transformationUtils;
    }

    public String renderDestinationShellId(
            UUID transformerId,
            Destination transformerDestination,
            Submodel sourceSubmodel
    ) {
        Map<String, Object> templateContext = templateRenderer.getTemplateContext(
                transformerId,
                List.of(),
                sourceSubmodel
        );

        return templateRenderer.render(
                transformerDestination.getAasDestination().getId(),
                templateContext
        );
    }

    public String renderDestinationSubmodelId(
            UUID transformerId,
            Destination transformerDestination,
            Submodel sourceSubmodel
    ) {
        // Set context for template rendering
        Map<String, Object> context = getTemplateContext(transformerId, transformerDestination, sourceSubmodel);

        // Set ID and IdShort for destination submodel:
        return this.templateRenderer.render(
                transformerDestination.getSubmodelDestination().getId(),
                context
        );
    }

    public String renderDestinationSubmodelIdShort(
            UUID transformerId,
            Destination transformerDestination,
            Submodel sourceSubmodel
    ) {
        // Set context for template rendering
        Map<String, Object> context = getTemplateContext(transformerId, transformerDestination, sourceSubmodel);

        // Set ID and IdShort for destination submodel:
        return  this.templateRenderer.render(
                transformerDestination.getSubmodelDestination().getIdShort(),
                context
        );
    }

    public Map<String, Object> getTemplateContext(
            UUID transformerId,
            Destination transformerDestination,
            Submodel sourceSubmodel
    ) {
        // Destination Shells:
        List<AssetAdministrationShell> destinationShells = transformationUtils.lookupDestinationShells(
                transformerId,
                transformerDestination,
                sourceSubmodel
        );

        // Set context for template rendering
        return templateRenderer.getTemplateContext(
                transformerId,
                destinationShells,
                sourceSubmodel
        );
    }
}
