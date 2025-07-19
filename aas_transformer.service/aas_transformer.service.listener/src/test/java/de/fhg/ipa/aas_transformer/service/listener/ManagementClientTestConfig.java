package de.fhg.ipa.aas_transformer.service.listener;

import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEventDTOListener;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesTriples;

@TestConfiguration
public class ManagementClientTestConfig {
    @MockBean
    ManagementClient managementClient;
    Sinks.Many<TransformerChangeEventDTOListener> changeEventDtoListenerSink =
            Sinks.many().multicast().onBackpressureBuffer();

    @PostConstruct
    public void initMock() {
        List<List<Object>> triple = getRandomTimeseriesTriples(1);
        Transformer transformer = (Transformer) triple.get(0).get(2);
        TransformerDTOListener transformerDtoListener = new TransformerDTOListener(
                transformer.getId(),
                transformer.getDestination(),
                transformer.getSourceSubmodelIdRules(),
                transformer.getTransformOnRequest()
        );

        Mockito
                .when(managementClient.getAllTransformerDTOListener())
                .thenReturn(Flux.just(transformerDtoListener));
        Mockito
                .when(managementClient.getTransformerChangeEventDTOListenerStream())
                .thenReturn(changeEventDtoListenerSink.asFlux());
    }
}
