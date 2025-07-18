package de.fhg.ipa.aas_transformer.model.message_event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.fhg.ipa.aas_transformer.model.SubmodelChangeEventType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = false)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SubmodelMessageEvent.class, name = "SubmodelMessageEvent"),
        @JsonSubTypes.Type(value = SubmodelElementMessageEvent.class, name = "SubmodelElementMessageEvent")
})
public abstract class MessageEvent {

    private SubmodelChangeEventType submodelChangeEventType;

    public MessageEvent() {}

    public MessageEvent(SubmodelChangeEventType submodelChangeEventType) {
        this.submodelChangeEventType = submodelChangeEventType;
    }

    public SubmodelChangeEventType getSubmodelChangeEventType() {
        return submodelChangeEventType;
    }

    public void setSubmodelChangeEventType(SubmodelChangeEventType submodelChangeEventType) {
        this.submodelChangeEventType = submodelChangeEventType;
    }
}