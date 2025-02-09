package de.fhg.ipa.aas_transformer.service.management;

import com.hubspot.jinjava.interpret.InterpretException;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.management.converter.modelmapper.TransformerChangeEventToTransformerChangeEventDTOListenerConverter;
import de.fhg.ipa.aas_transformer.service.management.converter.modelmapper.TransformerToTransformerDTOListenerConverter;
import de.fhg.ipa.aas_transformer.transformation.TransformationDetectionService;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.*;

import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.DELETE;
import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.transformation.TransformationDetectionUtils.isSubmodelSourceOfTransformer;
import static de.fhg.ipa.aas_transformer.transformation.TransformationDetectionUtils.lookupSourceSubmodels;
import static de.fhg.ipa.aas_transformer.transformation.TransformationExecutionService.lookupDestinationShells;
import static de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer.hasTemplate;
import static java.util.stream.Collectors.toList;

@Component
public class TransformerHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerHandler.class);
    private final ModelMapper modelMapper;
    private final AasRepository aasRepository;
    private final SubmodelRepository submodelRepository;
    private final TransformerJpaRepository transformerJpaRepository;
    private final RedisJobProducer redisJobProducer;
    private final TemplateRenderer templateRenderer;

    private final Sinks.Many<TransformerChangeEvent> transformerChangeEventSink =
            Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<TransformerChangeEventDTOListener> transformerChangeEventDTOListenerSink =
            Sinks.many().multicast().onBackpressureBuffer();

    public TransformerHandler(
            AasRepository aasRepository,
            SubmodelRepository submodelRepository,
            TransformerJpaRepository transformerJpaRepository,
            ModelMapper modelMapper,
            RedisJobProducer redisJobProducer,
            TemplateRenderer templateRenderer
    ) {
        this.aasRepository = aasRepository;
        this.submodelRepository = submodelRepository;
        this.transformerJpaRepository = transformerJpaRepository;
        this.modelMapper = modelMapper;
        this.redisJobProducer = redisJobProducer;
        this.templateRenderer = templateRenderer;

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
        return this.transformerJpaRepository
                .save(transformer)
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(t -> {
                    emitToSink(new TransformerChangeEvent(TransformerChangeEventType.CREATE, t));
                    if(execute) {
                        pushTransformationJobsAfterCreate(t);
                    }
                });
    }

    private void pushTransformationJobsAfterCreate(Transformer transformer) {
        TransformationDetectionService service = new TransformationDetectionService(
            modelMapper.map(transformer, TransformerDTOListener.class)
        );
        // get all submodels
        try {
            this.submodelRepository.getAllSubmodels().stream()
                    //filter all submodels being source of transformation
                    .filter(service::isSubmodelSourceOfTransformerActions)
                    .map(submodel -> {
                        // create TransformationJob
                        return new TransformationJob(
                                EXECUTE,
                                transformer.getId(),
                                submodel.getId(),
                                null
                        );
                    })
                    .forEach(job -> {
                        // push job for each submodel being source of transformation
                        try {
                            this.redisJobProducer.pushJob(job);
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (DeserializationException e) {
            LOG.error("Failed to get all submodels to push jobs for transformer: {} | {}", transformer.getId(), e.getMessage());
        }
    }

    public void deleteTransformer(UUID transformerId, Boolean doCleanup) {
        this.transformerJpaRepository
            .findById(transformerId)
            .publishOn(Schedulers.boundedElastic())
            .subscribe(t -> {
                if (doCleanup)
                    pushTransformationJobsAfterDelete(t);
                this.transformerJpaRepository
                    .delete(t)
                    .publishOn(Schedulers.boundedElastic())
                    .doOnSuccess(v -> {
                        emitToSink(new TransformerChangeEvent(TransformerChangeEventType.DELETE, t));
                    })
                    .subscribe();
            });
    }

    private void pushTransformationJobsAfterDelete(Transformer t) {
        this.getDestinationSubmodelIds(t.getId())
                // Create a TransformationJob for each destination submodel
                .forEach(submodelId -> {
                    try {
                        this.redisJobProducer.pushJob(new TransformationJob(
                                DELETE,
                                null,
                                submodelId,
                                null
                        ));
                    } catch (SerializationException e) {
                        throw new RuntimeException(e);
                    }
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
        Transformer t = this.transformerJpaRepository.findById(transformerId).block();
        List<Submodel> orphanDestinationSubmodels = new ArrayList<>();
        String destinationShellId = null;

        Map<String, Object> templateContext = this.templateRenderer.getTemplateContext(
                t.getId(),
                new ArrayList<>(),
                sourceSubmodel
        );

        if(hasTransformerAasDestination(t)) {
            destinationShellId = t.getDestination().getAasDestination().getId();

            if(hasTemplate(destinationShellId)) {
                try {
                    destinationShellId = templateRenderer.render(destinationShellId, templateContext);
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
        AssetAdministrationShell destinationShell = null;

        if(destinationShellId != null) {
            destinationShell = aasRepository.getAas(destinationShellId);
            templateContext = templateRenderer.getTemplateContext(
                    t.getId(),
                    List.of(destinationShell),
                    sourceSubmodel
            );

            try {
                String destinationSubmodelId = templateRenderer.render(destinationSubmodelDef.getId(), templateContext);
                return List.of(
                        submodelRepository.getSubmodel(destinationSubmodelId)
                );
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
            String destinationSubmodelIdShort = this.templateRenderer.render(
                    destinationSubmodelDef.getIdShort(),
                    templateContext
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
        Transformer t = this.transformerJpaRepository.findById(transformerId).block();
        List<Submodel> sourceSubmodels = lookupSourceSubmodels(t, this.submodelRepository);
        List<String> destinationSubmodelIds = new ArrayList<>();

        for(Submodel sourceSubmodel : sourceSubmodels) {
            List<AssetAdministrationShell> destinationShells = lookupDestinationShells(
                    aasRepository,
                    sourceSubmodel.getId(),
                    t.getDestination().getAasDestination()
            );

            Map<String, Object> templateContext = this.templateRenderer.getTemplateContext(
                    t.getId(),
                    destinationShells,
                    sourceSubmodel
            );

            destinationSubmodelIds.add(templateRenderer.render(
                    t.getDestination().getSubmodelDestination().getId(),
                    templateContext
            ));
        }

        return destinationSubmodelIds;
    }

    public boolean hasTransformerAasDestination(Transformer t) {
        DestinationAAS aas = t.getDestination().getAasDestination();

        return !(aas == null || aas.getId() == null || aas.getId().equals(""));
    }

    public Flux<Transformer> getAllTransformer() {
        return transformerJpaRepository.findAll();
    }
    
    public Flux<TransformerDTOListener> getAllTransformerDTOListener() {
        return transformerJpaRepository.findAll().map(this::convertToTransformerDTOListener);
    }

    public Flux<TransformerChangeEvent> getTransformerChangeEventFlux() {
        Flux<TransformerChangeEvent> flux = transformerChangeEventSink.asFlux();
        return flux;

    }

    public Flux<TransformerChangeEventDTOListener> getTransformerChangeEventDTOListenerFlux() {
        Flux<TransformerChangeEventDTOListener> flux = transformerChangeEventDTOListenerSink.asFlux();
        return flux;
    }
}
