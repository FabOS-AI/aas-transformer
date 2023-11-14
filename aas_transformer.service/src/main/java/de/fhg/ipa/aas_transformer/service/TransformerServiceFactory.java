package de.fhg.ipa.aas_transformer.service;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.actions.TransformerActionServiceFactory;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.springframework.stereotype.Service;

@Service
public class TransformerServiceFactory {

    private final TemplateRenderer templateRenderer;

    private final AASRegistry aasRegistry;

    private final AASManager aasManager;

    private final TransformerActionServiceFactory transformerActionServiceFactory;

    public TransformerServiceFactory(TemplateRenderer templateRenderer,
                                     AASRegistry aasRegistry,
                                     AASManager aasManager,
                                     TransformerActionServiceFactory transformerActionServiceFactory) {
        this.templateRenderer = templateRenderer;
        this.aasRegistry = aasRegistry;
        this.aasManager = aasManager;
        this.transformerActionServiceFactory = transformerActionServiceFactory;
    }

    public TransformerService create (Transformer transformer) {
        var transformerService = new TransformerService(
                transformer,
                templateRenderer,
                aasRegistry, aasManager,
                transformerActionServiceFactory);

        return transformerService;
    }

}
