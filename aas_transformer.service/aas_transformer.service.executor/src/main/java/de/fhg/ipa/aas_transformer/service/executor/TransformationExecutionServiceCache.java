package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.TransformerCache;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEvent;
import de.fhg.ipa.aas_transformer.persistence.api.TransformationDescriptionJpaRepository;
import de.fhg.ipa.aas_transformer.transformation.TransformationExecutionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.Disposable;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TransformationExecutionServiceCache extends TransformerCache implements ApplicationListener<ContextClosedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(TransformationExecutionServiceCache.class);
    private final TransformationServiceFactory transformationServiceFactory;
    private Disposable transformerEventDisposable;
    public List<TransformationExecutionService> transformationExecutionServices = new ArrayList<>();

    public TransformationExecutionServiceCache(
            TransformationServiceFactory transformationServiceFactory,
            ManagementClient managementClient
    ) {
        super(managementClient);
        this.transformationServiceFactory = transformationServiceFactory;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        LOG.info("ContextClosedEvent received in TransformationExecutionServiceCache");
        this.transformerEventDisposable.dispose();
        LOG.info("Subscription to transformer events has been disposed");
    }

    @PostConstruct
    public void init() {
        this.waitForManagement();

        this.transformerFlux
                .doOnError(e -> {
                    LOG.warn(
                            "ManagementClient failed to connect: {} - retrying in {} ms...",
                            e.getMessage(),
                            this.connectionRetryTimeoutInMs
                    );
                })
                .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofMillis(this.connectionRetryTimeoutInMs)))
                .doOnComplete(() -> {
                    this.transformerEventDisposable = this.transformerEventFlux
                            .log()
                            .retry()
                            .subscribe(
                                    this::handleTransformerEvent,
                                    e -> {
                                        LOG.error("Error while handling transformer event: " + e.getMessage());
                                        this.init();
                                    },
                                    () -> {
                                        LOG.info("Connection to ManagementClient is complete. Reconnecting...");
                                        this.init();
                                    }
                            );
                })
                .subscribe(this::addTransformationExecutionService);
    }

    private void waitForManagement() {
        while(true) {
            try {
                this.managementClient.getAllTransformer().collectList().block();
                break;
            } catch (WebClientRequestException e) {
                LOG.error("Failed to connect to ManagementClient: " + e.getMessage());
                try {
                    Thread.sleep(this.connectionRetryTimeoutInMs);
                } catch (InterruptedException ie) {
                    LOG.error("Thread interrupted while waiting to retry connection: " + ie.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public TransformationExecutionService getTransformationExecutionServiceByTransformerId(UUID transformerId) {
        return this.transformationExecutionServices.stream()
            .filter(t -> t.getTransformerId().equals(transformerId))
            .findFirst()
            .orElse(null);
    }

    public List<UUID> getTransformerIds() {
        return this.transformationExecutionServices.stream()
            .map(TransformationExecutionService::getTransformerId)
            .toList();
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
            default:
                break;
        }
    }
}
