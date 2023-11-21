package de.fhg.ipa.aas_transformer.service.events;

import org.eclipse.basyx.aas.metamodel.map.descriptor.AASDescriptor;

public class AASMessageEvent extends MessageEvent {

    private final AASDescriptor aasDescriptor;

    public AASMessageEvent(ChangeEventType changeEventType, AASDescriptor aasDescriptor) {
        super(changeEventType);
        this.aasDescriptor = aasDescriptor;
    }

    public AASDescriptor getAASDescriptor() {
        return aasDescriptor;
    }
}
