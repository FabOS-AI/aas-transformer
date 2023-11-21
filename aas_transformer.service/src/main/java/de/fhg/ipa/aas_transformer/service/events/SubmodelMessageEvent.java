package de.fhg.ipa.aas_transformer.service.events;

import org.eclipse.basyx.aas.metamodel.map.descriptor.SubmodelDescriptor;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;

public class SubmodelMessageEvent extends MessageEvent {

    private final SubmodelDescriptor submodelDescriptor;

    private final IIdentifier aasId;

    public SubmodelMessageEvent(ChangeEventType changeEventType, SubmodelDescriptor submodelDescriptor, IIdentifier aasId) {
        super(changeEventType);
        this.submodelDescriptor = submodelDescriptor;
        this.aasId = aasId;
    }

    public SubmodelDescriptor getSubmodelDescriptor() {
        return submodelDescriptor;
    }

    public IIdentifier getAasId() {
        return aasId;
    }
}
