package de.fhg.ipa.aas_transformer.service.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.redis.testcontainers.RedisContainer;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobReader;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import de.fhg.ipa.aas_transformer.service.listener.events.consumers.SubmodelElementMessageEventConsumer;
import de.fhg.ipa.aas_transformer.service.listener.events.consumers.SubmodelMessageEventConsumer;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.LangStringTextType;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultLangStringTextType;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultMultiLanguageProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.getAnsibleFactsSubmodel;
import static de.fhg.ipa.aas_transformer.test.utils.GenericTestConfig.getSimpleSubmodel;
import static de.fhg.ipa.aas_transformer.test.utils.RedisTestObjects.assertExpectedJobCount;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformerDTOListener;
import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AasITExtension.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class JobCreateTest {
    @Container
    @ServiceConnection(name = "redis")
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7"));

    static Submodel ansibleFactsSubmodel = getAnsibleFactsSubmodel();
    static TransformerDTOListener ansibleFactsTransformer = getAnsibleFactsTransformerDTOListener();

    @Autowired
    RedisJobReader redisJobReader;

    @TestConfiguration
    public static class TestConfig {
        @MockBean
        public static ManagementClient managementClient;

        @PostConstruct
        public void initMock(){
            Mockito
                    .when(managementClient.getAllTransformer())
                    .thenReturn(Flux.empty());
            Mockito
                    .when(managementClient.getAllTransformerDTOListener())
                    .thenReturn(Flux.just(ansibleFactsTransformer));
            Mockito
                    .when(managementClient.getTransformerChangeEventStream())
                    .thenReturn(Flux.empty());
            Mockito
                    .when(managementClient.getTransformerChangeEventDTOListenerStream())
                    .thenReturn(Flux.empty());
        }
    }

    @Autowired
    public SubmodelRepository submodelRepository;

    @Autowired
    RedisJobReader redisJobProducer;

    @Test
    @Order(10)
    public void testContextLoaded() {
        assertThat(submodelRepository).isNotNull();
        assertThat(redisJobProducer).isNotNull();
    }

    @Test
    @Order(15)
    public void testSubmodelRepoConnection() {
        assertTrue(submodelRepository.isSubmodelRepositoryAvailable());
    }

    @Test
    @Order(20)
    public void createSubmodelAndExpectJobCountInRedisOne() throws InterruptedException {
        // TODO add create of shell and registering submodel in shell
        submodelRepository.createOrUpdateSubmodel(ansibleFactsSubmodel);

        assertExpectedJobCount(redisJobReader, 1);
    }

    @Test
    @Order(30)
    public void modifySubmodelAndExpectJobCountInRedisPlusOne() throws InterruptedException {
        int expectedJobCount = redisJobProducer.getWaitingJobCount() + 1;

        //Change SubmodelELement:
        DefaultProperty newSubmodelElement = (DefaultProperty) ansibleFactsSubmodel.getSubmodelElements().get(0);
        String newPropertyValue = newSubmodelElement.getValue() + " modified";
        newSubmodelElement.setValue(newPropertyValue);

        submodelRepository.updateSubmodelElement(
                ansibleFactsSubmodel.getId(),
                ansibleFactsSubmodel.getSubmodelElements().get(0).getIdShort(),
                newSubmodelElement
        );

        assertExpectedJobCount(redisJobReader, expectedJobCount);
    }

    @Test
    @Order(40)
    public void deleteSourceSubmodelAndExpectJobCountInRedisPlusOne() throws FileNotFoundException, DeserializationException, InterruptedException {
        int expectedJobCount = redisJobProducer.getWaitingJobCount() + 1;
        Mockito
                .when(TestConfig.managementClient.getOrphanedDestinationSubmodels(
                        Mockito.any(UUID.class), Mockito.any(Submodel.class)
                ))
                .thenReturn(Mono.just(
                        List.of(getAnsibleFactsTransformer().getDestination().getSubmodelDestination().getId())
                ));

        submodelRepository.deleteSubmodel(ansibleFactsSubmodel.getId());

        assertExpectedJobCount(redisJobReader, expectedJobCount);
    }
}
