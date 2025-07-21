package de.fhg.ipa.aas_transformer.clients.management;

import de.fhg.ipa.aas_transformer.model.TransformerChangeEventDTOListener;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

public abstract class TransformerDTOListenerCache {
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
}
