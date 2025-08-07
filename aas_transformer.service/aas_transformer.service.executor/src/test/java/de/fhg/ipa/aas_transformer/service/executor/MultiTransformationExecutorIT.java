package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.clients.job_api.JobApiClient;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultiTransformationExecutorIT extends AbstractIT {
    @MockBean
    MetricsClient metricsClient;
    @Autowired
    RedisJobProducer redisJobProducer;

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
    private int currentIndex = 0;
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

    @PostConstruct
    public void init() {
        // Log Transformation:
        Mockito
                .when(metricsClient.addTransformationLog(Mockito.any()))
                .thenReturn(Mono.just(new TransformationLog(
                        "destinationAasId",
                        "destinationSubmodelId",
                        "sourceSubmodelId",
                        null,
                        null,
                        null,
                        null
                )));
    }

    @Test
    @Order(10)
    public void testMultiTransformation() throws InterruptedException, DeserializationException, ApiException {
        // Register AAS objects from triples:
        registerAasObjectsFromTriples(aasRegistry, aasRepository, smRegistry, smRepository, triples);

//        Mockito
//                .when(jobApiClient.getNextJob())
//                .thenAnswer(invocation -> this.getNextJob());
        triples.forEach(triple -> {redisJobProducer.pushJob(
                new TransformationJob(
                        UUID.randomUUID(),
                        EXECUTE,
                        testTransformer.getId(),
                        ((Submodel) triple.get(1)).getId(),
                        null,
                        null
                )
        );});


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

        return;
    }

    private Mono<TransformationJob> getNextJob() {
        if(this.currentIndex >= triples.size())
            return Mono.empty();

        return Mono.just(
                new TransformationJob(
                        UUID.randomUUID(),
                        EXECUTE,
                        testTransformer.getId(),
                        ((Submodel)triples.get(this.currentIndex++).get(1)).getId(),
                        null,
                        null
                )
        );
    }
}
