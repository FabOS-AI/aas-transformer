package de.fhg.ipa.aas_transformer.service.listener.events;

import de.fhg.ipa.aas_transformer.model.SubmodelChangeEventType;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;

public class SubmodelMessageEvent extends MessageEvent {

    private final Submodel submodel;

    public SubmodelMessageEvent(SubmodelChangeEventType submodelChangeEventType, Submodel submodel) {
        super(submodelChangeEventType);
        this.submodel = submodel;
    }

    public Submodel getSubmodel() {
        return submodel;
    }
}
