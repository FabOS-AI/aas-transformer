package de.fhg.ipa.aas_transformer.transformation.actions;

import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionSmCopy;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TransformerActionSmCopyService extends TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionSmCopyService.class);

    private final TransformerActionSmCopy transformerAction;
    private final SubmodelRepository submodelRepository;
    private final SubmodelRegistry submodelRegistry;
    private final TemplateRenderer templateRenderer;

    public TransformerActionSmCopyService(
            SubmodelRegistry submodelRegistry,
            SubmodelRepository submodelRepository,
            TemplateRenderer templateRenderer,
            TransformerActionSmCopy transformerAction
    ) {
        this.transformerAction = transformerAction;
        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public Submodel execute(
            Submodel sourceSubmodel,
            Submodel intermediateResult,
            Map<String, Object> context,
            boolean isFirstAction
    ) {
        String cpSourceSubmodelId = this.templateRenderer.render(this.transformerAction.getSubmodelId(), context);
        Submodel cpSourceSubmodel = this.submodelRepository.getExtSubmodel(this.submodelRegistry, cpSourceSubmodelId);

        if(cpSourceSubmodel.getAdministration() != null)
            intermediateResult.setAdministration(cpSourceSubmodel.getAdministration());
        if(cpSourceSubmodel.getCategory() != null)
            intermediateResult.setCategory(cpSourceSubmodel.getCategory());
        if(cpSourceSubmodel.getDescription() != null)
            intermediateResult.setDescription(cpSourceSubmodel.getDescription());
        if(cpSourceSubmodel.getDisplayName() != null)
            intermediateResult.setDisplayName(cpSourceSubmodel.getDisplayName());
        if(cpSourceSubmodel.getEmbeddedDataSpecifications() != null)
            intermediateResult.setEmbeddedDataSpecifications(cpSourceSubmodel.getEmbeddedDataSpecifications());
        if(cpSourceSubmodel.getExtensions() != null)
            intermediateResult.setExtensions(cpSourceSubmodel.getExtensions());
        if(cpSourceSubmodel.getQualifiers() != null)
            intermediateResult.setQualifiers(cpSourceSubmodel.getQualifiers());
        if(cpSourceSubmodel.getSemanticId() != null)
            intermediateResult.setSemanticId(cpSourceSubmodel.getSemanticId());
        if(cpSourceSubmodel.getSubmodelElements() != null)
            intermediateResult.getSubmodelElements().addAll(cpSourceSubmodel.getSubmodelElements());
        if(cpSourceSubmodel.getSupplementalSemanticIds() != null)
            intermediateResult.setSupplementalSemanticIds(cpSourceSubmodel.getSupplementalSemanticIds());

        // Avoids error during registration if type is not set in source submodel
        intermediateResult.getSemanticId().setType(ReferenceTypes.EXTERNAL_REFERENCE);

        return intermediateResult;
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
