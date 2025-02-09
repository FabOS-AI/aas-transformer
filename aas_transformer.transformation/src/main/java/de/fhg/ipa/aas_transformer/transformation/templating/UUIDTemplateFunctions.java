package de.fhg.ipa.aas_transformer.transformation.templating;

import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UUIDTemplateFunctions extends AbstractTemplateFunctions {

    public static final String NAMESPACE = "uuid";

    public UUIDTemplateFunctions() {
        try {
            var generateUUIDMethodRef = UUIDTemplateFunctions.class.getDeclaredMethod("generateUUID");
            var generateUUIDTemplateMethod = new ELFunctionDefinition(
                    UUIDTemplateFunctions.NAMESPACE, "generate", generateUUIDMethodRef);
            this.templateFunctions.add(generateUUIDTemplateMethod);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

}
