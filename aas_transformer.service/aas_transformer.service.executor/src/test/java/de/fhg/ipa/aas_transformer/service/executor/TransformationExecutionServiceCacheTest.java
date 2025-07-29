package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEvent;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEventDTOListener;
import de.fhg.ipa.aas_transformer.persistence.api.TransformationDescriptionJpaRepository;
import de.fhg.ipa.aas_transformer.transformation.TransformationUtils;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionServiceFactory;
import de.fhg.ipa.aas_transformer.transformation.templating.AasTemplateRenderer;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {
        TransformationExecutionServiceCache.class,
        TransformationServiceFactory.class,
        ManagementClient.class,
        TemplateRenderer.class,
        AasTemplateRenderer.class,
        TransformationUtils.class,
        AasRegistry.class,
        AasRepository.class,
        SubmodelRegistry.class,
        SubmodelRepository.class,
        TransformerActionServiceFactory.class,
        TransformationDescriptionJpaRepository.class,
        MetricsClient.class
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class TransformationExecutionServiceCacheTest {
    @MockBean
    TransformationDescriptionJpaRepository transformationDescriptionJpaRepository;
    @Autowired
    TransformationExecutionServiceCache transformationExecutionServiceCache;

    // Mocks ManagementClient; Client return testTransformer
    @TestConfiguration
    public static class TestConfig {
        @MockBean
        ManagementClient managementClient;
        static Sinks.Many<TransformerChangeEvent> changeEventSink =
                Sinks.many().unicast().onBackpressureBuffer();
        static Flux<TransformerChangeEvent> changeEventFlux = changeEventSink.asFlux();

        @PostConstruct
        public void initMock() {
            Mockito
                    .when(managementClient.getAllTransformer())
                    .thenReturn(Flux.empty());
            Mockito
                    .when(managementClient.getAllTransformerDTOListener())
                    .thenReturn(Flux.empty());
            Mockito
                    .when(managementClient.getTransformerChangeEventStream())
                    .thenReturn(changeEventFlux);
            Mockito
                    .when(managementClient.getTransformerChangeEventDTOListenerStream())
                    .thenReturn(Flux.empty());
        }
    }

    @Test
    @Order(10)
    public void testContext() {
        assertNotNull(transformationExecutionServiceCache);
    }

    @Test
    @Order(20)
    public void test() {
        TestConfig.changeEventSink.tryEmitNext(new TransformerChangeEvent());
    }

}
