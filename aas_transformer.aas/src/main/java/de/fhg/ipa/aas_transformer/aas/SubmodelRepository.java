package de.fhg.ipa.aas_transformer.aas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.fhg.ipa.aas_transformer.aas.deserializer.SubmodelDeserializer;
import de.fhg.ipa.aas_transformer.aas.serializer.SubmodelSerializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.basyx.core.exceptions.CollidingIdentifierException;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.eclipse.digitaltwin.basyx.core.pagination.CursorResult;
import org.eclipse.digitaltwin.basyx.core.pagination.PaginationInfo;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.SubmodelDescriptor;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Component
public class SubmodelRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelRepository.class);

    private final static int DEFAULT_IN_MEMORY_SIZE = 16 * 1024 * 1024;
    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final static SimpleModule simpleModule = new SimpleModule();
    static {
        simpleModule.addSerializer(Submodel.class, new SubmodelSerializer());
        simpleModule.addDeserializer(Submodel.class, new SubmodelDeserializer());
        objectMapper.registerModule(simpleModule);
    }
    private final String submodelRepositoryUrl;
    private WebClient webClient;

    private final ConnectedSubmodelRepository connectedSubmodelRepository;

    public SubmodelRepository(
            @Value("${aas.submodel-repository.url}") String submodelRepositoryUrl
    ) {
        this.submodelRepositoryUrl = submodelRepositoryUrl;
        this.connectedSubmodelRepository = new ConnectedSubmodelRepository(submodelRepositoryUrl);
        webClient = WebClient.builder()
                .baseUrl(submodelRepositoryUrl)
                .build();
        LOG.info("SubmodelRepository initialized with URL: {}", submodelRepositoryUrl);
    }

    public boolean isSubmodelRepositoryAvailable() {
        try {
            webClient.get().retrieve().toBodilessEntity().block().getStatusCode();
        } catch (WebClientResponseException e) {
            if (!e.getStatusCode().is4xxClientError())
                return false;
        }
        return true;
    }

    public List<Submodel> getSubmodelsWithLimit(int limit) {
        return this.connectedSubmodelRepository.getAllSubmodels(new PaginationInfo(limit, "")).getResult();
    }

    public List<Submodel> getAllSubmodels() throws DeserializationException {
        int limit = 100;
        String cursor = "";
        List<Submodel> submodels = new ArrayList<>();
        do {
            CursorResult<List<Submodel>> resultSms = this.connectedSubmodelRepository.getAllSubmodels(
                    new PaginationInfo(limit, cursor)
            );
            submodels.addAll(resultSms.getResult());
            cursor = resultSms.getCursor();
        } while(cursor != null);

        return submodels;
    }

    public Submodel getSubmodel(String submodelId) {
        var submodel = this.connectedSubmodelRepository.getSubmodel(submodelId);
        return submodel;
    }

    public static Submodel getExtSubmodel(
            SubmodelRegistry submodelRegistry,
            String submodelId
    ) {
        SubmodelDescriptor descriptor;
        int maxRetries = 3;
        int attempt = 0;
        int sleepTimeMs = 1000; // 1 Sekunde
        while (true) {
            try {
                descriptor = submodelRegistry.findSubmodelDescriptor(submodelId).orElseThrow();
                LOG.info("SubmodelDescriptor found by SubmodelId = {}", submodelId);
                break;
            } catch (NoSuchElementException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    LOG.error("SubmodelDescriptor with id {} not found in registry after {} attempts", submodelId, attempt);
                    return null;
                }
                LOG.warn("SubmodelDescriptor with id {} not found, retrying... (attempt {}/{})", submodelId, attempt, maxRetries);
                try {
                    Thread.sleep(sleepTimeMs); // 1 Sekunde warten
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        String endpoint = descriptor.getEndpoints().get(0).getProtocolInformation().getHref();
        return getExtSubmodel(endpoint);
    }

    public static Submodel getExtSubmodel(String endpoint, String submodelId) {
        String baseUrl = getSubmodelRepositoryBaseUrl(endpoint);
        ConnectedSubmodelRepository connectedSubmodelRepository = new ConnectedSubmodelRepository(baseUrl);
        LOG.info("Getting submodel with id: {} from {}", submodelId, baseUrl);
        return connectedSubmodelRepository.getSubmodel(submodelId);
    }

    public static Submodel getExtSubmodel(String endpoint) {
        LOG.info("Getting submodel from {}", endpoint);
        try {
            return getWebClient(endpoint)
                    .get()
                    .retrieve()
                    .bodyToMono(Submodel.class)
//                    .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(1)))
                    .block();
        } catch (Exception e) {
            LOG.error("Failed to get submodel from {}", endpoint);
            return null;
        }
    }

    private static WebClient getWebClient(String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                })
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_IN_MEMORY_SIZE))
                        .build())
                .build();
    }

    private static String getSubmodelRepositoryBaseUrl(String endpoint) {
        String delimiter = "/submodels";
        int index = endpoint.indexOf(delimiter);
        if(index == -1)
            return endpoint;
        return endpoint.substring(0, index);
    }

    public void createOrUpdateSubmodel(Submodel submodel) {
        try {
            LOG.info("Creating submodel with id: {}", submodel.getId());
            this.connectedSubmodelRepository.createSubmodel(submodel);
        } catch (CollidingIdentifierException e) {
            LOG.info("Updating submodel with id: {}", submodel.getId());
            this.connectedSubmodelRepository.updateSubmodel(submodel.getId(), submodel);
        }
        catch (RuntimeException e) {
            LOG.error(e.getMessage());
        }
    }

    public void deleteSubmodel(String submodelId) {
        this.connectedSubmodelRepository.deleteSubmodel(submodelId);
    }

    public void deleteAllSubmodels() throws DeserializationException {
        getAllSubmodels().forEach(submodel -> {
            deleteSubmodel(submodel.getId());
        });
    }

    public SubmodelElement getSubmodelElement(String submodelId, String smeIdShort) {
        var submodelElement = this.connectedSubmodelRepository.getSubmodelElement(submodelId, smeIdShort);
        return submodelElement;
    }

    public void createSubmodelElement(String submodelId, SubmodelElement submodelElement) {
        this.connectedSubmodelRepository.createSubmodelElement(submodelId, submodelElement);
    }

    public void updateSubmodelElement(String submodelId, String idShortPath, SubmodelElement submodelElement) {
        this.connectedSubmodelRepository.updateSubmodelElement(submodelId, idShortPath, submodelElement);
    }

    public void createOrUpdateSubmodelElement(String submodelId, String idShortPath, SubmodelElement submodelElement) {
        try {
            this.updateSubmodelElement(submodelId, idShortPath, submodelElement);
        } catch (ElementDoesNotExistException e) {
            submodelElement.setIdShort(idShortPath);
            this.connectedSubmodelRepository.createSubmodelElement(
                    submodelId,
                    submodelElement
            );
        }
    }

}
