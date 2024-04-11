package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.service.templating.AppTemplateFunctions;
import de.fhg.ipa.aas_transformer.service.templating.SubmodelTemplateFunctions;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import de.fhg.ipa.aas_transformer.service.templating.UUIDTemplateFunctions;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class TemplateRendererTest extends AbstractIT {

    private TemplateRenderer templateRenderer;

    private Map<String, Object> propertiesMap = Map.of("aas.submodel-repository.url", "http://localhost:8081");

    @BeforeAll
    public void beforeAll() throws IOException, DeserializationException {
        super.beforeAll();

        var submodelTemplateFunctions = new SubmodelTemplateFunctions(this.submodelRegistry, this.submodelRepository);

        var environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource("propertiesMap", propertiesMap));
        var appTemplateFunctions = new AppTemplateFunctions(environment);

        var uuidTemplateFunctions = new UUIDTemplateFunctions();
        this.templateRenderer = new TemplateRenderer(submodelTemplateFunctions, appTemplateFunctions, uuidTemplateFunctions);
    }

    @Nested
    public class SubmodelTemplateFunctionsTests {
        @Test
        public void getSubmodelElementValueOfSubmodel() throws IOException, DeserializationException, SerializationException {
            var templateString = "{{ submodel:sme_value(SOURCE_SUBMODEL,  'SimpleProperty') }}";

            var jsonSerializer = new JsonSerializer();
            var serializedSourceSubmodel = jsonSerializer.write(GenericTestConfig.getSimpleSubmodel());
            Map<String, Object> renderContext = Map.of("SOURCE_SUBMODEL", serializedSourceSubmodel);

            var result = templateRenderer.render(templateString, renderContext);
            assertThat(result).isEqualTo("SimpleValue");
        }
    }

    @Nested
    public class UuidTemplateFunctionsTests {
        @Test
        public void generateUUID() {
            var templateString = "{{ uuid:generate() }}";
            var result = templateRenderer.render(templateString);
            assertThatCode(() -> { UUID.fromString(result); }).doesNotThrowAnyException();
        }
    }

    @Nested
    public class AppTemplateFunctionsTests {
        @Test
        public void getApplicationProperty() {
            var templateString = "{{ app:prop('aas.submodel-repository.url') }}";
            var result = templateRenderer.render(templateString);
            assertThat(result).isEqualTo("http://localhost:8081");
        }
    }
}
