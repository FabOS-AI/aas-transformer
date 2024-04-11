package de.fhg.ipa.aas_transformer.service;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.service.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.service.aas.AasRepository;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.actions.TransformerActionServiceFactory;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.springframework.stereotype.Service;

@Service
public class TransformerServiceFactory {

    private final TemplateRenderer templateRenderer;

    private final AasRegistry aasRegistry;

    private final AasRepository aasRepository;

    private final SubmodelRegistry submodelRegistry;

    private final SubmodelRepository submodelRepository;

    private final TransformerActionServiceFactory transformerActionServiceFactory;

    public TransformerServiceFactory(TemplateRenderer templateRenderer,
                                     AasRegistry aasRegistry, AasRepository aasRepository,
                                     SubmodelRegistry submodelRegistry, SubmodelRepository submodelRepository,
                                     TransformerActionServiceFactory transformerActionServiceFactory) {
        this.templateRenderer = templateRenderer;
        this.aasRegistry = aasRegistry;
        this.aasRepository = aasRepository;
        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;
        this.transformerActionServiceFactory = transformerActionServiceFactory;
    }

    public TransformerService create (Transformer transformer) {
        var transformerService = new TransformerService(
                transformer,
                this.templateRenderer,
                this.aasRegistry, this.aasRepository,
                this.submodelRegistry, this.submodelRepository,
                this.transformerActionServiceFactory);

        return transformerService;
    }

}
