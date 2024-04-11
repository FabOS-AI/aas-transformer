package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.Application;
import de.fhg.ipa.aas_transformer.service.TransformerRestController;
import de.fhg.ipa.aas_transformer.service.TransformerServiceFactory;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.actions.TransformerActionServiceFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.objectMapper;
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
    private SubmodelRegistry submodelRegistry;

    @Autowired
    private SubmodelRepository submodelRepository;

    @LocalServerPort
    private int port;

    private Actions actions = new Actions();

    private Transformer osTransformer;

    private Submodel ansibleFactsSubmodel;

    @BeforeEach
    public void beforeAll() throws IOException, DeserializationException {
        //RestAssured
        RestAssured.port = port;
        RestAssured.basePath = "/aas/transformer";

        var ansibleFactsSubmodelFile = new File("src/test/resources/submodels/ansible-facts-submodel.json");
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        this.ansibleFactsSubmodel = jsonDeserializer.read(new FileInputStream(ansibleFactsSubmodelFile), Submodel.class);

        var transformerFile = new File("src/test/resources/transformer/operating_system.json");
        var transformerJson = new String(Files.readAllBytes(transformerFile.toPath()));
        this.osTransformer = objectMapper.readValue(transformerFile, Transformer.class);
    }

    private class Actions {
        private List<SubmodelElement> getSubmodelElements(String smId) {
            var submodel = submodelRepository.getSubmodel(smId);

            return submodel.getSubmodelElements();
        }

        private SubmodelElement getSubmodelElement(String smId, String smeKey) {
            var submodelElement = submodelRepository.getSubmodelElement(smId, smeKey);

            return submodelElement;
        }

        public void cleanupTransformersAndSubmodels() throws DeserializationException {
            // Delete Transformer:
            transformerJpaRepository.deleteAll();

            // Delete all submodels
            var submodels = submodelRepository.getAllSubmodels();
            for (var submodel : submodels) {
                submodelRepository.deleteSubmodel(submodel.getId());
            }
        }

        public void checkNoAASExists() {
//            var aasCollection = aasManager.retrieveAASAll();
//            assertThat(aasCollection).hasSize(0);
        }

        public void checkNoSubmodelExists() throws DeserializationException {
            var submodels = submodelRepository.getAllSubmodels();
            assertThat(submodels).hasSize(0);
    }

        public void checkNoTransformerExists() {
            assertThat(transformerRestController.getTransformer()).hasSize(0);
        }

        public void checkDestinationSubmodelHasBeenCreatedByTransformer() throws InterruptedException, DeserializationException {
            int maxTries = 10;
            int sleepTimeInMs = 1000;

            for(int tryNo = 0; tryNo < maxTries; tryNo++) {
                try {
                    var submodels = submodelRepository.getAllSubmodels();

                    assertThat(submodels).extracting(Submodel::getId).contains(
                            ansibleFactsSubmodel.getIdShort(),
                            osTransformer.getDestination().getSubmodelDestination().getIdShort());
                } catch(AssertionError e) {
                    Thread.sleep(sleepTimeInMs);
                }
            }

            var submodels = submodelRepository.getAllSubmodels();

            assertThat(submodels).extracting(Submodel::getId).contains(
                    ansibleFactsSubmodel.getIdShort(),
                    osTransformer.getDestination().getSubmodelDestination().getIdShort());
        }

        public void checkDestinationSubmodelHasBeenCreatedByTransformerAndContainsSubmodelElements() throws InterruptedException {
            int maxTries = 10;
            int sleepTimeInMs = 1000;

            var transformerCopyActions = (List<TransformerActionCopy>)(List<?>)osTransformer.getTransformerActions();
            var expectedSubmodelElements = transformerCopyActions.stream()
                    .map(TransformerActionCopy::getSourceSubmodelElement)
                    .collect(Collectors.toList())
                    .toArray(new String[transformerCopyActions.size()]);

            for(int tryNo = 0; tryNo < maxTries; tryNo++) {
                var submodelElements = actions.getSubmodelElements(osTransformer.getDestination().getSubmodelDestination().getId());
                try {
                    assertThat(submodelElements)
                            .extracting(SubmodelElement::getIdShort)
                            .contains(expectedSubmodelElements);
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
//            aasManager.createAAS(GenericTestConfig.AAS);
//            var availableAASs = aasManager.retrieveAASAll();
//            assertThat(availableAASs)
//                    .extracting(IAssetAdministrationShell::getIdShort)
//                    .containsExactlyInAnyOrder(GenericTestConfig.AAS.getIdShort());
//
//            var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
//            assertThat(submodels).hasSize(0);
        }

        public void createAnsibleFactsSubmodelSubmodel() throws DeserializationException {
            submodelRepository.createOrUpdateSubmodel(ansibleFactsSubmodel);
            var submodels = submodelRepository.getAllSubmodels();
            assertThat(submodels).extracting(Submodel::getIdShort).contains(ansibleFactsSubmodel.getIdShort());
        }

        public void createOSTransformer() throws IOException {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("execute", true)
                    .body(osTransformer)
                    .post();
//            Optional<Transformer> optionalTransformer = transformerRestController.getTransformer(osTransformer.getId());
//            assertThat(optionalTransformer).isPresent();
        }

        public void updateOfSourceSubmodel() throws IOException, InterruptedException {
            String keyOfSubmodelElement = "distribution";
            String newValueOfSubmodelElement = "Windows";

            // Get ansible-facts submodel
            var sourceSubmodel = submodelRepository.getSubmodel(ansibleFactsSubmodel.getId());
            var newSubmodelElements = new ArrayList<>(sourceSubmodel.getSubmodelElements());
            // Change value of SubmodelElement with key 'distribution'
            var submodelElement = (Property)sourceSubmodel.getSubmodelElements().stream()
                    .filter(sme -> sme.getIdShort().equals(keyOfSubmodelElement)).findAny().get();
            newSubmodelElements.remove(submodelElement);
            submodelElement.setValue(newValueOfSubmodelElement);
            newSubmodelElements.add(submodelElement);
            sourceSubmodel.setSubmodelElements(newSubmodelElements);
            submodelRepository.createOrUpdateSubmodel(sourceSubmodel);

            int maxTries = 10;
            int sleepTimeInMs = 1000;

            for(int tryNo = 0; tryNo < maxTries; tryNo++) {
                try {
                    // Get changed Submodel Element from Submodel operating-system:
                    var destinationSubmodelElement = (Property)actions.getSubmodelElement(
                            osTransformer.getDestination().getSubmodelDestination().getId(),
                            keyOfSubmodelElement
                    );
                    assertThat(newValueOfSubmodelElement).isEqualTo(destinationSubmodelElement.getValue());
                    return;
                } catch (ElementDoesNotExistException | AssertionError e) {
                    String msg = "Submodel Element '"+ keyOfSubmodelElement +"' has not the correct value or was not found. Retry in " +(sleepTimeInMs/1000)+ " seconds";
                    LOG.debug(msg);
                    Thread.sleep(sleepTimeInMs);
                }
            }

            Assertions.fail();
        }

        public void deleteSourceSubmodel() throws IOException, InterruptedException {
            submodelRepository.deleteSubmodel(ansibleFactsSubmodel.getId());

            int maxTries = 10;
            int sleepTimeInMs = 1000;

            for(int tryNo = 0; tryNo < maxTries; tryNo++) {
                try {
                    var submodels = submodelRepository.getAllSubmodels();
                    assertThat(submodels).extracting(Submodel::getId)
                            .doesNotContain(
                                    ansibleFactsSubmodel.getId(),
                                    osTransformer.getDestination().getSubmodelDestination().getIdShort());
                    return;
                } catch (AssertionError e) {
                    System.out.println(e.getMessage());
                    Thread.sleep(sleepTimeInMs);
                } catch (DeserializationException e) {
                    throw new RuntimeException(e);
                }
            }

            Assertions.fail();
        }

        public void deleteOsTransformer() throws InterruptedException {
            given()
                    .contentType(ContentType.JSON)
                    .basePath("/aas/transformer/{transformerId}")
                    .pathParam("transformerId", osTransformer.getId())
                    .queryParam("doCleanup", true)
                    .delete();

            int maxTries = 10;
            int sleepInMs = 1000;

            for(int x = 0; x < maxTries; x++) {
                try {
                    var transformers = transformerRestController.getTransformer();
                    assertThat(transformers).hasSize(0);
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
            assertThat(submodelRegistry).isNotNull();
            assertThat(submodelRepository).isNotNull();
        }
    }

    @Nested
    @Order(20)
    @TestClassOrder(ClassOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Create transformer and then AAS and submodel")
    public class createTransformerFirstThenAasAndSubmodel {
        @BeforeAll
        public void beforeAll() throws DeserializationException {
            actions.cleanupTransformersAndSubmodels();
        }

        @Nested
        @Order(10)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class createTransformer {
            @Test
            @Order(10)
            public void checkNoTransformerExists() {
                actions.checkNoTransformerExists();
            }

            @Test
            @Order(20)
            public void createOSTransformer() throws IOException {
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
            public void createSubmodel() throws DeserializationException {
                actions.createAnsibleFactsSubmodelSubmodel();
            }
        }

        @Nested
        @Order(30)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        public class submodelCreatedByTransformer {
            @Test
            @Order(10)
            public void checkDestinationSubmodelHasBeenCreatedByTransformer() throws InterruptedException, DeserializationException {
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
            public void testSubmodelCountExpectTwo() throws DeserializationException {
                var submodels = submodelRepository.getAllSubmodels();
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
        public void beforeAll() throws DeserializationException {
            actions.cleanupTransformersAndSubmodels();
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
            public void createSubmodel() throws IOException, DeserializationException {
                actions.createAnsibleFactsSubmodelSubmodel();
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
            public void createOsTransformer() throws IOException {
                actions.createOSTransformer();
            }

            @Test
            @Order(30)
            public void checkDestinationSubmodelHasBeenCreated() throws InterruptedException, DeserializationException {
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
            public void deleteOsTransformers() throws InterruptedException {
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
        public void beforeAll() throws DeserializationException {
            actions.cleanupTransformersAndSubmodels();
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
            public void createOsTransformer() throws IOException {
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
            public void checkNoSubmodelExists() throws DeserializationException {
                actions.checkNoSubmodelExists();
            }

            @Test
            @Order(50)
            public void createSubmodelWithoutSubmodelElements() throws DeserializationException {
                var submodelWithoutSubmodelElements = ansibleFactsSubmodel;
                ansibleFactsSubmodel.setSubmodelElements(new ArrayList<SubmodelElement>());
                submodelRepository.createOrUpdateSubmodel(ansibleFactsSubmodel);

                var submodel = submodelRepository.getSubmodel(ansibleFactsSubmodel.getIdShort());
                assertThat(submodel).isNotNull();
                assertThat(ansibleFactsSubmodel.getSubmodelElements()).hasSize(0);
            }

            @Test
            @Order(80)
            public void addSubmodelElementsToSubmodel() throws IOException {
                var submodelElements = ansibleFactsSubmodel.getSubmodelElements();
                var submodelBefore = submodelRepository.getSubmodel(ansibleFactsSubmodel.getId());

                for (var submodelElement : submodelElements) {
                    submodelRepository.createSubmodelElement(ansibleFactsSubmodel.getId(), submodelElement);
                    LOG.debug("Added Submodel " + submodelElement.getIdShort() + "=" + ((Property)submodelElement).getValue());
                }

                var submodelAfter = submodelRepository.getSubmodel(ansibleFactsSubmodel.getId());

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
            public void checkDestinationSubmodelHasBeenCreatedByTransformer() throws InterruptedException, IOException, DeserializationException {
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
                public void testSubmodelCountExpectTwo() throws DeserializationException {
                    var submodels = submodelRepository.getAllSubmodels();
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
