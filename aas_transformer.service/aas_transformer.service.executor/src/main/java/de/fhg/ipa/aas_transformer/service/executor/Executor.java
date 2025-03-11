package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisClient;
import de.fhg.ipa.aas_transformer.model.TransformationDescription;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.persistence.api.TransformationDescriptionJpaRepository;
import de.fhg.ipa.aas_transformer.transformation.TransformationExecutionService;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.submodelservice.SubmodelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
public class Executor {
    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);
    private final TransformationExecutionServiceCache transformationExecutionServiceCache;
    private final AasRegistry aasRegistry;
    private final AasRepository aasRepository;
    private final SubmodelRepository submodelRepository;
    private final SubmodelRegistry submodelRegistry;
    private final TemplateRenderer templateRenderer;
    private final ManagementClient managementClient;
    private final TransformationDescriptionJpaRepository transformationDescriptionJpaRepository;

    public Executor(
            TransformationExecutionServiceCache transformationExecutionServiceCache,
            AasRegistry aasRegistry,
            AasRepository aasRepository,
            SubmodelRepository submodelRepository,
            SubmodelRegistry submodelRegistry,
            TemplateRenderer templateRenderer,
            ManagementClient managementClient,
            TransformationDescriptionJpaRepository transformationDescriptionJpaRepository
    ) {
        this.transformationExecutionServiceCache = transformationExecutionServiceCache;
        this.aasRegistry = aasRegistry;
        this.aasRepository = aasRepository;
        this.submodelRepository = submodelRepository;
        this.submodelRegistry = submodelRegistry;
        this.templateRenderer = templateRenderer;
        this.managementClient = managementClient;
        this.transformationDescriptionJpaRepository = transformationDescriptionJpaRepository;
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

    public List<Submodel> executeBatchOnRequest() {
        List<TransformationDescription> transformationDescriptions =
                transformationDescriptionJpaRepository.findAll().collectList().block();
        return transformationDescriptions.stream()
                .map(d -> executeOnRequest(d.getTargetSubmodelId()))
                .filter(s -> s != null)
                .toList();
    }

    public Submodel executeOnRequest(String submodelId) {
        TransformationDescription transformationDescription =
                transformationDescriptionJpaRepository.findTopByTargetSubmodelIdOrderByIdDesc(submodelId).block();
        if(transformationDescription == null)
            return null;
        UUID transformerId = transformationDescription.getTransformerId();
        TransformationExecutionService executionService = this.transformationExecutionServiceCache
                .getTransformationExecutionServiceByTransformerId(transformerId);

        if(executionService == null) {
            LOG.error("No TransformationExecutionService found for {} destinationSubmodelId with transformerId {}",
                    submodelId,
                    transformerId
            );
            return null;
        } else {
            return executionService.executeTransformationDescription(
                    transformationDescription,
                    RedisClient.getConsumerId()
            );
        }
    }

    private void executeDeleted(TransformationJob job) {
        String sourceSubmodelId = job.getSubmodelId();

        LOG.info("Execute {} job | sourceSmId {} | TransformerID {}",
                job.getTransformationJobAction(),
                sourceSubmodelId,
                job.getTransformerId()
        );
        TransformationExecutionService executionService = this.transformationExecutionServiceCache
                .getTransformationExecutionServiceByTransformerId(job.getTransformerId());

        if(executionService.getTransformOnRequest()) {
            List<TransformationDescription> descriptions = transformationDescriptionJpaRepository.findBySourceSubmodelId(
                    sourceSubmodelId
            ).collectList().block();
            descriptions.forEach(d -> {
                // Delete Target Submodel Descriptor:
                this.submodelRegistry.deleteSubmodelDescriptor(d.getTargetSubmodelId());
                // Delete Transformation Description:
                transformationDescriptionJpaRepository.delete(d).block();
            });
        } else {
            this.submodelRepository.deleteSubmodel(sourceSubmodelId);
        }
        this.aasRegistry.removeSubmodelDescriptorFromAllAas(sourceSubmodelId);
        this.aasRepository.removeSubmodelReferenceFromAllAas(sourceSubmodelId);
    }

    private void printProcessingMsg(TransformationJob job) {
        LOG.info("Processing job | sourceSmId {} | TransformerID {}",
                job.getSubmodelId(),
                job.getTransformerId()
        );
    }
}
