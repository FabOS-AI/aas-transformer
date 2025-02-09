package de.fhg.ipa.aas_transformer.service.listener.events;

import de.fhg.ipa.aas_transformer.model.SubmodelChangeEventType;

public abstract class MessageEvent {

    protected final SubmodelChangeEventType submodelChangeEventType;

    public MessageEvent(SubmodelChangeEventType submodelChangeEventType) {
        this.submodelChangeEventType = submodelChangeEventType;
    }

    public SubmodelChangeEventType getChangeEventType() {
        return submodelChangeEventType;
    }

}
