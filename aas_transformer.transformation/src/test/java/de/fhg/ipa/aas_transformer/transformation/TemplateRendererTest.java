package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplateRendererTest {

    @Test
    public void testHasTemplateExpectNoTemplate() {
        String templateId = "aas_transformer:template";
        assertFalse(TemplateRenderer.hasTemplate(templateId));
    }

    @Test
    public void testHasTemplateWithOneTemplateExpectTemplate() {
        String templateId = "{{ submodel:id(SOURCE_SUBMODEL) }}_red";
        assertTrue(TemplateRenderer.hasTemplate(templateId));
    }

    @Test
    public void testHasTemplateWithTwoTemplatesExpectTemplate() {
        String templateId = "{{ submodel:id(SOURCE_SUBMODEL) }}{{ submodel:id(SOURCE_SUBMODEL) }}_red";
        assertTrue(TemplateRenderer.hasTemplate(templateId));
    }
}
