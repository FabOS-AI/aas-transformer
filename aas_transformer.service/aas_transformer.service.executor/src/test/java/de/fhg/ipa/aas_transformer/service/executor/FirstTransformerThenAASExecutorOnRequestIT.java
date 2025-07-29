package de.fhg.ipa.aas_transformer.service.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.fhg.ipa.aas_transformer.aas.deserializer.SubmodelDeserializer;
import de.fhg.ipa.aas_transformer.aas.serializer.SubmodelSerializer;
import de.fhg.ipa.aas_transformer.clients.job_api.JobApiClient;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.persistence.api.TransformationDescriptionJpaRepository;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import de.fhg.ipa.aas_transformer.transformation.TransformationExecutionService;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.SubmodelDescriptor;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.eclipse.digitaltwin.basyx.submodelrepository.http.pagination.GetSubmodelsResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Base64;
import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FirstTransformerThenAASExecutorOnRequestIT extends AbstractIT {
    @LocalServerPort
    private int transformerExecutorPort;
    @MockBean
    JobApiClient jobApiClient;
    private WebClient webclient;
    private ConnectedSubmodelRepository submodelRepository;
    // Test Objects:
    static Transformer factsTransformer = getAnsibleFactsTransformer(true, true);
    static DefaultAssetAdministrationShell shell = getSimpleShell("", "");
    static Submodel factsSubmodel = getAnsibleFactsSubmodel();
    static SubmodelDescriptor operatingSystemSubmodelDescriptor;

    // Transformer Objects
    @Autowired
    private TransformationDescriptionJpaRepository transformationDescriptionJpaRepository;
    @MockBean
    MetricsClient metricsClient;

    // Mocks ManagementClient; Client return factsTransformer
    @TestConfiguration
    public static class TestConfig {
        @MockBean
        ManagementClient managementClient;

        @PostConstruct
        public void initMock(){
            Sinks.Many<TransformerChangeEvent> changeEventSink =
                    Sinks.many().unicast().onBackpressureBuffer();
            Sinks.Many<TransformerChangeEventDTOListener> changeEventDtoListenerSink =
                    Sinks.many().unicast().onBackpressureBuffer();
            Mockito
                    .when(managementClient.getAllTransformer())
                    .thenReturn(Flux.just(factsTransformer));
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

    @BeforeEach
    void setUp() throws ApiException {
         webclient = getExecutorWebclient("http://localhost:"+transformerExecutorPort);

         // Create Shell and facts submodel:
        this.aasRepository.createOrUpdateAas(shell);
        this.aasRepository.addSubmodelReferenceToAas(shell.getId(), factsSubmodel);
        this.smRepository.createOrUpdateSubmodel(factsSubmodel);
        this.aasRegistry.addSubmodelDescriptorToAas(
                shell.getId(),
                this.smRegistry.findSubmodelDescriptor(factsSubmodel.getId()).get()
        );
    }

    private WebClient getExecutorWebclient(String baseUrl) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Submodel.class, new SubmodelSerializer());
        simpleModule.addDeserializer(Submodel.class, new SubmodelDeserializer());
        objectMapper.registerModule(simpleModule);

        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                }).build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(strategies)
                .build();
    }

    @Test
    @Order(10)
    public void testCountOfExecutionServicesExpectOne() {
        List<TransformationExecutionService> services =
                transformationExecutionServiceCache.transformationExecutionServices;

        assertEquals(1, services.size());
    }

    @Test
    @Order(20)
    public void testCreateTransformationJobExpectOneTransformationDescription() throws DeserializationException, SerializationException, InterruptedException, ApiException {
        Mockito
                .when(metricsClient.addTransformationLog(Mockito.any()))
                .thenReturn(Mono.empty());

        TransformationJob createdJob = new TransformationJob(
                TransformationJobAction.EXECUTE,
                factsTransformer.getId(),
                factsSubmodel.getId(),
                null,
                null
        );

        assertEquals(1, aasRepository.getAas(shell.getId()).getSubmodels().size());
        assertExpectedSubmodelCount(aasRegistry, aasRepository, smRepository, shell.getId(), 1, 1);

        Mockito
                .when(jobApiClient.getNextJob())
                .thenReturn(Mono.just(createdJob))
                .thenReturn(Mono.empty());
//        redisJobClient.rightPushJob(new RedisTransformationJob(createdJob));

        waitForTransformationDescriptionCount(1);

        List<TransformationDescription> result = transformationDescriptionJpaRepository.findAll().collectList().block();
        List<SubmodelDescriptor> smDescriptors = smRegistry.getSubmodelDescriptors();

        assertTrue(result.size() == 1);
        assertTrue(smDescriptors.size() == 2);

        operatingSystemSubmodelDescriptor = smDescriptors.stream()
                .filter(descriptor -> descriptor.getId().contains("operating_system"))
                .findFirst()
                .get();
    }

    @Test
    @Order(30)
    public void testRequestOneSubmodelExpectDestinationSubmodel() {
        Mockito
                .when(metricsClient.addTransformationLog(Mockito.any()))
                .thenReturn(Mono.empty());

        WebClient.ResponseSpec responseString = webclient
                .get() // Adjust the base URL as needed
                .uri("/submodels/" + b64Encode(operatingSystemSubmodelDescriptor.getId()))
                .retrieve();

        Submodel response = webclient
                .get() // Adjust the base URL as needed
                .uri("/submodels/" + b64Encode(operatingSystemSubmodelDescriptor.getId()))
                .retrieve()
                .bodyToMono(Submodel.class)
                .block();

        assertNotNull(response);
    }

    @Test
    @Order(40)
    public void testRequestAllSubmodelExpectDestinationSubmodel() {
        Mockito
                .when(metricsClient.addTransformationLog(Mockito.any()))
                .thenReturn(Mono.empty());

        GetSubmodelsResult response = webclient
                .get()
                .uri("/submodels")
                .retrieve()
                .bodyToMono(GetSubmodelsResult.class)
                .block();

        assertNotNull(response);
    }

    @Test
    @Order(50)
    public void testSendDeleteTransformationJobExpectNoDestinationSubmodel() throws InterruptedException {
        TransformationJob deleteJob = new TransformationJob(
                TransformationJobAction.DELETE,
                factsTransformer.getId(),
                factsSubmodel.getId(),
                null,
                null
        );

        Mockito
                .when(jobApiClient.getNextJob())
                .thenReturn(Mono.just(deleteJob))
                .thenReturn(Mono.empty());
//        redisJobClient.rightPushJob(new RedisTransformationJob(deleteJob));

        waitForTransformationDescriptionCount(0);

        String response = webclient
                .get()
                .uri("/submodels/" + b64Encode(operatingSystemSubmodelDescriptor.getId()))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertNull(response);
    }

    private void waitForTransformationDescriptionCount(int desiredCount) throws InterruptedException {
        List<TransformationDescription> result = transformationDescriptionJpaRepository.findAll().collectList().block();
        int tryCount = 0;
        int maxTries = 20;
        while(!(result.size() == desiredCount)) {
            tryCount++;
            if(tryCount > maxTries) break;
            sleep(500);
            result = transformationDescriptionJpaRepository.findAll().collectList().block();
        }
    }

    private String b64Encode(String str) {
        return Base64.getEncoder().encodeToString(str.getBytes());
    }

//    @Test
//    @Order(30)
//    public void testDeleteTransformationJobExpectOneSubmodelAndNoJobs() throws SerializationException, InterruptedException, DeserializationException, ApiException {
//        String destinationSubmodelId = getDestinationSubmodelIdOfAnsibleFactsTransformer(
//                factsTransformer,
//                shell.getId()
//        );
//
//        TransformationJob deletedJob = new TransformationJob(
//            TransformationJobAction.DELETE,
//            null,
//                destinationSubmodelId,
//            null
//        );
//
//        redisClient.rightPushJob(new RedisTransformationJob(deletedJob));
//
//        int expectedSubmodelCount = 1;
//
//        assertExpectedJobCount(redisJobReader,0);
//        assertExpectedSubmodelCount(aasRegistry, aasRepository, smRepository, shell.getId(), expectedSubmodelCount, expectedSubmodelCount);
//    }
}
