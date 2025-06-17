package de.fhg.ipa.aas_transformer.service.listener;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.TransformerDTOListenerCache;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.service.listener.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.transformation.TransformationDetectionService;
import de.fhg.ipa.aas_transformer.transformation.TransformationUtils;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TransformationDetectionServiceCache extends TransformerDTOListenerCache implements ApplicationListener<ContextClosedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(TransformationDetectionServiceCache.class);

    private final SubmodelRepository submodelRepository;
    private final TemplateRenderer templateRenderer;
    private final TransformationUtils transformationUtils;
    private final AasRegistry aasRegistry;
    private final SubmodelRegistry submodelRegistry;
    public List<TransformationDetectionService> transformationDetectionServices = new ArrayList<>();
    private Disposable transformerEventDisposable;

    @Value("${aas_transformer.services.listener.strict-mode.enabled:false}")
    private boolean strictModeEnabled;

    @Value("${aas_transformer.services.listener.avoid-recursive-calls.enabled:false}")
    private boolean avoidRecursiveCallsEnabled;

    public TransformationDetectionServiceCache(
            ManagementClient managementClient,
            AasRegistry aasRegistry,
            SubmodelRegistry submodelRegistry,
            SubmodelRepository submodelRepository,
            TemplateRenderer templateRenderer,
            TransformationUtils transformationUtils
    ) {
        super(managementClient);
        this.aasRegistry = aasRegistry;
        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;
        this.templateRenderer = templateRenderer;
        this.transformationUtils = transformationUtils;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        LOG.info("ContextClosedEvent received in TransformationDetectionServiceCache");
        this.transformerEventDisposable.dispose();
        LOG.info("Subscription to transformer events has been disposed");
    }

    @PostConstruct
    public void init() {
        this.transformerFlux
            .doOnError(e -> {
                LOG.warn("ManagementClient failed to connect: " + e.getMessage() + " - retrying in " + this.connectionRetryTimeoutInMs + " ms...");
            })
            .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofMillis(this.connectionRetryTimeoutInMs)))
            .doOnComplete(() -> {
                transformerEventDisposable = this.transformerEventFlux
                        .log()
                        .retry()
                        .subscribe(
                                this::handleTransformerEvent,
                                e -> {
                                    LOG.error("Error while handling transformer event: " + e.getMessage());
                                    this.init();
                                },
                                () -> {
                                    LOG.info("Connection to ManagementClient lost. Reconnecting...");
                                    this.init();
                                }
                        );
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
                .filter(service -> !avoidRecursiveCallsEnabled || !service.isSubmodelDestinationOfTransformerAction(event.getSubmodel()))
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
                if(service.getTransformerDTOListener().getTransformOnRequest())
                    return List.of(new TransformationJob(
                            TransformationJobAction.DELETE,
                            service.getTransformerDTOListener().getId(),
                            event.getSubmodel().getId(),
                            null
                    ));
                else
                    return managementClient
                            .getOrphanedDestinationSubmodels(
                                    service.getTransformerDTOListener().getId(),
                                    (DefaultSubmodel) event.getSubmodel()
                            )
                            .block()
                            .stream()
                            .map(smId -> new TransformationJob(
                                    TransformationJobAction.DELETE,
                                    service.getTransformerDTOListener().getId(),
                                    smId,
                                    null
                            ))
                            .collect(Collectors.toList());
            default:
                return null;

        }
    }

    private void addTransformationDetectionService(TransformerDTOListener transformerDTOListener) {
        transformationDetectionServices.add(
                new TransformationDetectionService(
                        transformerDTOListener,
                        templateRenderer,
                        transformationUtils,
                        aasRegistry,
                        submodelRegistry
                )
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
            default:
                break;
        }
    }
}
