package de.fhg.ipa.aas_transformer.service.templating;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TemplateRenderer {

    private Jinjava jinjava = new Jinjava();

    private Map<String, Object> globalRenderContext = new HashMap<>();

    public TemplateRenderer(AASTemplateFunctions aasTemplateFunctions, AppTemplateFunctions appTemplateFunctions, UUIDTemplateFunctions uuidTemplateFunctions) {
        for (var templateFunctionsClass : List.of(
                aasTemplateFunctions,
                appTemplateFunctions,
                uuidTemplateFunctions)
        ) {
            for (var templateFunction : templateFunctionsClass.getTemplateFunctions()) {
                this.jinjava.getGlobalContext().registerFunction(templateFunction);
            }
        }
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
