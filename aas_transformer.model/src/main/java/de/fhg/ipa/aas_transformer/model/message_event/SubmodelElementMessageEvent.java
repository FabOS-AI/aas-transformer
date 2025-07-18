package de.fhg.ipa.aas_transformer.model.message_event;

import de.fhg.ipa.aas_transformer.model.SubmodelChangeEventType;

public class SubmodelElementMessageEvent extends MessageEvent {

    private String submodelId;

    private String submodelElementId;

    public SubmodelElementMessageEvent() {}

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

    public void setSubmodelId(String submodelId) {
        this.submodelId = submodelId;
    }

    public void setSubmodelElementId(String submodelElementId) {
        this.submodelElementId = submodelElementId;
    }
}
