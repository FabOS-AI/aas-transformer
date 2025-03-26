package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.TransformationDescription;
import de.fhg.ipa.aas_transformer.persistence.api.TransformationDescriptionJpaRepository;
import de.fhg.ipa.aas_transformer.transformation.TransformationExecutionService;
import de.fhg.ipa.aas_transformer.transformation.TransformationUtils;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.SubmodelDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
public class SubmodelDescriptorHandler {
    @Autowired
    private TransformationDescriptionJpaRepository transformationDescriptionJpaRepository;
    @Autowired
    private TemplateRenderer templateRenderer;
    @Autowired
    private TransformationExecutionServiceCache transformationExecutionServiceCache;
    @Autowired
    private AasRegistry aasRegistry;
    @Autowired
    private AasRepository aasRepository;
    @Autowired
    private SubmodelRegistry submodelRegistry;
    @Autowired
    private SubmodelRepository submodelRepository;
    @Autowired
    private TransformationUtils transformationUtils;
    @Value("${aas_transformer.services.executor.external_base_url}")
    public String externalBaseUrl;

    public SubmodelDescriptor getSubmodelDescriptor(String submodelIdentifier) {
        TransformationDescription description = transformationDescriptionJpaRepository
                .findByTargetSubmodelId(submodelIdentifier)
                .block();

        return getSubmodelDescriptor(description);
    }

    public List<SubmodelDescriptor> getSubmodelDescriptors() {
        List<SubmodelDescriptor> submodelDescriptors = new ArrayList<>();
        List<TransformationDescription> descriptions = transformationDescriptionJpaRepository
                .findAll()
                .collectList()
                .block();

        descriptions.forEach(description ->
            submodelDescriptors.add(getSubmodelDescriptor(description))
        );

        return submodelDescriptors;
    }

    private SubmodelDescriptor getSubmodelDescriptor(TransformationDescription description) {
        String sourceSubmodelId = description.getSourceSubmodelId();
        UUID transformerId = description.getTransformerId();
        TransformationExecutionService execService = this.transformationExecutionServiceCache
                .getTransformationExecutionServiceByTransformerId(transformerId);

        List<AssetAdministrationShell> destinationShells = transformationUtils.lookupDestinationShells(
                sourceSubmodelId,
                execService.getTransformer().getDestination().getAasDestination(),
                templateRenderer.getTemplateContext(
                        transformerId,
                        List.of(),
                        submodelRepository.getExtSubmodel(submodelRegistry, sourceSubmodelId)
                )
        );

        // Set context for template rendering
        Map<String, Object> context = templateRenderer.getTemplateContext(
                transformerId,
                destinationShells,
                submodelRepository.getExtSubmodel(submodelRegistry, sourceSubmodelId)
        );

        // Set ID and IdShort for destination submodel:
        var destinationSubmodelId = this.templateRenderer.render(
                execService.getTransformer().getDestination().getSubmodelDestination().getId(),
                context
        );
        var destinationSubmodelIdShort = this.templateRenderer.render(
                execService.getTransformer().getDestination().getSubmodelDestination().getIdShort(),
                context
        );

        return submodelRegistry.createSubmodelDescriptor(
                destinationSubmodelId,
                destinationSubmodelIdShort,
                externalBaseUrl
        );
    }
}
