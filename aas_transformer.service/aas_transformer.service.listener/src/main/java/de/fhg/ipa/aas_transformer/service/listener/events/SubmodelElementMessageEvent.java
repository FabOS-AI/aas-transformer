package de.fhg.ipa.aas_transformer.service.listener.events;

import de.fhg.ipa.aas_transformer.model.SubmodelChangeEventType;

public class SubmodelElementMessageEvent extends MessageEvent {

    private final String submodelId;

    private final String submodelElementId;

    public SubmodelElementMessageEvent(SubmodelChangeEventType submodelChangeEventType, String submodelId, String submodelElementId) {
        super(submodelChangeEventType);
        this.submodelId = submodelId;
        this.submodelElementId = submodelElementId;
    }

    public String getSubmodelId() {
        return submodelId;
    }

    public String getSubmodelElementId() {
        return submodelElementId;
    }
}
