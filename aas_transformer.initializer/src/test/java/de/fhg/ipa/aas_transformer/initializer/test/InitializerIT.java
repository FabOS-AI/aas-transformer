package de.fhg.ipa.aas_transformer.initializer.test;

import de.fhg.ipa.aas_transformer.model.Transformer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static de.fhg.ipa.aas_transformer.initializer.test.InitializerITConfig.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InitializerIT {
    InitializerITConfig initializerITConfig = new InitializerITConfig();

    @BeforeAll
    public void beforeAll() {
        //RestAssured
        RestAssured.port = dockerCompose.getServicePort(
                AAS_TRANSFORMER_SERVICE_NAME,
                AAS_TRANSFORMER_SERVICE_PORT
        );
        RestAssured.basePath = "/aas/transformer";
    }

    @Test
    @Order(10)
    public void testJsonFileListAndTransformerListHaveSameLength() {
        assertEquals(
            jsonFiles.size(),
            transformerListFromFiles.size()
        );
    }

    @Test
    @Order(20)
    public void testTransformerApiAvailable() {
        given()
                .get()
                .then()
                .statusCode(200);
    }

    @Test
    @Order(30)
    public void testGetAllTransformerExpectLengthOfTransformerList() throws InterruptedException {

        for(int x = 0; x < 10; x++) {
            try {
                given()
                        .get()
                        .then()
                        .assertThat()
                        .body("size()", is(transformerListFromFiles.size()));

            } catch (AssertionError e) {
                Thread.sleep(1000);
            }

        }
    }

    @Test
    @Order(40)
    public void testGetTransformerByIdAndExpectInstanceOfTransformer() throws IOException {
        for(Transformer transformer : transformerListFromFiles) {
            given()
                    .basePath("/aas/transformer/{transformerId}")
                    .pathParam("transformerId", transformer.getId())
                    .get()
                    .then()
                    .assertThat()
                    .statusCode(200)
                    .body("id", equalTo(transformer.getId().toString()))
                    .body("destination.idShort", equalTo(transformer.getDestination().getSubmodelDestination().getIdShort()))
                    .body("transformerActions.size()", is(transformer.getTransformerActions().size()));
        }
    }
}
