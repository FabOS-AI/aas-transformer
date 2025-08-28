package de.fhg.ipa.aas_transformer.transformation.actions;

import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.actions.TransformerAction;
import de.fhg.ipa.aas_transformer.model.actions.TransformerActionSmCopy;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
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
        Submodel cpSourceSubmodel = SubmodelRepository.getExtSubmodel(this.submodelRegistry, cpSourceSubmodelId);

        if(cpSourceSubmodel == null) {
            LOG.warn("Submodel with ID {}' not found in Submodel Repository. Skipping Transformer Action '{}'.",
                    cpSourceSubmodelId,
                    this.transformerAction.getType()
            );
            return intermediateResult;
        }

        intermediateResult.setAdministration(cpSourceSubmodel.getAdministration());
        intermediateResult.setCategory(cpSourceSubmodel.getCategory());
        intermediateResult.setDescription(cpSourceSubmodel.getDescription());
        intermediateResult.setDisplayName(cpSourceSubmodel.getDisplayName());
        intermediateResult.setEmbeddedDataSpecifications(cpSourceSubmodel.getEmbeddedDataSpecifications());
        intermediateResult.setExtensions(cpSourceSubmodel.getExtensions());
        intermediateResult.setQualifiers(cpSourceSubmodel.getQualifiers());
        intermediateResult.setSemanticId(cpSourceSubmodel.getSemanticId());
        intermediateResult.getSubmodelElements().addAll(cpSourceSubmodel.getSubmodelElements());
        intermediateResult.setSupplementalSemanticIds(cpSourceSubmodel.getSupplementalSemanticIds());

        return intermediateResult;
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
