package de.fhg.ipa.aas_transformer.model.message_event;

import de.fhg.ipa.aas_transformer.model.SubmodelChangeEventType;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;

public class SubmodelMessageEvent extends MessageEvent {

    private Submodel submodel;

    public SubmodelMessageEvent() {}

    public SubmodelMessageEvent(SubmodelChangeEventType submodelChangeEventType, Submodel submodel) {
        super(submodelChangeEventType);
        this.submodel = submodel;
    }

    public Submodel getSubmodel() {
        return submodel;
    }

    public void setSubmodel(Submodel submodel) {
        this.submodel = submodel;
    }
}
