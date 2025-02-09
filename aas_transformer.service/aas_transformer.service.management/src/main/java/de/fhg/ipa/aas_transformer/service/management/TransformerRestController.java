package de.fhg.ipa.aas_transformer.service.management;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEvent;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEventDTOListener;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/aas/transformer")
public class TransformerRestController {
    static {
        io.swagger.v3.core.jackson.ModelResolver.enumsAsRef = true;
    }

    private static final Logger LOG = LoggerFactory.getLogger(TransformerRestController.class);

    private final TransformerJpaRepository transformerJpaRepository;
    private final TransformerHandler transformerHandler;

    public TransformerRestController(
            TransformerJpaRepository transformerJpaRepository,
            TransformerHandler transformerHandler
    ) {
        this.transformerJpaRepository = transformerJpaRepository;
        this.transformerHandler = transformerHandler;
    }

    @RequestMapping(method = RequestMethod.POST)
    @Operation(summary = "Create Transformer")
    public Mono<Transformer> createTransformer(
            @RequestBody Transformer transformer,
            @RequestParam(name = "execute", defaultValue = "false") Boolean execute
    ) {
        return transformerHandler.createOrUpdateTransformer(transformer, execute);
    }

    @RequestMapping(method = RequestMethod.GET)
    @Operation(summary = "Get all Transformer")
    public Flux<Transformer> getAllTransformer() {
        return transformerHandler.getAllTransformer();
    }


    @RequestMapping(method = RequestMethod.GET, value = "/dto-listener")
    @Operation(summary = "Get all Transformer as DTOListener")
    public Flux<TransformerDTOListener> getAllTransformerDTOListener() {
        return transformerHandler.getAllTransformerDTOListener();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Get Stream of Transformer Change Events")
    public Flux<TransformerChangeEvent> getTransformerChangeEventStream() {
        return transformerHandler.getTransformerChangeEventFlux();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/events/dto-listener", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Get Stream of Transformer Change Events as DTOListener")
    public Flux<TransformerChangeEventDTOListener> getTransformerChangeEventDTOListenerStream() {
        return transformerHandler.getTransformerChangeEventDTOListenerFlux();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{transformerId}")
    @Operation(summary = "Get one Transformer")
    public Mono<Transformer> getTransformer(@PathVariable(name = "transformerId") UUID transformerId) {
        return transformerJpaRepository.findById(transformerId);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{transformerId}")
    @Operation(summary = "Delete one Transformer")
    public void deleteTransformer(@PathVariable(name = "transformerId") UUID transformerId, @RequestParam(name = "doCleanup", defaultValue = "false") Boolean doCleanup) {
        transformerHandler.deleteTransformer(transformerId, doCleanup);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{transformerId}/destination-submodels")
    @Operation(summary = "Get destination submodel IDs of a Transformer (destination definition must have no templates)")
    public List<String> getDestinationSubmodels(@PathVariable(name = "transformerId") UUID transformerId) {
        return transformerHandler.getDestinationSubmodelIds(transformerId);
    }


    @RequestMapping(method = RequestMethod.POST, value = "/{transformerId}/orphaned-submodels")
    @Operation(summary = "Get orphaned destination submodel IDs of a Transformer. Might return empty list even if orphans exist.")
    public List<String> getOrphanedDestinationSubmodels(
            @RequestBody Submodel sourceSubmodel,
            @PathVariable(name = "transformerId") UUID transformerId
    ) throws DeserializationException {
        return transformerHandler.getOrphanedDestinationSubmodelIds(transformerId, sourceSubmodel);
    }
}
