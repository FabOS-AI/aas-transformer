package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisTransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEvent;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEventDTOListener;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.clients.redis.RedisTestObjects.assertExpectedJobCount;

@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultiTransformationExecutorIT extends AbstractIT {

    // region Test vars
    // Test triples:
    static List<List<Object>> triples;

    static {
        try {
            triples = getRandomAnsibleFactsTriples(5, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    static Transformer testTransformer = (Transformer)triples.get(0).get(2);
    // endregion

    // Mocks ManagementClient; Client return factsTransformer
    @TestConfiguration
    public static class TestConfig {
        @MockBean
        ManagementClient managementClient;
        static Sinks.Many<TransformerChangeEvent> changeEventSink =
                Sinks.many().unicast().onBackpressureBuffer();
        static Sinks.Many<TransformerChangeEventDTOListener> changeEventDtoListenerSink =
                Sinks.many().unicast().onBackpressureBuffer();

        @PostConstruct
        public void initMock() {
            Mockito
                    .when(managementClient.getAllTransformer())
                    .thenReturn(Flux.just(testTransformer));
            Mockito
                    .when(managementClient.getAllTransformerDTOListener())
                    .thenReturn(Flux.empty());
            Mockito
                    .when(managementClient.getTransformerChangeEventStream())
                    .thenReturn(changeEventSink.asFlux());
            Mockito
                    .when(managementClient.getTransformerChangeEventDTOListenerStream())
                    .thenReturn(changeEventDtoListenerSink.asFlux());
        }
    }

    @Test
    @Order(10)
    public void testMultiTransformation() throws SerializationException, InterruptedException, DeserializationException, ApiException {
        // Register AAS objects from triples:
        registerAasObjectsFromTriples(aasRegistry, aasRepository, smRegistry, smRepository, triples);

        // Register jobs for transformation of triples:
        for(List<Object>triple : triples) {
            TransformationJob job = new TransformationJob(
                EXECUTE,
                testTransformer.getId(),
                ((Submodel)triple.get(1)).getId(),
                null,
                null
            );
            redisClient.rightPushJob(new RedisTransformationJob(job));
        }

        // Assert job count:
        assertExpectedJobCount(redisJobReader, 0);

        // Assert submodel count:
        for(List<Object>triple : triples) {
            assertExpectedSubmodelCount(
                    aasRegistry,
                    aasRepository,
                    smRepository,
                    ((AssetAdministrationShell) triple.get(0)).getId(),
                    triples.size()*2,
                    2
            );
        }
    }
}
