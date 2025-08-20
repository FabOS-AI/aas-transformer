package de.fhg.ipa.aas_transformer.service.management;

import com.hubspot.jinjava.interpret.InterpretException;
import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.persistence.api.TransformationDescriptionJpaRepository;
import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.management.converter.modelmapper.TransformerChangeEventToTransformerChangeEventDTOListenerConverter;
import de.fhg.ipa.aas_transformer.service.management.converter.modelmapper.TransformerToTransformerDTOListenerConverter;
import de.fhg.ipa.aas_transformer.transformation.TransformationDetectionService;
import de.fhg.ipa.aas_transformer.transformation.TransformationUtils;
import de.fhg.ipa.aas_transformer.transformation.templating.AasTemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.DELETE;
import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.transformation.TransformationDetectionUtils.isSubmodelSourceOfTransformer;
import static de.fhg.ipa.aas_transformer.transformation.TransformationDetectionUtils.lookupSourceSubmodels;
import static de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer.hasTemplate;

@Component
public class TransformerHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerHandler.class);
    private final ModelMapper modelMapper;
    private final AasRegistry aasRegistry;
    private final AasRepository aasRepository;
    private final SubmodelRegistry submodelRegistry;
    private final SubmodelRepository submodelRepository;
    private final TransformerJpaRepository transformerJpaRepository;
    private final TransformationDescriptionJpaRepository transformationDescriptionJpaRepository;
    private final RedisJobProducer redisJobProducer;
    private final AasTemplateRenderer aasTemplateRenderer;

    private final Sinks.Many<TransformerChangeEvent> transformerChangeEventSink =
            Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<TransformerChangeEventDTOListener> transformerChangeEventDTOListenerSink =
            Sinks.many().multicast().onBackpressureBuffer();
    private final TransformationUtils transformationUtils;

    public TransformerHandler(
            AasRegistry aasRegistry,
            AasRepository aasRepository,
            SubmodelRegistry submodelRegistry,
            SubmodelRepository submodelRepository,
            TransformerJpaRepository transformerJpaRepository,
            TransformationDescriptionJpaRepository transformationDescriptionJpaRepository,
            ModelMapper modelMapper,
            RedisJobProducer redisJobProducer,
            AasTemplateRenderer aasTemplateRenderer,
            TransformationUtils transformationUtils
    ) {
        this.aasRegistry = aasRegistry;
        this.aasRepository = aasRepository;
        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;
        this.transformerJpaRepository = transformerJpaRepository;
        this.transformationDescriptionJpaRepository = transformationDescriptionJpaRepository;
        this.modelMapper = modelMapper;
        this.redisJobProducer = redisJobProducer;
        this.aasTemplateRenderer = aasTemplateRenderer;
        this.transformationUtils = transformationUtils;

        // Set Model Mapper Converters:
        modelMapper.addConverter(new TransformerToTransformerDTOListenerConverter());
        modelMapper.addConverter(new TransformerChangeEventToTransformerChangeEventDTOListenerConverter());
    }

    private TransformerDTOListener convertToTransformerDTOListener(Transformer transformer) {
        return modelMapper.map(transformer, TransformerDTOListener.class);
    }

    private TransformerChangeEventDTOListener convertToTransformerChangeEventDTOListener(TransformerChangeEvent transformerChangeEvent) {
        return modelMapper.map(transformerChangeEvent, TransformerChangeEventDTOListener.class);
    }

    private void emitToSink(TransformerChangeEvent transformerChangeEvent) {
        transformerChangeEventSink.tryEmitNext(transformerChangeEvent);
        transformerChangeEventDTOListenerSink.tryEmitNext(
                convertToTransformerChangeEventDTOListener(transformerChangeEvent)
        );
    }

//    @Transactional
    public Mono<Transformer> createOrUpdateTransformer(Transformer transformer, Boolean execute) {
        Transformer existingTransformer = transformerJpaRepository.findById(transformer.getId()).block();

        if(existingTransformer != null) {
            LOG.info("Update transformer with ID: {}", transformer.getId());
            return updateTransformer(existingTransformer, transformer, execute);
        } else {
            LOG.info("Creating transformer with ID: {}", transformer.getId());
            return createTransformer(transformer, execute);
        }
    }

    private Mono<Transformer> createTransformer(Transformer transformer, Boolean execute) {
        return this.transformerJpaRepository
                .save(transformer)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1)))
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(t -> {
                    LOG.info("Transformer created with ID: {}", transformer.getId());
                    emitToSink(new TransformerChangeEvent(TransformerChangeEventType.CREATE, t));
                    if(execute) {
                        pushTransformationJobsAfterCreate(t);
                    }
                });
    }

    private Mono<Transformer> updateTransformer(Transformer existingTransformer, Transformer newTransformer, Boolean execute) {
        existingTransformer.setDestination(newTransformer.getDestination());
        existingTransformer.setTransformerActions(newTransformer.getTransformerActions());
        existingTransformer.setSourceSubmodelIdRules(newTransformer.getSourceSubmodelIdRules());
        existingTransformer.setTransformOnRequest(newTransformer.getTransformOnRequest());

        return createTransformer(existingTransformer, execute);
    }

    private void pushTransformationJobsAfterCreate(Transformer transformer) {
        TransformationDetectionService service = new TransformationDetectionService(
            modelMapper.map(transformer, TransformerDTOListener.class),
            aasTemplateRenderer,
            transformationUtils,
            aasRegistry,
            submodelRegistry
        );
        // get all submodels
        try {
            this.submodelRepository.getAllSubmodels().stream()
                    //filter all submodels being source of transformation
                    .filter(service::isSubmodelSourceOfTransformerActions)
                    .map(submodel -> {
                        // render target submodel ID:
                        String destinationSubmodelId = aasTemplateRenderer.renderDestinationSubmodelId(
                          transformer.getId(),
                          transformer.getDestination(),
                          submodel
                        );

                        // create TransformationJob
                        return new TransformationJob(
                                UUID.randomUUID(),
                                EXECUTE,
                                transformer.getId(),
                                submodel.getId(),
                                null,
                                destinationSubmodelId
                        );
                    })
                    .forEach(job -> {
                        // push job for each submodel being source of transformation
                        this.redisJobProducer.pushJob(job);
                    });
        } catch (DeserializationException e) {
            LOG.error("Failed to get all submodels to push jobs for transformer: {} | {}", transformer.getId(), e.getMessage());
        }
    }

    public void deleteTransformer(UUID transformerId, Boolean doCleanup) {
        this.transformerJpaRepository
            .findById(transformerId)
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1)))
            .publishOn(Schedulers.boundedElastic())
            .subscribe(t -> {
                if (doCleanup)
                    pushTransformationJobsAfterDelete(t);
                this.transformerJpaRepository
                    .delete(t)
                    .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1)))
                    .publishOn(Schedulers.boundedElastic())
                    .doOnSuccess(v -> {
                        emitToSink(new TransformerChangeEvent(TransformerChangeEventType.DELETE, t));
                        LOG.info("Deleted transformer with ID: {}", transformerId);
                    })
                    .block();
                this.transformationDescriptionJpaRepository
                    .findByTransformerId(transformerId)
                    .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1)))
                    .subscribe(td -> {
                        this.submodelRegistry.deleteSubmodelDescriptor(td.getTargetSubmodelId());
                        this.transformationDescriptionJpaRepository
                                .delete(td)
                                .doOnSuccess(v ->
                                        LOG.info("Deleted transformation description with ID: {}", td.getId())
                                )
                                .block();
                    });
            });
    }

    private void pushTransformationJobsAfterDelete(Transformer t) {
        this.getDestinationSubmodelIds(t.getId())
                // Create a TransformationJob for each destination submodel
                .forEach(submodelId -> {
                    // render target submodel ID:
                    this.redisJobProducer.pushJob(new TransformationJob(
                            UUID.randomUUID(),
                            DELETE,
                            t.getId(),
                            submodelId,
                            null,
                            null
                    ));
                });
    }

    public boolean hasShellOrphanDestinationSubmodel(AssetAdministrationShell shell, Transformer t) {
        boolean hasOrphanDestinationSubmodel = false;
        for (Reference shellSubmodelRef : shell.getSubmodels()) {
            // Get all submodels of  shell
            List<Submodel> shellSubmodels = shellSubmodelRef.getKeys()
                    .stream()
                    .filter(k -> k.getType().equals(KeyTypes.SUBMODEL))
                    .map(Key::getValue)
                    .map(submodelId -> submodelRepository.getSubmodel(submodelId))
                    .toList();

            // check if one of shell submodels is source of transformer
            Optional<Submodel> optionalSourceSubmodel = shellSubmodels
                    .stream()
                    .filter(s -> isSubmodelSourceOfTransformer(s, t.getSourceSubmodelIdRules()))
                    .findFirst();

            // if no source submodel is found, return true/shell has orphan destination submodels based on transformer definition
            if (optionalSourceSubmodel.isEmpty()) {
                hasOrphanDestinationSubmodel = true;
                break;
            }
        }
        return hasOrphanDestinationSubmodel;
    }

    public List<Submodel> getOrphanDestinationSubmodelsTransformerId(UUID transformerId, Submodel sourceSubmodel) throws DeserializationException {
        Transformer t = this.transformerJpaRepository.findById(transformerId).retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1))).block();
        if (t == null) {
            LOG.error("Transformer with ID {} not found", transformerId);
            return List.of();
        }
        List<Submodel> orphanDestinationSubmodels = new ArrayList<>();
        String destinationShellId = null;

        if(hasTransformerAasDestination(t)) {
            destinationShellId = t.getDestination().getAasDestination().getId();

            if(hasTemplate(destinationShellId)) {
                try {
                    destinationShellId = aasTemplateRenderer.renderDestinationShellId(
                            t.getId(),
                            t.getDestination(),
                            sourceSubmodel
                    );
                } catch(InterpretException e) {
                    LOG.warn("""
                    Destination AAS is defined with templates in transformer but data missing to render template.
                    Trying to determine orphaned destination submodels of transformer {} without destination AAS.
                    """, t
                    );

                    destinationShellId = null;
                }
            }
        }

        DestinationSubmodel destinationSubmodelDef = t.getDestination().getSubmodelDestination();

        if(destinationShellId != null) {
            try {
                String destinationSubmodelId = aasTemplateRenderer.renderDestinationSubmodelId(
                        t.getId(),
                        t.getDestination(),
                        sourceSubmodel
                );
                return List.of(submodelRepository.getSubmodel(destinationSubmodelId));
            } catch (InterpretException e) {
                LOG.error("Failed to render destination submodel ID template: {}", e.getMessage());
            }
        }

        // the upcoming code requires AasDestination Definition to be missing in transformer otherwise just return empty list
        if(hasTransformerAasDestination(t))
            return orphanDestinationSubmodels;

        // get orphaned destination submodel based on idShort from definition of destination submodel in transformer
        // and the fact that AasDestination Definition is missing in transformer => source and destination submodel have been in the same shell
        try {
            String destinationSubmodelIdShort = this.aasTemplateRenderer.renderDestinationSubmodelIdShort(
                    t.getId(),
                    t.getDestination(),
                    sourceSubmodel
            );
            List<Submodel> destinationSubmodels = this.submodelRepository.getAllSubmodels()
                    .stream()
                    .filter(s -> s.getIdShort().equals(destinationSubmodelIdShort))
                    .toList();

            for(Submodel destinationSubmodel : destinationSubmodels) {
                List<AssetAdministrationShell> destinationShells;
                destinationShells = aasRepository.getAllAasContainingSubmodelBySubmodelId(
                        destinationSubmodel.getId()
                );

                for (AssetAdministrationShell shell : destinationShells) {
                    // Check if shell has multiple submodels with same destination idShort => orphan detection not possible:
                    if(hasShellSubmodelsWithSameIdShort(shell, destinationSubmodelIdShort))
                        continue;
                    
                    if (hasShellOrphanDestinationSubmodel(shell, t))
                        orphanDestinationSubmodels.add(destinationSubmodel);
                }
            }
        } catch(InterpretException e) {
            LOG.error("Failed to render destination submodel IDShort template: {}", e.getMessage());
        }
        return orphanDestinationSubmodels;
    }

    private boolean hasShellSubmodelsWithSameIdShort(AssetAdministrationShell shell, String destinationSubmodelIdShort) {
        long duplicateIdShortCount = shell.getSubmodels().stream()
                .flatMap(s -> s.getKeys().stream()
                        .filter(k -> k.getType().equals(KeyTypes.SUBMODEL))
                        .map(Key::getValue)
                        .map(submodelId -> submodelRepository.getSubmodel(submodelId))
                )
                .filter(s -> s.getIdShort().equals(destinationSubmodelIdShort))
                .count();
        return duplicateIdShortCount > 1;
    }

    public List<String> getOrphanedDestinationSubmodelIds(UUID transformerId, Submodel sourceSubmodel) throws DeserializationException {
        return getOrphanDestinationSubmodelsTransformerId(transformerId, sourceSubmodel)
                .stream()
                .map(Submodel::getId)
                .toList();
    }

    // requires source submodel to be present in submodel repository
    public List<String> getDestinationSubmodelIds(UUID transformerId) {
        Transformer t = this.transformerJpaRepository.findById(transformerId).retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1))).block();
        List<Submodel> sourceSubmodels = lookupSourceSubmodels(t, this.submodelRepository);
        List<String> destinationSubmodelIds = new ArrayList<>();

        for(Submodel sourceSubmodel : sourceSubmodels) {
            destinationSubmodelIds.add(
                    aasTemplateRenderer.renderDestinationSubmodelId(
                            transformerId,
                            t.getDestination(),
                            sourceSubmodel
                    )
            );
        }

        return destinationSubmodelIds;
    }

    public boolean hasTransformerAasDestination(Transformer t) {
        DestinationAAS aas = t.getDestination().getAasDestination();

        return !(aas == null || aas.getId() == null || aas.getId().equals(""));
    }

    public Flux<Transformer> getAllTransformer() {
        return transformerJpaRepository.findAll().retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1)));
    }
    
    public Flux<TransformerDTOListener> getAllTransformerDTOListener() {
        return transformerJpaRepository.findAll().retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1))).map(this::convertToTransformerDTOListener);
    }

    public Flux<TransformerChangeEvent> getTransformerChangeEventFlux() {
        Flux<TransformerChangeEvent> flux = transformerChangeEventSink.asFlux();
        Flux<TransformerChangeEvent> keepAliveFlux = Flux.interval(Duration.ofSeconds(10)).map(tick ->
                new TransformerChangeEvent(TransformerChangeEventType.KEEP_ALIVE, null)
        );
        return Flux.merge(flux, keepAliveFlux);

    }

    public Flux<TransformerChangeEventDTOListener> getTransformerChangeEventDTOListenerFlux() {
        Flux<TransformerChangeEventDTOListener> flux = transformerChangeEventDTOListenerSink.asFlux();
        Flux<TransformerChangeEventDTOListener> keepAliveFlux = Flux.interval(Duration.ofSeconds(10)).map(tick ->
                new TransformerChangeEventDTOListener(TransformerChangeEventType.KEEP_ALIVE, null)
        );
        return Flux.merge(flux, keepAliveFlux);
    }
}
