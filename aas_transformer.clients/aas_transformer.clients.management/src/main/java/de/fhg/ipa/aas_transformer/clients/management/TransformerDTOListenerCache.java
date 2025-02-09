package de.fhg.ipa.aas_transformer.clients.management;

import de.fhg.ipa.aas_transformer.model.TransformerChangeEventDTOListener;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class TransformerDTOListenerCache {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerDTOListenerCache.class);

    protected final int connectionRetryTimeoutInMs = 1000;
    protected final ManagementClient managementClient;
    protected Flux<TransformerDTOListener> transformerFlux;
    protected Flux<TransformerChangeEventDTOListener> transformerEventFlux;

    public TransformerDTOListenerCache(ManagementClient managementClient) {
        this.managementClient = managementClient;
        this.transformerFlux = this.managementClient.getAllTransformerDTOListener();
        this.transformerEventFlux = this.managementClient.getTransformerChangeEventDTOListenerStream();
    }

//    @PostConstruct
//    public void init() {
//        this.transformerFlux
//            .doOnError(e -> {
//                LOG.warn("ManagementClient failed to connect: " + e.getMessage() + " - retrying in " + connectionRetryTimeoutInMs + " ms...");
//            })
//            .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofMillis(connectionRetryTimeoutInMs)))
//            .doOnComplete(() -> {
//                this.transformerEventFlux.log().subscribe(this::handleTransformerEvent);
//            })
//            .subscribe(this::addTransformer);
//    }
//
//    public List<TransformerDTOListener> getTransformers() {
//        return transformers;
//    }
//
//    private void addTransformer(TransformerDTOListener transformer) {
//        transformers.add(transformer);
//    }
//
//    private void deleteTransformer(TransformerDTOListener transformer) {
//        transformers.removeIf(t -> t.getId().equals(transformer.getId()));
//    }
//
//    private void handleTransformerEvent(TransformerChangeEventDTOListener event) {
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
