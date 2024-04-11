package de.fhg.ipa.aas_transformer.service.events;

public class SubmodelElementMessageEvent extends MessageEvent {

    private final String submodelId;

    private final String submodelElementId;

    public SubmodelElementMessageEvent(ChangeEventType changeEventType, String submodelId, String submodelElementId) {
        super(changeEventType);
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
