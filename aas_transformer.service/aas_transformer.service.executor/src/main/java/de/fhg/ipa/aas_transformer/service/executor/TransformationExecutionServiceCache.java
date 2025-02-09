package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.TransformerCache;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEvent;
import de.fhg.ipa.aas_transformer.transformation.TransformationExecutionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TransformationExecutionServiceCache extends TransformerCache {
    private static final Logger LOG = LoggerFactory.getLogger(TransformationExecutionServiceCache.class);
    private final TransformationServiceFactory transformationServiceFactory;

    public List<TransformationExecutionService> transformationExecutionServices = new ArrayList<>();

    public TransformationExecutionServiceCache(
            TransformationServiceFactory transformationServiceFactory,
            ManagementClient managementClient
    ) {
        super(managementClient);
        this.transformationServiceFactory = transformationServiceFactory;
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
            .subscribe(this::addTransformationExecutionService);
    }

    public TransformationExecutionService getTransformationExecutionServiceByTransformerId(UUID transformerId) {
        return this.transformationExecutionServices.stream()
            .filter(t -> t.getTransformerId().equals(transformerId))
            .findFirst()
            .orElse(null);
    }

    private void addTransformationExecutionService(Transformer transformer) {
        this.transformationExecutionServices.add(
            this.transformationServiceFactory.createExecutionService(transformer)
        );
    }

    private void deleteTransformationExecutionService(Transformer transformer) {
        transformationExecutionServices.removeIf(
                t -> t.getTransformerId().equals(
                        transformer.getId()
                )
        );
    }

    private void handleTransformerEvent(TransformerChangeEvent event) {
        switch (event.getType()) {
            case CREATE:
            case UPDATE:
                addTransformationExecutionService(event.getTransformer());
                break;
            case DELETE:
                deleteTransformationExecutionService(event.getTransformer());
                break;
        }
    }
}
