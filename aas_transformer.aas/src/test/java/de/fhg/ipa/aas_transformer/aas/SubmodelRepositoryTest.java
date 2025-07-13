package de.fhg.ipa.aas_transformer.aas;


import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.client.internal.ApiClient;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.internal.SubmodelRepositoryApi;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;

import static org.junit.Assert.assertNotNull;

@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SubmodelRepositoryTest {
    String endpoint = "http://xetics.eol-test.h2:80/submodels/UHJvZHVjdF80MDFfcHJvZHVjdGRhdGE";
    String wrongEndpoint = "http://xetics.eol-test.h2:80/submodels/UHJvZHVjdF80MDFfcHJvZHVjdGRhdGEXXXXXXXXX";
    String baseUrl = "http://xetics.eol-test.h2:80";
    String submodelId = "Product_401_productdata";
    String submodelIdB64 = "UHJvZHVjdF80MDFfcHJvZHVjdGRhdGE=";

//    String baseUrl = "http://ui.eol-test.h2:80/api/aas";
//    String submodelId = "http://ipa.fraunhofer.de/h2giga/eol-test-dummy-aas/tests/5";
//    String submodelIdB64 = "aHR0cDovL2lwYS5mcmF1bmhvZmVyLmRlL2gyZ2lnYS9lb2wtdGVzdC1kdW1teS1hYXMvdGVzdHMvNA==";

//    String baseUrl = "http://xetics.eol-test.h2:80";
//    String submodelId = "Equipment_262_equipmentdata";
//    String submodelIdB64 = "RXF1aXBtZW50XzI2Ml9lcXVpcG1lbnRkYXRh";

    @Test
    @Order(10)
    public void testGetSubmodelWithBasePath() {
        ConnectedSubmodelRepository connectedSubmodelRepository = new ConnectedSubmodelRepository(baseUrl);
        Submodel submodel = connectedSubmodelRepository.getSubmodel(submodelId);
        assertNotNull(submodel);
    }

    @Order(20)
    @Test
    public void testGetExtSubmodel() {
        SubmodelRegistry submodelRegistry = new SubmodelRegistry("http://eol-test.h2/sm-registry","http://submodel-repo.local");

//        Submodel submodel = SubmodelRepository.getExtSubmodel(submodelRegistry, submodelId);
        Submodel submodel1 = SubmodelRepository.getExtSubmodel(endpoint);
        Submodel submodel2 = SubmodelRepository.getExtSubmodel(wrongEndpoint);
    }

//    @Test
//    @Order(30)
//    public void testGetSubmodelWithBasePathAndWebclient() {
//        int maxInMemorySize = 2 * 1024 * 1024;
//
//        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
//                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
//                .build();
//        WebClient webClient = WebClient.builder().exchangeStrategies(exchangeStrategies).baseUrl(baseUrl).build();
//
//        String response = webClient
//                .get()
//                .uri("/submodels/"+submodelIdB64)
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//    }
//
}
