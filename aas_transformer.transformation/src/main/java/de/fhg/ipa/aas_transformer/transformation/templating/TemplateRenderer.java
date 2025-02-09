package de.fhg.ipa.aas_transformer.transformation.templating;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class TemplateRenderer {
    private static final Logger LOG = LoggerFactory.getLogger(TemplateRenderer.class);

    private Jinjava jinjava = new Jinjava();
    private JsonSerializer jsonSerializer = new JsonSerializer();

    private Map<String, Object> globalRenderContext = new HashMap<>();

    public TemplateRenderer(List<AbstractTemplateFunctions> templateFunctions) {
        for (var templateFunctionsClass : templateFunctions) {
            for (var templateFunction : templateFunctionsClass.getTemplateFunctions()) {
                this.jinjava.getGlobalContext().registerFunction(templateFunction);
            }
        }
    }

    public Map<String, Object> getTemplateContext(
            UUID transformerId,
            List<AssetAdministrationShell> destinationShells,
            Submodel sourceSubmodel
    ) {
        var context = new HashMap<String, Object>();

        try {
            String serializedDestinationShells = jsonSerializer.write(destinationShells);
            context.put("DESTINATION_SHELLS", serializedDestinationShells);
        } catch (SerializationException e) {
            LOG.error(e.getMessage());
        }

        // Source Submodel:
        try {
            String serializedSourceSubmodel = jsonSerializer.write(sourceSubmodel);
            context.put("SOURCE_SUBMODEL", serializedSourceSubmodel);
        } catch (SerializationException e) {
            LOG.error(e.getMessage());
        }

        // UUID based on Transformer and source Submodel ID:
        String uuidBase = transformerId.toString() + sourceSubmodel.getId();
        byte[] uuidBaseBytes = uuidBase.getBytes(StandardCharsets.UTF_8);
        context.put("UUID", UUID.nameUUIDFromBytes(uuidBaseBytes));

        return context;
    }

    public static boolean hasTemplate(String inputString) {
        String regexp = "\\{\\{.*\\}\\}";
        var regExPattern = Pattern.compile(regexp);
        return regExPattern.matcher(inputString).matches();
    }

    public String render(String template) {
        return this.render(template, this.globalRenderContext);
    }

    public String render(String template, Map<String, Object> renderContext) {
        try {
            var combinedRenderContext = new HashMap<>(globalRenderContext);
            combinedRenderContext.putAll(renderContext);

            var result = this.jinjava.render(template, combinedRenderContext);
            return result;
        } catch (FatalTemplateErrorsException e) {

            e.getErrors().forEach(subError -> {
                subError.getException().printStackTrace();
            });

            throw e;
        }
    }

    public <T extends Map<String, Object>> T render(T map) {
        return this.render(map, this.globalRenderContext);
    }

    public <T extends Map<String, Object>> T render(T map, Map<String, Object> renderContext) {
        map.replaceAll((k, v) -> {
            var renderedValue = this.render(v.toString(), renderContext);
            return renderedValue;
        });

        return map;
    }
}
