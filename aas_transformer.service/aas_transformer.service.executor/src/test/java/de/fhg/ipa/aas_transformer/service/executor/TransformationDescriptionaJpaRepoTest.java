package de.fhg.ipa.aas_transformer.service.executor;

import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import de.fhg.ipa.aas_transformer.model.TransformationDescription;
import de.fhg.ipa.aas_transformer.persistence.api.TransformationDescriptionJpaRepository;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MariaDbExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class TransformationDescriptionaJpaRepoTest {
    @Autowired
    private TransformationDescriptionJpaRepository transformationDescriptionJpaRepository;

    private static String targetSubmodelId = "test-target-submodel-id";
    private static UUID transformerId = UUID.fromString("28b5f79f-f59f-41a7-b29e-63599f373e76");
    private static TransformationDescription transformationDescription = new TransformationDescription(
            null,
            transformerId,
            "test-source-submodel-id",
            targetSubmodelId
    );

    @Test
    @Order(10)
    public void testGetAllTransformationDescriptionsExpectNone() {
        List<TransformationDescription> descriptions = transformationDescriptionJpaRepository.findAll().collectList().block();
        assertTrue(descriptions.size() == 0);
    }

    @Test
    @Order(20)
    public void testCreateTransformationDescription() {
        transformationDescriptionJpaRepository.save(transformationDescription).block();
    }

    @Test
    @Order(25)
    public void testCreateTransformationDescriptionAgain() {
        assertThrows(DuplicateKeyException.class, () -> {
            transformationDescriptionJpaRepository.save(transformationDescription).block();
        });
    }

    @Test
    @Order(30)
    public void testGetAllTransformationDescriptionsExpectOne() {
        List<TransformationDescription> descriptions = transformationDescriptionJpaRepository.findAll().collectList().block();
        assertTrue(descriptions.size() == 1);
    }

    @Test
    @Order(40)
    public void testGetTransformationDescriptionBySourceSubmodelId() {
        TransformationDescription description = transformationDescriptionJpaRepository
                .findByTargetSubmodelId(targetSubmodelId).block();
        assertNotNull(description);

        assertEquals(
                transformationDescription.getTargetSubmodelId(),
                description.getTargetSubmodelId()
        );
    }

    @Test
    @Order(50)
    public void testDeleteTransformationDescriptionByTransformerId() {
        List<TransformationDescription> descriptionsToBeDeleted = transformationDescriptionJpaRepository.findByTransformerId(transformerId).collectList().block();
        assertTrue(descriptionsToBeDeleted.size() == 1);

        descriptionsToBeDeleted.forEach(
                description -> transformationDescriptionJpaRepository.delete(description).block()
        );
        List<TransformationDescription> descriptions = transformationDescriptionJpaRepository.findAll().collectList().block();
        assertTrue(descriptions.size() == 0);
    }

}
