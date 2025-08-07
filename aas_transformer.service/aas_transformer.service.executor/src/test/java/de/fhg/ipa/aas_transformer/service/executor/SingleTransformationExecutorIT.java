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
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SingleTransformationExecutorIT extends AbstractIT {
    @MockBean
    MetricsClient metricsClient;
//    @MockBean
//    JobApiClient jobApiClient;
    @Autowired
    RedisJobProducer redisJobProducer;

    // region Test vars
    // Test Transformer/AAS Objects:
    static boolean isInitialAasSetupDone = false;
    static AssetAdministrationShell testAas = getSimpleShell("", "");
    static Submodel timeseriesSubmodel = getRandomTimeseriesSubmodel(5, 50);
    static Submodel ansibleFactsSubmodel = getAnsibleFactsSubmodel();
    static List<Submodel> testSubmodels = List.of(timeseriesSubmodel, ansibleFactsSubmodel);
    static Transformer factsTransformerCopy = getAnsibleFactsTransformer(false);
    static Transformer factsTransformerSubmodelTemplate =  new Transformer(
            UUID.randomUUID(),
            new Destination(new DestinationSubmodel(
                    "operating_system_sm_template",
                    "{{destinationShells:shell_id(DESTINATION_SHELLS, 0)}}/operating_system_sm_template"
            )),
            List.of(new TransformerActionSubmodelTemplate(
                            "{  \"modelType\": \"Submodel\",  \"kind\": \"Instance\",  \"id\": \"{{destinationShells:shell_id(DESTINATION_SHELLS, 0)}}/operating_system_sm_template\",  \"idShort\": \"operating_system_sm_template\",  \"submodelElements\": [    {      \"modelType\": \"Property\",      \"value\": \"{{ submodel:sme_value(SOURCE_SUBMODEL, 'distribution') }}\",      \"valueType\": \"xs:string\",      \"idShort\": \"distribution_new\"    },    {      \"modelType\": \"Property\",      \"value\": \"{{ submodel:sme_value(SOURCE_SUBMODEL, 'distribution_release') }}\",      \"valueType\": \"xs:string\",      \"idShort\": \"distribution_release_new\"    }  ]}"
                    )
            ),
            List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, "ansible_facts"))),
            false
    );
    static Transformer factsTransformerSubmodelElementTemplate = new Transformer(
            UUID.randomUUID(),
            new Destination(new DestinationSubmodel(
                    "operating_system_sme_template",
                    "{{destinationShells:shell_id(DESTINATION_SHELLS, 0)}}/operating_system_sme_template"
            )),
            List.of(new TransformerActionSubmodelElementTemplate(
                    "distribution_new",
                    "{  \"modelType\": \"Property\",  \"value\": \"{{ submodel:sme_value(SOURCE_SUBMODEL, 'distribution') }}\",  \"valueType\": \"xs:string\",  \"idShort\": \"distribution_new\"}"
                    )
            ),
            List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, "ansible_facts"))),
            false

    );
    static List<Transformer> testTransformers = List.of(
            // COPY:
            factsTransformerCopy,
            // SUBMODEL_TEMPLATE:
            factsTransformerSubmodelTemplate,
            // SUBMODEL_ELEMENT_TEMPLATE:
            factsTransformerSubmodelElementTemplate,
            // TIMESERIES:
            // AVG:
            new Transformer(
                UUID.randomUUID(),
                new Destination(new DestinationSubmodel(
                        "{{ submodel:idShort(SOURCE_SUBMODEL) }}_avg",
                        "{{ submodel:id(SOURCE_SUBMODEL) }}_avg")
                ),
                List.of(new TransformerActionTsAvg(
                        List.of("sensor0", "sensor1"),
                        5)
                ),
                List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS,  new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort()))),
                false
            ),
            // MDN:
            new Transformer(
                    UUID.randomUUID(),
                    new Destination(new DestinationSubmodel(
                            "{{ submodel:idShort(SOURCE_SUBMODEL) }}_mdn",
                            "{{ submodel:id(SOURCE_SUBMODEL) }}_mdn")
                    ),
                    List.of(new TransformerActionTsMdn(
                            List.of("sensor0", "sensor1"),
                            5
                            )
                    ),
                    List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort()))),
                    false
            ),
            // TAKE-EVERY:
            new Transformer(
                    UUID.randomUUID(),
                    new Destination(new DestinationSubmodel(
                            "{{ submodel:idShort(SOURCE_SUBMODEL) }}_take_every",
                            "{{ submodel:id(SOURCE_SUBMODEL) }}_take_every")
                    ),
                    List.of(new TransformerActionTsReduceTakeEvery(5)
                    ),
                    List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort()))),
                    false
            ),
            // DROP-EVERY:
            new Transformer(
                    UUID.randomUUID(),
                    new Destination(new DestinationSubmodel(
                            "{{ submodel:idShort(SOURCE_SUBMODEL) }}_drop_every",
                            "{{ submodel:id(SOURCE_SUBMODEL) }}_drop_every")
                    ),
                    List.of(new TransformerActionTsReduceDropEvery(5)),
                    List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort()))),
                    false
            )
    );
    //endregion

    // Mocks ManagementClient; Client return testTransformer
    @TestConfiguration
    public static class TestConfig {
        @MockBean
        ManagementClient managementClient;
        Sinks.Many<TransformerDTOListener> sinkTransformerDtoListener = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<TransformerChangeEvent> sinkChangeEvent = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<TransformerChangeEventDTOListener> sinkChangeEventDtoListener = Sinks.many().unicast().onBackpressureBuffer();

        @PostConstruct
        public void initMock() {
            // Get All Endpoints:
            Mockito
                    .when(managementClient.getAllTransformer())
                    .thenReturn(Flux.fromStream(testTransformers.stream()));
            Mockito
                    .when(managementClient.getAllTransformerDTOListener())
                    .thenReturn(sinkTransformerDtoListener.asFlux());

            // Get ChangeEvent Streams:
            Mockito
                    .when(managementClient.getTransformerChangeEventStream())
                    .thenReturn(sinkChangeEvent.asFlux());
            Mockito
                    .when(managementClient.getTransformerChangeEventDTOListenerStream())
                    .thenReturn(sinkChangeEventDtoListener.asFlux());
        }
    }

    @PostConstruct
    public void init() {
//        Mockito
//                .when(jobApiClient.getNextJob())
//                .thenReturn(Mono.empty());

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

    private static Stream<Arguments> getTestTransformers() {

        return testTransformers
                .stream()
                .map(transformer -> Arguments.of(Named.of(
                        transformer.getTransformerActions().get(0).getActionType().toString(),
                        transformer))
                );
    }

    private static Submodel getTestSubmodel(Transformer transformer) {
        if(
                transformer.equals(factsTransformerCopy) ||
                transformer.equals(factsTransformerSubmodelTemplate) ||
                transformer.equals(factsTransformerSubmodelElementTemplate)
        ) {
            return ansibleFactsSubmodel;
        } else {
            return timeseriesSubmodel;
        }
    }

    @ParameterizedTest
    @MethodSource("getTestTransformers")
    @Order(10)
    public void testExecute(Transformer transformer) throws InterruptedException, DeserializationException, ApiException {
        if(!isInitialAasSetupDone) {
            for(Submodel submodel : testSubmodels) {
                registerShellAndSubmodel(
                        this.aasRegistry,
                        this.aasRepository,
                        this.smRegistry,
                        this.smRepository,
                        testAas,
                        submodel
                );
            }
            isInitialAasSetupDone = true;
        }
        TransformationJob job = new TransformationJob(
                UUID.randomUUID(),
                EXECUTE,
                transformer.getId(),
                getTestSubmodel(transformer).getId(),
                null,
                null
        );

        // Assert Submodel count:
        int expectedCount = 2+testTransformers.indexOf(transformer);
        assertExpectedSubmodelCount(
                aasRegistry,
                aasRepository,
                smRepository,
                testAas.getId(),
                expectedCount,
                expectedCount
        );

//        Mockito
//                .when(jobApiClient.getNextJob())
//                .thenReturn(Mono.just(job))
//                .thenReturn(Mono.empty());
        redisJobProducer.pushJob(job);
//        redisJobClient.rightPushJob(new RedisTransformationJob(job));
//
//        // Assert job count:
//        assertExpectedJobCount(redisJobConsumer, 0);

        // Assert Submodel count:
        expectedCount++;
        assertExpectedSubmodelCount(
                aasRegistry,
                aasRepository,
                smRepository,
                testAas.getId(),
                expectedCount,
                expectedCount
        );
    }
}
