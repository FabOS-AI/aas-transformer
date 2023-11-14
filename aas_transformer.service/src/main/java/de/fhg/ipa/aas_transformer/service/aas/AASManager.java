package de.fhg.ipa.aas_transformer.service.aas;

import org.eclipse.basyx.aas.manager.ConnectedAssetAdministrationShellManager;
import org.eclipse.basyx.aas.metamodel.map.AssetAdministrationShell;
import org.eclipse.basyx.aas.registration.api.IAASRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AASManager extends ConnectedAssetAdministrationShellManager {

    private String aasServerUrl;

    public AASManager(
            @Value("${aas.server.url}") String aasServerUrl,
            IAASRegistry aasRegistry) {
        super(aasRegistry);
        this.aasServerUrl = aasServerUrl;
    }

    public void createAAS(AssetAdministrationShell aas) {
        super.createAAS(aas, aasServerUrl);
    }

    public String getAasServerUrl() {
        return this.aasServerUrl;
    }

}
