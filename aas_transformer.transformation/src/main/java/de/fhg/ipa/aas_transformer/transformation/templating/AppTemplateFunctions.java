package de.fhg.ipa.aas_transformer.transformation.templating;

import com.hubspot.jinjava.lib.fn.InjectedContextFunctionProxy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AppTemplateFunctions extends AbstractTemplateFunctions {

    private static final String NAMESPACE = "app";

    private final Environment environment;

    public AppTemplateFunctions
            (Environment environment) {
        this.environment = environment;

        try {
            var getValueOfPropertyMethodRef = AppTemplateFunctions.class.getDeclaredMethod(
                    "getValueOfProperty",
                    String.class);
            var getAppPropTemplateFunction = InjectedContextFunctionProxy.defineProxy(
                    AppTemplateFunctions.NAMESPACE, "prop",
                    getValueOfPropertyMethodRef, this);
            this.templateFunctions.add(getAppPropTemplateFunction);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public String getValueOfProperty(String propertyPath) {
        var value = this.environment.getProperty(propertyPath);
        return value;
    }

}
