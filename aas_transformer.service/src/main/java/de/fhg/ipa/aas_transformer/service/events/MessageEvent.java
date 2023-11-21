package de.fhg.ipa.aas_transformer.service.events;

public abstract class MessageEvent {

    protected final ChangeEventType changeEventType;

    public MessageEvent(ChangeEventType changeEventType) {
        this.changeEventType = changeEventType;
    }

    public ChangeEventType getChangeEventType() {
        return changeEventType;
    }

}
