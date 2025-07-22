package de.fhg.ipa.aas_transformer.clients.management;

import de.fhg.ipa.aas_transformer.model.TransformerChangeEventDTOListener;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import reactor.core.publisher.Flux;

public abstract class TransformerDTOListenerCache {
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
