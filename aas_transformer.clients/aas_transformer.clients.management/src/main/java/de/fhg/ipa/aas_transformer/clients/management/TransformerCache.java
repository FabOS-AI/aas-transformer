package de.fhg.ipa.aas_transformer.clients.management;

import de.fhg.ipa.aas_transformer.clients.ApiClient;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEvent;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEventDTOListener;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

public class TransformerCache {
    protected final int connectionRetryTimeoutInMs = 1000;
    private final ManagementClient managementClient;
//    private List<Transformer> transformers = new ArrayList<>();
    protected Flux<Transformer> transformerFlux;
    protected Flux<TransformerChangeEvent> transformerEventFlux;

    public TransformerCache(ManagementClient managementClient) {
        this.managementClient = managementClient;
        this.transformerFlux = this.managementClient.getAllTransformer();
        this.transformerEventFlux = this.managementClient.getTransformerChangeEventStream();
    }

//    @PostConstruct
//    public void init() {
//        transformerFlux
//            .doOnError(e -> {
//                LOG.warn("ManagementClient failed to connect: " + e.getMessage() + " - retrying in " + connectionRetryTimeoutInMs + " ms...");
//            })
//            .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofMillis(connectionRetryTimeoutInMs)))
//            .doOnComplete(() -> {
//                transformerEventFlux.log().subscribe(this::handleTransformerEvent);
//            })
//            .subscribe(this::addTransformer);
//    }
//
//    public List<Transformer> getTransformers() {
//        return transformers;
//    }
//
//    private void addTransformer(Transformer transformer) {
//        transformers.add(transformer);
//    }
//
//    private void deleteTransformer(Transformer transformer) {
//        transformers.removeIf(t -> t.getId().equals(transformer.getId()));
//    }
//
//    private void handleTransformerEvent(TransformerChangeEvent event) {
//        switch (event.getType()) {
//            case CREATE:
//            case UPDATE:
//                addTransformer(event.getTransformer());
//                break;
//            case DELETE:
//                deleteTransformer(event.getTransformer());
//                break;
//        }
//    }
}
