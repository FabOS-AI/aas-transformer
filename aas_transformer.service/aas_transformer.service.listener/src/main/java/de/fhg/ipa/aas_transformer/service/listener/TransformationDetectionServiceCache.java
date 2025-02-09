package de.fhg.ipa.aas_transformer.service.listener;

import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.TransformerDTOListenerCache;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.service.listener.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.transformation.TransformationDetectionService;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TransformationDetectionServiceCache extends TransformerDTOListenerCache {
    private static final Logger LOG = LoggerFactory.getLogger(TransformationDetectionServiceCache.class);

    private final SubmodelRepository submodelRepository;
    public List<TransformationDetectionService> transformationDetectionServices = new ArrayList<>();

    @Value("${aas_transformer.services.listener.strict-mode.enabled:false}")
    private boolean strictModeEnabled;

    public TransformationDetectionServiceCache(
            ManagementClient managementClient,
            SubmodelRepository submodelRepository
    ) {
        super(managementClient);
        this.submodelRepository = submodelRepository;
    }

    @PostConstruct
    public void init() {
        this.transformerFlux
            .doOnError(e -> {
                LOG.warn("ManagementClient failed to connect: " + e.getMessage() + " - retrying in " + this.connectionRetryTimeoutInMs + " ms...");
            })
            .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofMillis(this.connectionRetryTimeoutInMs)))
            .doOnComplete(() -> {
                this.transformerEventFlux.log().subscribe(this::handleTransformerEvent);
            })
            .subscribe(this::addTransformationDetectionService);
    }

    public @NotNull List<TransformerDTOListener> getTransformerDTOListenerCache() {
        return transformationDetectionServices
                .stream()
                .map(service -> service.getTransformerDTOListener())
                .collect(Collectors.toList());
    }

    public List<TransformationJob> getTransformationJobsBySubmodelMessageEvent(SubmodelMessageEvent event) {
        return this.transformationDetectionServices
                .stream()
                .filter(service -> service.isSubmodelSourceOfTransformerActions(event.getSubmodel()))
                .map(service -> createTransformationJob(event, service))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<TransformationJob> createTransformationJob(SubmodelMessageEvent event, TransformationDetectionService service) {
        switch (event.getChangeEventType()) {
            case CREATED:
            case UPDATED:
                Submodel sourceSubmodel = null;
                if(strictModeEnabled)
                    sourceSubmodel = submodelRepository.getSubmodel(event.getSubmodel().getId());
                return List.of(new TransformationJob(
                        TransformationJobAction.EXECUTE,
                        service.getTransformerDTOListener().getId(),
                        event.getSubmodel().getId(),
                        sourceSubmodel
                ));
            case DELETED:
                return managementClient
                        .getOrphanedDestinationSubmodels(
                                service.getTransformerDTOListener().getId(),
                                (DefaultSubmodel) event.getSubmodel()
                        )
                        .block()
                        .stream()
                        .map(smId -> new TransformationJob(
                                TransformationJobAction.DELETE,
                                null,
                                smId,
                                null
                        ))
                        .collect(Collectors.toList());
            default:
                return null;

        }
    }

    public void getTransformationDetectionServiceByTransformerId(UUID id) {
        transformationDetectionServices.stream()
            .filter(t -> t.getTransformerDTOListener().getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    private void addTransformationDetectionService(TransformerDTOListener transformerDTOListener) {
        transformationDetectionServices.add(
                new TransformationDetectionService(transformerDTOListener)
        );
    }

    private void deleteTransformationDetectionService(TransformerDTOListener transformerDTOListener) {
        transformationDetectionServices.removeIf(
                t -> t.getTransformerDTOListener().getId().equals(
                        transformerDTOListener.getId()
                )
        );
    }

    private void handleTransformerEvent(TransformerChangeEventDTOListener event) {
        switch (event.getType()) {
            case CREATE:
            case UPDATE:
                addTransformationDetectionService(event.getTransformer());
                break;
            case DELETE:
                deleteTransformationDetectionService(event.getTransformer());
                break;
        }
    }
}
