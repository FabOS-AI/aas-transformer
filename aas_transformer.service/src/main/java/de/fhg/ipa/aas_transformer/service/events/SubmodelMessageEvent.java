package de.fhg.ipa.aas_transformer.service.events;

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;

public class SubmodelMessageEvent extends MessageEvent {

    private final Submodel submodel;

    public SubmodelMessageEvent(ChangeEventType changeEventType, Submodel submodel) {
        super(changeEventType);
        this.submodel = submodel;
    }

    public Submodel getSubmodel() {
        return submodel;
    }
}
