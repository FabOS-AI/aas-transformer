package de.fhg.ipa.aas_transformer.service;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/aas/transformer")
public class TransformerRestController {

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
    public void createTransformer(@RequestBody Transformer transformer,
                                  @RequestParam(name = "execute", defaultValue = "false") Boolean execute) {
        transformerHandler.createOrUpdateTransformer(transformer, execute);
    }

    @RequestMapping(method = RequestMethod.GET)
    @Operation(summary = "Get all Transformer")
    public List<Transformer> getTransformer() {
        return transformerJpaRepository.findAll();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{transformerId}")
    @Operation(summary = "Get one Transformer")
    public Optional<Transformer> getTransformer(@PathVariable(name = "transformerId") UUID transformerId) {
        return transformerJpaRepository.findById(transformerId);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{transformerId}")
    @Operation(summary = "Delete one Transformer")
    public void deleteTransformer(@PathVariable(name = "transformerId") UUID transformerId, @RequestParam(name = "doCleanup", defaultValue = "false") Boolean doCleanup) {
        transformerHandler.deleteTransformer(transformerId, doCleanup);
    }
}
