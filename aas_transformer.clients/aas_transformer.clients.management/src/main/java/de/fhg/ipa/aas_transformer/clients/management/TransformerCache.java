package de.fhg.ipa.aas_transformer.clients.management;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEvent;
import reactor.core.publisher.Flux;

public abstract class TransformerCache {
    protected final int connectionRetryTimeoutInMs = 1000;
    protected final ManagementClient managementClient;
    protected Flux<Transformer> transformerFlux;
    protected Flux<TransformerChangeEvent> transformerEventFlux;

    public TransformerCache(ManagementClient managementClient) {
        this.managementClient = managementClient;
        this.transformerFlux = this.managementClient.getAllTransformer();
        this.transformerEventFlux = this.managementClient.getTransformerChangeEventStream();
    }
}
