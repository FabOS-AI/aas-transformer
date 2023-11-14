package de.fhg.ipa.aas_transformer.service.aas;

import org.eclipse.basyx.aas.registration.proxy.AASRegistryProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AASRegistry extends AASRegistryProxy {

    public AASRegistry(@Value("${aas.registry.url}") String aasRegistryUrl) {
        super(aasRegistryUrl);
    }

}
