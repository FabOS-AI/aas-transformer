package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobReader;
import de.fhg.ipa.aas_transformer.clients.redis.RedisTransformationJob;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.*;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;
import static de.fhg.ipa.aas_transformer.test.utils.RedisTestObjects.assertExpectedJobCount;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;

@ExtendWith(RedisExtension.class)
@ExtendWith(AasITExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SingleTransformationExecutorIT {

    // region Test vars
    // Service Ports:
    String aasRegistryPort = System.getProperty("aas.aas-registry.port");
    String aasRepositoryPort = System.getProperty("aas.aas-repository.port");
    String smRegistryPort = System.getProperty("aas.submodel-registry.port");
    String smRepositoryPort = System.getProperty("aas.submodel-repository.port");
    String redisPort = System.getProperty("spring.data.redis.port");

    // AAS Service Clients:
    AasRegistry aasRegistry;
    AasRepository aasRepository;
    SubmodelRegistry smRegistry;
    SubmodelRepository smRepository;

    // Test Transformer/AAS Objects:
    static boolean isInitialAasSetupDone = false;
    static AssetAdministrationShell testAas = getSimpleShell("", "");
    static Submodel timeseriesSubmodel = getRandomTimeseriesSubmodel(5, 50);
    static Submodel ansibleFactsSubmodel = getAnsibleFactsSubmodel();
    static List<Submodel> testSubmodels = List.of(timeseriesSubmodel, ansibleFactsSubmodel);
    static Transformer factsTransformerCopy = getAnsibleFactsTransformer();
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
            List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, "ansible_facts")))
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
            List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, "ansible_facts")))

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
                List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS,  new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort())))
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
                    List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort())))
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
                    List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort())))
            ),
            // DROP-EVERY:
            new Transformer(
                    UUID.randomUUID(),
                    new Destination(new DestinationSubmodel(
                            "{{ submodel:idShort(SOURCE_SUBMODEL) }}_drop_every",
                            "{{ submodel:id(SOURCE_SUBMODEL) }}_drop_every")
                    ),
                    List.of(new TransformerActionTsReduceDropEvery(5)),
                    List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.ID_SHORT, timeseriesSubmodel.getIdShort())))
            )
    );

    @Autowired
    RedisJobReader redisJobReader;

    RedisClient redisClient;
    // endregion

    @PostConstruct
    public void init() {
        // Setup Redis Client:
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", Integer.parseInt(redisPort));
        connectionFactory.start();
        redisClient = new RedisClient(connectionFactory);
    }

    // Mocks ManagementClient; Client return testTransformer
    @TestConfiguration
    public static class TestConfig {
        @MockBean
        ManagementClient managementClient;

        @PostConstruct
        public void initMock() {
            Mockito
                    .when(managementClient.getAllTransformer())
                    .thenReturn(Flux.fromStream(testTransformers.stream()));
            Mockito
                    .when(managementClient.getAllTransformerDTOListener())
                    .thenReturn(Flux.empty());
            Mockito
                    .when(managementClient.getTransformerChangeEventStream())
                    .thenReturn(Flux.empty());
            Mockito
                    .when(managementClient.getTransformerChangeEventDTOListenerStream())
                    .thenReturn(Flux.empty());
        }
    }

    public SingleTransformationExecutorIT() {
        this.aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        this.aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        this.smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        this.smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);
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
                EXECUTE,
                transformer.getId(),
                getTestSubmodel(transformer).getId(),
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

        redisClient.leftPushJob(new RedisTransformationJob(job));

        // Assert job count:
        assertExpectedJobCount(redisJobReader, 0);

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
