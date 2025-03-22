package de.fhg.ipa.aas_transformer.test.system;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.basyx.pagination.GetSubmodelElementsResult;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.GetSubmodelDescriptorsResult;
import org.eclipse.digitaltwin.basyx.submodelrepository.http.pagination.GetSubmodelsResult;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;

import java.util.*;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.getRandomAnsibleFactsTriples;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.registerAasObjectsFromTriples;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExtTransformOnRequestTest extends AbstractExtSystemTest {

    // Create Transformer with transformOnRequest = true
    static List<Object> ansibleFactsTriple = getRandomAnsibleFactsTriples(1, true).get(0);
    static DefaultAssetAdministrationShell sourceShell = (DefaultAssetAdministrationShell) ansibleFactsTriple.get(0);
    static Submodel sourceSubmodel = (Submodel) ansibleFactsTriple.get(1);
    static List<SubmodelElement> sourceSubmodelElements = sourceSubmodel.getSubmodelElements();
    static Transformer transformer = (Transformer) ansibleFactsTriple.get(2);
    static String destinationSubmodelId = transformer.getDestination().getSubmodelDestination().getId();
    static Submodel updatedSourceSubmodel = (Submodel)getRandomAnsibleFactsTriples(1, true).get(0).get(1);
    static List<SubmodelElement> updatedSourceSubmodelElements = updatedSourceSubmodel.getSubmodelElements();
    static Map<String, String> submodelElementMap = Map.of(
            "distribution", "distribution_new",
            "distribution_release", "distribution_release_new"
    );
    static {
        transformer.setTransformOnRequest(true);
        updatedSourceSubmodel.setId(sourceSubmodel.getId());
        updatedSourceSubmodel.setIdShort(sourceSubmodel.getIdShort());
    }


    @BeforeAll
    void beforeAll() {
        deleteAllTransformer();
        createTestTransformer();
    }

    @AfterAll
    void afterAll() throws DeserializationException {
        deleteAllAasObjects();
        deleteAllTransformer();
    }

    @Test
    @Order(10)
    public void testCreateSourceExpectTwoSubmodelDescriptorsAndDestinationSubmodelNotNull() throws InterruptedException {
        List<Transformer> transformerList = this.managementClient.getAllTransformer().collectList().block();
        assertTrue(transformerList.size() == 1);
        assertTransformerCaches(1);

        sleep(1000);

        registerAasObjectsFromTriples(
                aasRegistry,
                aasRepository,
                smRegistry,
                smRepository,
                List.of(ansibleFactsTriple)
        );

        sleep(3000);

        assertTrue(smRegistry.getSubmodelDescriptors().size() == 2);
        assertTrue(getDestinationSubmodels().size() == 1);

        Submodel destinationSubmodel = getDestinationSubmodel();
        List<SubmodelElement> destinationSubmodelElements = destinationSubmodel.getSubmodelElements();

        assertNotNull(destinationSubmodel);

        // Check values of source/target submodel elements are equal:
        submodelElementMap.forEach((sourceSmeIdShort, destinationSmeIdShort) -> {
            assertEquals(
                    getValueOfSubmodelElement(sourceSubmodelElements, sourceSmeIdShort),
                    getValueOfSubmodelElement(destinationSubmodelElements, destinationSmeIdShort)
            );
        });

        // Check if destination Submodel is registered in source shell:
        assertTrue(aasRepository.getAas(sourceShell.getId()).getSubmodels().size() == 2);

        // Check executor hosted Submodel Descriptors
        assertTrue(getDestinationSubmodelDescriptors().size() == 1);
    }

    @Test
    @Order(15)
    public void testGetSubmodelElementsOfTargetExpectTwo() {
        List<SubmodelElement> destinationSubmodelElements = getDestinationSubmodelElements();

        assertTrue(destinationSubmodelElements.size() == 2);
    }

    @Test
    @Order(16)
    public void testGetSubmodelElementOfTargetExpectNotNull() {
        submodelElementMap.forEach((sourceSmeIdShort, destinationSmeIdShort) -> {
            assertDoesNotThrow(() -> getDestinationSubmodelElement(destinationSmeIdShort));
        });
    }

    @Test
    @Order(20)
    public void testUpdateSourceExpectDestinationUpdated() throws InterruptedException {
        assertTransformerCaches(1);
        assertNotEquals(sourceSubmodel, updatedSourceSubmodel);

        // Update existing source submodel:
        smRepository.createOrUpdateSubmodel(updatedSourceSubmodel);

        sleep(2000);

        assertTrue(smRegistry.getSubmodelDescriptors().size() == 2);
        assertTrue(getDestinationSubmodels().size() == 1);

        Submodel destinationSubmodel = getDestinationSubmodel();

        assertEquals(
                getValueOfSubmodelElement(updatedSourceSubmodelElements, "distribution"),
                getValueOfSubmodelElement(destinationSubmodel.getSubmodelElements(), "distribution_new")
        );

        assertEquals(
                getValueOfSubmodelElement(updatedSourceSubmodelElements, "distribution_release"),
                getValueOfSubmodelElement(destinationSubmodel.getSubmodelElements(), "distribution_release_new")
        );

        // Check if destination Submodel is registered in source shell:
        assertTrue(aasRepository.getAas(sourceShell.getId()).getSubmodels().size() == 2);

        // Check executor hosted Submodel Descriptors
        assertTrue(getDestinationSubmodelDescriptors().size() == 1);
    }

    @Test
    @Order(30)
    public void testDeleteSourceExpectDestinationDeleted() throws InterruptedException {
        assertTransformerCaches(1);
        smRepository.deleteSubmodel(sourceSubmodel.getId());
        sleep(2000);

        int smDescriptorsSize = smRegistry.getSubmodelDescriptors().size();

        assertTrue(smDescriptorsSize == 0, "SubmodelDescriptors not empty. Actual size: " + smDescriptorsSize);

        Optional<Submodel> destinationSubmodel = Optional.ofNullable(getDestinationSubmodel());

        assertTrue(destinationSubmodel.isEmpty());

        // Check if destination Submodel is unregistered in source shell:
        assertTrue(aasRepository.getAas(sourceShell.getId()).getSubmodels().size() == 0);

        // Check executor hosted Submodel Descriptors
        assertTrue(getDestinationSubmodelDescriptors().size() == 0);
    }

    // Run two times to test if transformation description got deleted
    @RepeatedTest(2)
    @Order(40)
    public void testDeleteTransformerExpectDestinationDeleted() throws InterruptedException, DeserializationException {
        assertTrue(smRegistry.getSubmodelDescriptors().size() == 0);
        assertTrue(getDestinationSubmodels().size() == 0);
        assertTrue( this.managementClient.getAllTransformer().collectList().block().size() == 1);

        registerAasObjectsFromTriples(
                aasRegistry,
                aasRepository,
                smRegistry,
                smRepository,
                List.of(ansibleFactsTriple)
        );

        sleep(2000);

        assertTrue(smRegistry.getSubmodelDescriptors().size() == 2);
        assertTrue(getDestinationSubmodels().size() == 1);

        deleteAllTransformer();

        sleep(2000);

        assertTrue(smRegistry.getSubmodelDescriptors().size() == 1);
        assertTrue(getDestinationSubmodels().size() == 0);

        // Check executor hosted Submodel Descriptors
        assertTrue(getDestinationSubmodelDescriptors().size() == 0);

        // Cleanup and reset test environment for second run
        deleteAllAasObjects();
        createTestTransformer();
    }

    private void createTestTransformer() {
        managementClient.createTransformer(transformer, false).block();
    }

    private void deleteAllTransformer() {
        managementClient
                .getAllTransformer()
                .collectList()
                .block()
                .forEach(t -> managementClient.deleteTransformer(t.getId(), false).block());
    }

    private void deleteAllAasObjects() throws DeserializationException {
        aasRepository.deleteAllAas();
        smRepository.deleteAllSubmodels();
        smRegistry.getSubmodelDescriptors().forEach(sd -> {
            try {
                smRepository.deleteSubmodel(sd.getId());
            } catch (Exception e) {}
        });
    }

    private void assertTransformerCaches(int expectedCount) {
        List<UUID> executorTransformerIdList = executorWebclient
                .get()
                .uri("/transformer-id-list")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UUID>>() {
                })
                .block();

        List<TransformerDTOListener> listenerTransformerCache = listenerWebclient
                .get()
                .uri("/transformer-cache")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<TransformerDTOListener>>() {
                })
                .block();

        assertTrue(executorTransformerIdList.size() == listenerTransformerCache.size());
        assertTrue(executorTransformerIdList.size() == expectedCount);
    }

    private List<Submodel> getDestinationSubmodels() {
        return executorWebclient
                .get()
                .uri("/submodels")
                .retrieve()
                .bodyToMono(GetSubmodelsResult.class)
                .block()
                .getResult();
    }

    private List<org.eclipse.digitaltwin.basyx.submodelregistry.client.model.SubmodelDescriptor> getDestinationSubmodelDescriptors() {
        return executorWebclient
                .get()
                .uri("/submodel-descriptors")
                .retrieve()
                .bodyToMono(GetSubmodelDescriptorsResult.class)
                .block()
                .getResult();
    }

    private SubmodelDescriptor getDestinationSubmodelDescriptor() {
        return executorWebclient
                .get()
                .uri("/submodel-descriptors/"+b64Encode(destinationSubmodelId))
                .retrieve()
                .bodyToMono(SubmodelDescriptor.class)
                .block();
    }

    private List<SubmodelElement> getDestinationSubmodelElements() {
        return executorWebclient
                .get()
                .uri("/submodels/"+b64Encode(destinationSubmodelId)+"/submodel-elements")
                .retrieve()
                .bodyToMono(GetSubmodelElementsResult.class)
                .block()
                .getResult();
    }

    private SubmodelElement getDestinationSubmodelElement(String submodelElementIdShort) {
        return executorWebclient
                .get()
                .uri("/submodels/"+b64Encode(destinationSubmodelId)+"/submodel-elements/"+b64Encode(submodelElementIdShort))
                .retrieve()
                .bodyToMono(SubmodelElement.class)
                .block();
    }

    private Submodel getDestinationSubmodel() {
        return executorWebclient
                .get()
                .uri("/submodels/"+b64Encode(destinationSubmodelId))
                .retrieve()
                .bodyToMono(Submodel.class)
                .block();
    }

    private String getValueOfSubmodelElement(List<SubmodelElement> submodelElements, String idShort) {
        return submodelElements
                .stream()
                .filter(e -> e.getIdShort().equals(idShort))
                .findFirst()
                .map(se -> ((DefaultProperty) se).getValue())
                .orElse(null);
    }

    private String b64Encode(String str) {
        return Base64.getEncoder().encodeToString(str.getBytes());
    }
}
