package de.fhg.ipa.aas_transformer.service.listener;

import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("")
public class ListenerRestController {
    @Autowired
    TransformationDetectionServiceCache transformationDetectionServiceCache;

    @RequestMapping(path = "/transformer-cache", method = RequestMethod.GET)
    @Operation(summary = "Get cached TransformerDTOListener objects")
    public @NotNull List<TransformerDTOListener> getTransformerCache() {
        return transformationDetectionServiceCache.getTransformerDTOListenerCache();
    }
}
