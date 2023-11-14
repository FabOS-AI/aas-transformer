package de.fhg.ipa.aas_transformer.service;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.actions.TransformerActionService;
import de.fhg.ipa.aas_transformer.service.actions.TransformerActionServiceFactory;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.eclipse.basyx.aas.metamodel.api.parts.asset.AssetKind;
import org.eclipse.basyx.aas.metamodel.map.AssetAdministrationShell;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.aas.metamodel.map.parts.Asset;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.api.reference.enums.KeyElements;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.reference.Reference;
import org.eclipse.basyx.vab.exception.provider.ProviderException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransformerService {

    private final Transformer transformer;

    private final TemplateRenderer templateRenderer;

    private final AASRegistry aasRegistry;

    private final AASManager aasManager;

    private final TransformerActionServiceFactory transformerActionServiceFactory;

    private List<TransformerActionService> transformerActionServices = new ArrayList<>();

    public TransformerService(Transformer transformer, TemplateRenderer templateRenderer, AASRegistry aasRegistry, AASManager aasManager, TransformerActionServiceFactory transformerActionServiceFactory) {
        this.transformer = transformer;
        this.templateRenderer = templateRenderer;
        this.aasRegistry = aasRegistry;
        this.aasManager = aasManager;
        this.transformerActionServiceFactory = transformerActionServiceFactory;

        for (var transformerAction : transformer.getTransformerActions()) {
            var transformerActionService = this.transformerActionServiceFactory.create(transformerAction);
            this.transformerActionServices.add(transformerActionService);
        }
    }

    public void execute(IIdentifier sourceAASId) {
        Map<String, Object> renderContext = Map.of(
                "SOURCE_AAS", sourceAASId.getId()
        );

        IIdentifier destinationAASId;
        if (transformer.getDestination().getAasDestination() != null) {
            destinationAASId = this.templateRenderer
                    .render(transformer.getDestination().getAasDestination().getIdentifier(), renderContext);
            var destinationAASIdShort = this.templateRenderer
                    .render(transformer.getDestination().getAasDestination().getIdShort(), renderContext);

            if (!sourceAASId.equals(transformer.getDestination().getAasDestination().getIdentifier())) {

                try {
                    // Check if destination AAS exists
                    var destinationAASDescriptor = this.aasRegistry.lookupAAS(transformer.getDestination().getAasDestination().getIdentifier());
                } catch (ProviderException e) {
                    // Create destination AAS if not existing
                    var newDestinationAAS = new AssetAdministrationShell(
                            destinationAASIdShort,
                            destinationAASId,
                            new Asset(destinationAASIdShort,
                                    destinationAASId,
                                    AssetKind.INSTANCE)
                    );
                    var aasServerEndpoint = this.templateRenderer
                            .render(transformer.getDestination().getAasDestination().getEndpoint(), renderContext);
                    this.aasManager.createAAS(newDestinationAAS, aasServerEndpoint);
                }
            }
        }
        else {
            destinationAASId = sourceAASId;
        }

        var destinationSubmodel = new Submodel();
        destinationSubmodel.setIdShort(transformer.getDestination().getSmDestination().getIdShort());
        destinationSubmodel.setParent(new Reference(sourceAASId, KeyElements.ASSETADMINISTRATIONSHELL, true));
        destinationSubmodel.setIdentification(
                new CustomId(destinationSubmodel.getIdShort())
        );

        aasManager.createSubmodel(destinationAASId, destinationSubmodel);
        var newDestinationSubmodel = aasManager.retrieveSubmodel(destinationAASId, destinationSubmodel.getIdentification());


        for (var transformerActionService : transformerActionServices) {
            transformerActionService.execute(sourceAASId, newDestinationSubmodel);
        }
    }

    public boolean isSubmodelSourceOfTransformerActions(Submodel submodel) {
        for (var transformerActionService : this.transformerActionServices) {
            if (transformerActionService.isSubmodelSourceOfTransformerAction(submodel)) {
                return true;
            }
        }
        return false;
    }
}
