package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisClient;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.transformation.TransformationExecutionService;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Executor {
    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);
    private final TransformationExecutionServiceCache transformationExecutionServiceCache;
    private final AasRegistry aasRegistry;
    private final AasRepository aasRepository;
    private final SubmodelRepository submodelRepository;
    private final TemplateRenderer templateRenderer;
    private final ManagementClient managementClient;

    public Executor(
            TransformationExecutionServiceCache transformationExecutionServiceCache,
            AasRegistry aasRegistry,
            AasRepository aasRepository,
            SubmodelRepository submodelRepository,
            TemplateRenderer templateRenderer,
            ManagementClient managementClient
    ) {
        this.transformationExecutionServiceCache = transformationExecutionServiceCache;
        this.aasRegistry = aasRegistry;
        this.aasRepository = aasRepository;
        this.submodelRepository = submodelRepository;
        this.templateRenderer = templateRenderer;
        this.managementClient = managementClient;
    }

    public void execute(TransformationJob job) {
        printProcessingMsg(job);
        switch (job.getTransformationJobAction()) {
            case EXECUTE:
                executeCreated(job);
                break;
            case DELETE:
                executeDeleted(job);
                break;
            default:
                LOG.error("Unknown transformation job type {}", job.getTransformationJobAction());
        }
    }

    private void executeCreated(TransformationJob job) {
        TransformationExecutionService executionService = this.transformationExecutionServiceCache
                .getTransformationExecutionServiceByTransformerId(job.getTransformerId());

        if(executionService == null)
            LOG.error("No TransformationExecutionService found for {} job with transformerId {}",
                    job.getTransformationJobAction(),
                    job.getTransformerId()
            );
        else
            executionService.execute(job, RedisClient.getConsumerId());
    }

    private void executeDeleted(TransformationJob job) {
        LOG.info("Execute {} job | sourceSmId {} | TransformerID {}",
                job.getTransformationJobAction(),
                job.getSubmodelId(),
                job.getTransformerId()
        );
        this.submodelRepository.deleteSubmodel(job.getSubmodelId());
        this.aasRegistry.removeSubmodelDescriptorFromAllAas(job.getSubmodelId());
        this.aasRepository.removeSubmodelReferenceFromAllAas(job.getSubmodelId());
    }

    private void printProcessingMsg(TransformationJob job) {
        LOG.info("Processing job | sourceSmId {} | TransformerID {}",
                job.getSubmodelId(),
                job.getTransformerId()
        );
    }
}
