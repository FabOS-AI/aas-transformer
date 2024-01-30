package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.service.templating.AASTemplateFunctions;
import de.fhg.ipa.aas_transformer.service.templating.AppTemplateFunctions;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import de.fhg.ipa.aas_transformer.service.templating.UUIDTemplateFunctions;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.AAS;
import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.objectMapper;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class AasTemplateRendererTest extends AbstractIT {

    private TemplateRenderer templateRenderer;

    private Map<String, Object> propertiesMap = Map.of("aas.server.url", "http://localhost:4001/aasServer");

    @BeforeAll
    public void beforeAll() throws IOException {
        super.beforeAll();

        var aasTemplateFunctions = new AASTemplateFunctions(this.aasRegistry, this.aasManager);

        var environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource("propertiesMap", propertiesMap));
        var appTemplateFunctions = new AppTemplateFunctions(environment);

        var uuidTemplateFunctions = new UUIDTemplateFunctions();
        this.templateRenderer = new TemplateRenderer(aasTemplateFunctions, appTemplateFunctions, uuidTemplateFunctions);

        aasManager.createAAS(AAS);
        String smIdShort =  objectMapper.readTree(AasTemplateRendererTestConfig.SUBMODEL_ANSIBLE_FACTS).get("idShort").asText();
        given()
                .contentType(ContentType.JSON)
                .body(AasTemplateRendererTestConfig.SUBMODEL_ANSIBLE_FACTS)
                .log()
                .all()
                .put(getAASServerUrl()+"/shells/"+AAS.getIdentification().getId()+"/aas/submodels/"+smIdShort)
                .then()
                .log()
                .all();
    }

    @Nested
    public class AasTemplateFunctionsTests {
        @Test
        public void resolveSubmodelElementValue() {
            var templateString = "{{ aas:sme_value('aas-id', 'ansible-facts',  'distribution') }}";
            var result = templateRenderer.render(templateString);
            assertThat(result).isEqualTo("Ubuntu");
        }

        @Test
        public void findSubmodelByPropertyValue() {
            var templateString = "{{ aas:find_sm_by_prop('distribution',  'Ubuntu') }}";
            var result = templateRenderer.render(templateString);
            assertThat(result).isEqualTo("ansible-facts");
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
            var templateString = "{{ app:prop('aas.server.url') }}";
            var result = templateRenderer.render(templateString);
            assertThat(result).isEqualTo("http://localhost:4001/aasServer");
        }
    }
}
