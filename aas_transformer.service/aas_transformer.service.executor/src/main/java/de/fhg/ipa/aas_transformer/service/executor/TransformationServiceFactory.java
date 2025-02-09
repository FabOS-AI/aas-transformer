package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.transformation.TransformationExecutionService;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionServiceFactory;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransformationServiceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TransformationServiceFactory.class);

    private final TemplateRenderer templateRenderer;
    private final AasRegistry aasRegistry;
    private final AasRepository aasRepository;
    private final SubmodelRegistry submodelRegistry;
    private final SubmodelRepository submodelRepository;
    private final TransformerActionServiceFactory transformerActionServiceFactory;
    private final MetricsClient metricsClient;

    public TransformationServiceFactory(
            TemplateRenderer templateRenderer,
            AasRegistry aasRegistry,
            AasRepository aasRepository,
            SubmodelRegistry submodelRegistry,
            SubmodelRepository submodelRepository,
            TransformerActionServiceFactory transformerActionServiceFactory,
            MetricsClient metricsClient
    ) {
        this.templateRenderer = templateRenderer;
        this.aasRegistry = aasRegistry;
        this.aasRepository = aasRepository;
        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;
        this.transformerActionServiceFactory = transformerActionServiceFactory;
        this.metricsClient = metricsClient;
    }

    public TransformationExecutionService createExecutionService(Transformer transformer) {
        LOG.info("Creating new TransformationExecutionService for transformer: " + transformer.getId());

        var executionService = new TransformationExecutionService(
                transformer,
                this.templateRenderer,
                this.aasRegistry,
                this.aasRepository,
                this.submodelRegistry,
                this.submodelRepository,
                this.transformerActionServiceFactory,
                this.metricsClient
        );

        return executionService;
    }
}
