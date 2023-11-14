package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.Application;
import de.fhg.ipa.aas_transformer.service.TransformerRestController;
import de.fhg.ipa.aas_transformer.service.TransformerServiceFactory;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.actions.TransformerActionServiceFactory;
import de.fhg.ipa.aas_transformer.service.mqtt.MessageListener;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.basyx.aas.metamodel.api.IAssetAdministrationShell;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.api.submodelelement.ISubmodelElement;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.eclipse.basyx.vab.exception.provider.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(AasITExtension.class)
@SpringBootTest(
        classes = { Application.class, TransformerServiceFactory.class, TransformerActionServiceFactory.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { "spring.config.location=classpath:application-test.yml" })
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class AasTransformerIT {

    private final static Logger LOG = LoggerFactory.getLogger(AasTransformerIT.class);

    @Autowired
    private TransformerRestController transformerRestController;

    @Autowired
    private TransformerJpaRepository transformerJpaRepository;

    @Autowired
    private AASRegistry aasRegistry;

    @Autowired
    private AASManager aasManager;

    @LocalServerPort
    private int port;

    private Actions actions = new Actions();

    @BeforeEach
    public void beforeAll() {
        //RestAssured
        RestAssured.port = port;
        RestAssured.basePath = "/aas/transformer";
    }

    private class Actions {
        private Map<String, ISubmodelElement> getSubmodelElements(IIdentifier aasId, IIdentifier smId) {
            ISubmodel osSubmodel = aasManager.retrieveSubmodel(
                    aasId,
                    smId
            );

            return osSubmodel.getSubmodelElements();
        }

        private ISubmodelElement getSubmodelElement(IIdentifier aasId, IIdentifier smId, String smeKey) {
            ISubmodel osSubmodel = aasManager.retrieveSubmodel(
                    aasId,
                    smId
            );

            return osSubmodel.getSubmodelElement(smeKey);
        }

        public void cleanupTransformersAndAAS() {
            // Delete Transformer:
            transformerJpaRepository.deleteAll();

            // Delete all AAS
            var aasCollection = aasManager.retrieveAASAll();
            aasCollection
                    .stream()
                    .forEach(aas -> {
                        try {
                            aasManager.deleteAAS(GenericTestConfig.AAS.getIdentification());
                        }
                        catch (ResourceNotFoundException e) {

                        }
                    });
        }

        public void checkNoAASExists() {
            var aasCollection = aasManager.retrieveAASAll();
            assertThat(aasCollection).hasSize(0);
        }

        public void checkNoSubmodelExists() {
            var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
            assertThat(submodels).hasSize(0);
    }

        public void checkNoTransformerExists() {
            assertThat(transformerRestController.getTransformer()).hasSize(0);
        }

        public void checkDestinationSubmodelHasBeenCreatedByTransformer() throws InterruptedException, IOException {
            int maxTries = 10;
            int sleepTimeInMs = 1000;

            for(int tryNo = 0; tryNo < maxTries; tryNo++) {
                try {
                    var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());

                    assertThat(submodels).containsKeys(
                            GenericTestConfig.getAnsibleFactsSubmodel().getIdShort(),
                            GenericTestConfig.osTransformer.getDestination().getSmDestination().getIdShort());
                } catch(AssertionError e) {
                    Thread.sleep(sleepTimeInMs);
                }
            }

            var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());

            assertThat(submodels).containsKeys(
                    GenericTestConfig.getAnsibleFactsSubmodel().getIdShort(),
                    GenericTestConfig.osTransformer.getDestination().getSmDestination().getIdShort());
        }

        public void checkDestinationSubmodelHasBeenCreatedByTransformerAndContainsSubmodelElements() throws InterruptedException {
            int maxTries = 10;
            int sleepTimeInMs = 1000;

            var transformerCopyActions = (List<TransformerActionCopy>)(List<?>)GenericTestConfig.osTransformer.getTransformerActions();
            var expectedSubmodelElements = transformerCopyActions.stream()
                    .map(TransformerActionCopy::getSourceSubmodelElement)
                    .collect(Collectors.toList())
                    .toArray(new String[transformerCopyActions.size()]);

            for(int tryNo = 0; tryNo < maxTries; tryNo++) {
                var submodelElements = actions.getSubmodelElements(
                        GenericTestConfig.AAS.getIdentification(),
                        GenericTestConfig.osTransformer.getDestination().getSmDestination().getIdentifier()
                );
                try {
                    assertThat(submodelElements).containsKeys(expectedSubmodelElements);
                    return;
                } catch (AssertionError e) {
                    String msg = "Submodel Elements not there. Retry in " + (sleepTimeInMs/1000) + " seconds";
                    LOG.debug(msg);
                    Thread.sleep(sleepTimeInMs);
                }
            }

            Assertions.fail();
        }

        public void createAAS() {
            aasManager.createAAS(GenericTestConfig.AAS);
            var availableAASs = aasManager.retrieveAASAll();
            assertThat(availableAASs)
                    .extracting(IAssetAdministrationShell::getIdShort)
                    .containsExactlyInAnyOrder(GenericTestConfig.AAS.getIdShort());

            var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
            assertThat(submodels).hasSize(0);
        }

        public void createSubmodel() throws IOException {
            aasManager.createSubmodel(GenericTestConfig.AAS.getIdentification(), GenericTestConfig.getAnsibleFactsSubmodel());
            var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
            assertThat(submodels).containsKey(GenericTestConfig.getAnsibleFactsSubmodel().getIdShort());
        }

        public void createOSTransformer() {
            given()
                    .contentType(ContentType.JSON)
                    .body(GenericTestConfig.osTransformer)
                    .post();
            Optional<Transformer> optionalTransformer = transformerRestController.getTransformer(GenericTestConfig.osTransformer.getId());
            assertThat(optionalTransformer).isPresent();
        }

        public void updateOfSourceSubmodel() throws IOException, InterruptedException {
            String keyOfSubmodelElement = "distribution";
            String newValueOfSubmodelElement = "Windows";

            // Get ansible-facts submodel
            var sourceSubmodel = aasManager.retrieveSubmodel(
                    GenericTestConfig.AAS.getIdentification(),
                    GenericTestConfig.getAnsibleFactsIdentifier()
            );

            // Change value of SubmodelElement with key 'distribution'
            sourceSubmodel.getSubmodelElement(keyOfSubmodelElement).setValue(newValueOfSubmodelElement);

            int maxTries = 10;
            int sleepTimeInMs = 1000;

            for(int tryNo = 0; tryNo < maxTries; tryNo++) {
                // Get changed Submodel Element from Submodel operating-system:
                ISubmodelElement destinationSubmodelElement = actions.getSubmodelElement(
                        GenericTestConfig.AAS.getIdentification(),
                        GenericTestConfig.osTransformer.getDestination().getSmDestination().getIdentifier(),
                        keyOfSubmodelElement
                );
                try {
                    assertThat(newValueOfSubmodelElement).isEqualTo(destinationSubmodelElement.getValue());
                    return;
                } catch (AssertionError e) {
                    String msg = "Submodel Element '"+ keyOfSubmodelElement +"' has not the correct value. Retry in " +(sleepTimeInMs/1000)+ " seconds";
                    LOG.debug(msg);
                    Thread.sleep(sleepTimeInMs);
                }
            }

            Assertions.fail();
        }

        public void deleteSourceSubmodel() throws IOException, InterruptedException {
            try {
                aasManager.deleteSubmodel(
                        GenericTestConfig.AAS.getIdentification(),
                        GenericTestConfig.getAnsibleFactsIdentifier()
                );
            } catch(ResourceNotFoundException e) {}
            Thread.sleep(1000 * 5);

            int maxTries = 10;
            int sleepTimeInMs = 1000;

            for(int tryNo = 0; tryNo < maxTries; tryNo++) {
                try {
                    var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
                    assertThat(submodels).hasSize(0);
                    return;
                } catch (AssertionError e) {
                    String msg = "Submodel Count has not expected size of 0. Retry in " +(sleepTimeInMs/1000)+ " seconds";
                    System.out.println(msg);
                    Thread.sleep(sleepTimeInMs);
                }
            }

            Assertions.fail();
        }

        public void deleteOsTransformer() throws InterruptedException {
            given()
                    .contentType(ContentType.JSON)
                    .basePath("/aas/transformer/{transformerId}")
                    .pathParam("transformerId", GenericTestConfig.osTransformer.getId())
                    .queryParam("doCleanup", true)
                    .delete();

            int maxTries = 10;
            int sleepInMs = 1000;

            for(int x = 0; x < maxTries; x++) {
                try {
                    assertThat(transformerRestController.getTransformer()).hasSize(0);
                    return;
                } catch (AssertionError e) {
                    Thread.sleep(sleepInMs);
                }
            }

            Assertions.fail();
        }
    }




    @Nested
    @Order(10)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class preTest {
        @Test
        @Order(10)
        public void testComponentsNotNull() {
            assertThat(transformerRestController).isNotNull();
            assertThat(transformerJpaRepository).isNotNull();
            assertThat(aasManager).isNotNull();
            assertThat(aasRegistry).isNotNull();
        }
    }

    @Nested
    @Order(20)
    @TestClassOrder(ClassOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Create transformer and then AAS and submodel")
    public class createTransformerFirstThenAasAndSubmodel {
        @BeforeAll
        public void beforeAll() {
            actions.cleanupTransformersAndAAS();
        }

        @Nested
        @Order(10)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class createAasTransformer {
            @Test
            @Order(10)
            public void checkNoTransformerExists() {
                actions.checkNoTransformerExists();
            }

            @Test
            @Order(20)
            public void createOSTransformer() {
                actions.createOSTransformer();
            }
        }

        @Nested
        @Order(20)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class createAasAndSubmodel {
            @Test
            @Order(10)
            public void checkNoAASExists() {
                actions.checkNoAASExists();
            }

            @Test
            @Order(20)
            public void createAAS() {
                actions.createAAS();
            }

            @Test
            @Order(30)
            public void createSubmodel() throws IOException {
                actions.createSubmodel();
            }
        }

        @Nested
        @Order(30)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class submodelCreatedByTransformer {
            @Test
            @Order(10)
            public void checkDestinationSubmodelHasBeenCreatedByTransformer() throws InterruptedException, IOException {
                actions.checkDestinationSubmodelHasBeenCreatedByTransformer();
            }

            @Test
            @Order(30)
            public void checkDestinationSubmodelHasBeenCreatedByTransformerAndContainsSubmodelElements() throws InterruptedException {
                actions.checkDestinationSubmodelHasBeenCreatedByTransformerAndContainsSubmodelElements();
            }
        }

        @Nested
        @Order(40)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class submodelUpdateTriggersTransformer {

            @Test
            @Order(10)
            public void updateOfSourceSubmodel() throws IOException, InterruptedException {
                actions.updateOfSourceSubmodel();
            }
        }

        @Nested
        @Order(50)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class testDeleteOfSourceSubmodel {
            @Test
            @Order(10)
            public void testSubmodelCountExpectTwo() {
                var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
                assertThat(submodels).hasSize(2);
            }

            @Test
            @Order(20)
            public void deleteOfSourceSubmodel() throws IOException, InterruptedException {
                actions.deleteSourceSubmodel();
            }
        }
    }

    @Nested
    @Order(30)
    @TestClassOrder(ClassOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Create AAS and Submodel and then transformer")
    public class testCreateAasAndSubmodelFirstThenTransformer {
        @BeforeAll
        public void beforeAll() {
            actions.cleanupTransformersAndAAS();
        }

        @Nested
        @Order(10)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class createAasAndSubmodel {
            @Test
            @Order(10)
            public void checkNoAASExists() {
                actions.checkNoAASExists();
            }

            @Test
            @Order(20)
            public void createAAS() {
                actions.createAAS();
            }

            @Test
            @Order(30)
            public void createSubmodel() throws IOException {
                actions.createSubmodel();
            }
        }

        // Create Transformer and check submodel operating-system gets created
        @Nested
        @Order(20)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class createTransformer {
            @Test
            @Order(10)
            public void checkNoTransformerExists() {
                actions.checkNoTransformerExists();
            }

            @Test
            @Order(20)
            public void createOsTransformer() {
                actions.createOSTransformer();
            }

            @Test
            @Order(30)
            public void checkDestinationSubmodelHasBeenCreated() throws InterruptedException, IOException {
                actions.checkDestinationSubmodelHasBeenCreatedByTransformer();
            }

            @Test
            @Order(40)
            public void checkDestinationSubmodelHasBeenCreatedAndContainsSubmodelElements() throws InterruptedException {
                actions.checkDestinationSubmodelHasBeenCreatedByTransformerAndContainsSubmodelElements();
            }
        }

        // Delete Transformer and check associated submodel gets deleted too
        @Nested
        @Order(30)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class deleteTransformer {
            @Test
            @Order(10)
            public void deleteOsTransformer() throws InterruptedException {
                actions.deleteOsTransformer();
            }
        }
    }

    @Nested
    @Order(40)
    @TestClassOrder(ClassOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Create transformer, then submodel (without submodel elements) and then submodel elements")
    public class testCreateTransformerFirstThenAasAndSubmodelAndSubmodelElements {
        @BeforeAll
        public void beforeAll() {
            actions.cleanupTransformersAndAAS();
        }

        // create Transformer:
        @Nested
        @Order(10)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class createAasTransformer {
            @Test
            @Order(10)
            public void checkNoTransfomerExists() {
                actions.checkNoTransformerExists();
            }

            @Test
            @Order(20)
            public void createOsTransformer() {
                actions.createOSTransformer();
            }
        }

        // create AAS / Submodel:
        @Nested
        @Order(20)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class createAasAndSubmodelAndSubmodelElementsInvidiually {
            @Test
            @Order(10)
            public void checkNoAASExists() {
                actions.checkNoAASExists();
            }

            @Test
            @Order(20)
            public void createAAS() {
                actions.createAAS();
            }

            @Test
            @Order(40)
            public void checkNoSubmodelExists() {
                actions.checkNoSubmodelExists();
            }

            @Test
            @Order(50)
            public void createSubmodelWithoutSubmodelElements() throws IOException {
                aasManager.createSubmodel(GenericTestConfig.AAS.getIdentification(), GenericTestConfig.getAnsibleFactsSubmodelWithoutSubmodelElements());

                var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
                assertThat(submodels).containsKey(GenericTestConfig.getAnsibleFactsSubmodel().getIdShort());

                var ansibleFactsSubmodel = submodels.get(GenericTestConfig.getAnsibleFactsSubmodel().getIdShort());
                assertThat(ansibleFactsSubmodel.getSubmodelElements()).hasSize(0);
            }

            @Test
            @Order(80)
            public void addSubmodelElementsToSubmodel() throws IOException {
                var submodelElements = GenericTestConfig.getAnsibleFactsSubmodelElements();
                ISubmodel submodelBefore = aasManager.retrieveSubmodel(
                        GenericTestConfig.AAS.getIdentification(),
                        GenericTestConfig.getAnsibleFactsIdentifier()
                );

                for (SubmodelElement se : submodelElements) {
                    submodelBefore.addSubmodelElement(se);
                    LOG.debug("Added Submodel " + se.getIdShort() + "=" + se.get("value"));
                }

                var submodelAfter = aasManager.retrieveSubmodel(
                        GenericTestConfig.AAS.getIdentification(),
                        GenericTestConfig.getAnsibleFactsIdentifier()
                );

                assertThat(submodelAfter.getSubmodelElements()).hasSize(submodelElements.size());
            }
        }


        // check Transformer got triggered
        // check Transformer has created new submodel:
        @Nested
        @Order(30)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class testSubmodelCreatedByTransformer {
            @Test
            @Order(10)
            public void checkDestinationSubmodelHasBeenCreatedByTransformer() throws InterruptedException, IOException {
                actions.checkDestinationSubmodelHasBeenCreatedByTransformer();
            }

            @Test
            @Order(30)
            public void checkDestinationSubmodelHasBeenCreatedByTransformerAndContainsSubmodelElements() throws InterruptedException {
                actions.checkDestinationSubmodelHasBeenCreatedByTransformerAndContainsSubmodelElements();
            }

            @Nested
            @Order(40)
            @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
            public class testSubmodelUpdateTriggersTransformer {

                @Test
                @Order(10)
                public void updateOfSourceSubmodel() throws IOException, InterruptedException {
                    actions.updateOfSourceSubmodel();
                }
            }

            // delete Submodel / AAS
            // Check Transformer deletes associated submodel (if necessary at all)
            @Nested
            @Order(50)
            @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
            public class testDeleteOfSourceSubmodel {
                @Test
                @Order(10)
                public void testSubmodelCountExpectTwo() throws InterruptedException {
                    var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
                    assertThat(submodels).hasSize(2);
                }

                @Test
                @Order(20)
                public void deleteOfSourceSubmodel() throws IOException, InterruptedException {
                    actions.deleteSourceSubmodel();
                }

                @Test
                @Order(40)
                public void testReceivedObjectCacheIsEmpty() {
                    var receivedObjectCache = MessageListener.getMessageCache();
                    assertThat(receivedObjectCache).hasSize(0);
                }
            }
        }

        @Nested
        @Order(50)
        @TestClassOrder(ClassOrderer.OrderAnnotation.class)
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("Create transformer then AAS and submodel via server API")
        public class testCreateTransformerFirstThenAasAndSubmodelViaServerApi {
            @BeforeAll
            public void beforeAll() {
                actions.cleanupTransformersAndAAS();
            }

            // create Transformer:
            @Nested
            @Order(10)
            @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
            public class createAasTransformer {
                @Test
                @Order(10)
                public void testGetAllTransformerExpectNone() {
                    actions.checkNoTransformerExists();
                }

                @Test
                @Order(20)
                public void testCreateOsTransformer() {
                    actions.createOSTransformer();
                }
            }

            // create AAS / Submodel:
            @Nested
            @Order(20)
            @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
            public class createAasAndSubmodel {
                @Test
                @Order(10)
                public void checkNoAASExists() {
                    actions.checkNoAASExists();
                }

                @Test
                @Order(20)
                public void createAAS() {
                    actions.createAAS();
                }

                @Test
                @Order(30)
                public void createSubmodel() throws IOException {
                    String aasId = GenericTestConfig.AAS.getIdentification().getId();
                    String smIdShort = GenericTestConfig.getAnsibleFactsSubmodelWithoutSubmodelElements().getIdShort();

                    try (BufferedReader br = new BufferedReader(new FileReader(GenericTestConfig.ansibleFactsSubmodelAndSubmodelElementFile))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.out.println(line);
                        }
                    }

                    given()
                            .contentType(ContentType.JSON)
                            .body(GenericTestConfig.ansibleFactsSubmodelAndSubmodelElementFile)
                            .log()
                            .all()
                            .put(aasManager.getAasServerUrl() + "/shells/" + aasId + "/aas/submodels/" + smIdShort)
                            .then()
                            .log()
                            .all();

                    var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
                    assertThat(submodels).containsKey(GenericTestConfig.getAnsibleFactsSubmodel().getIdShort());
                }
            }

            // check Transformer got triggered
            // check Transformer has created new submodel:
            @Nested
            @Order(30)
            @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
            public class testSubmodelCreatedByTransformer {
                @Test
                @Order(10)
                public void checkDestinationSubmodelHasBeenCreatedByTransformer() throws InterruptedException, IOException {
                    actions.checkDestinationSubmodelHasBeenCreatedByTransformer();
                }

                @Test
                @Order(20)
                public void checkDestinationSubmodelHasBeenCreatedByTransformerAndContainsSubmodelElements() throws InterruptedException {
                    actions.checkDestinationSubmodelHasBeenCreatedByTransformerAndContainsSubmodelElements();
                }
            }

            @Nested
            @Order(40)
            @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
            public class testSubmodelUpdateTriggersTransformer {
                String keyOfSubmodelElement = "distribution";
                String newValueOfSubmodelElement = "Windows";

                @Test
                @Order(10)
                public void testUpdateOfSourceSubmodel() throws IOException, InterruptedException {
                    actions.updateOfSourceSubmodel();
                }
            }

            // delete Submodel / AAS
            // Check Transformer deletes associated submodel (if necessary at all)
            @Nested
            @Order(50)
            @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
            public class testDeleteOfSourceSubmodel {
                @Test
                @Order(10)
                public void testSubmodelCountExpectTwo() throws InterruptedException {
                    var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
                    assertThat(submodels).hasSize(2);
                }

                @Test
                @Order(20)
                public void deleteOfSourceSubmodel() throws IOException, InterruptedException {
                    actions.deleteSourceSubmodel();
                }
            }
        }
    }
}
