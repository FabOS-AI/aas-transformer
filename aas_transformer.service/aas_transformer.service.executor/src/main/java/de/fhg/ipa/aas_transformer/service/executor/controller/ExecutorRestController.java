package de.fhg.ipa.aas_transformer.service.executor.controller;

import de.fhg.ipa.aas_transformer.service.executor.TransformationExecutionServiceCache;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("")
public class ExecutorRestController {
    @Autowired
    TransformationExecutionServiceCache transformationExecutionServiceCache;

    @RequestMapping(path = "/transformer-id-list", method = RequestMethod.GET)
    @Operation(summary = "Get cached transformers as list of ids")
    public List<UUID> getTransformerIds() {
        return transformationExecutionServiceCache.getTransformerIds();
    }
}
