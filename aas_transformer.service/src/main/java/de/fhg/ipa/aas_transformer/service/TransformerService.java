package de.fhg.ipa.aas_transformer.service;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.actions.TransformerActionService;
import de.fhg.ipa.aas_transformer.service.actions.TransformerActionServiceFactory;
import de.fhg.ipa.aas_transformer.service.events.consumers.MessageEventConsumer;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.eclipse.basyx.aas.metamodel.api.parts.asset.AssetKind;
import org.eclipse.basyx.aas.metamodel.map.AssetAdministrationShell;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.aas.metamodel.map.descriptor.SubmodelDescriptor;
import org.eclipse.basyx.aas.metamodel.map.parts.Asset;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.api.reference.enums.KeyElements;
import org.eclipse.basyx.submodel.metamodel.connected.ConnectedSubmodel;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.reference.Reference;
import org.eclipse.basyx.vab.exception.provider.ProviderException;
import org.eclipse.basyx.vab.exception.provider.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TransformerService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerService.class);

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

    public void execute(IIdentifier sourceAASId, IIdentifier sourceSMId) {
        try {
            var renderContext = new HashMap<String, Object>();
            renderContext.put("SOURCE_AAS", sourceAASId.getId());
            if (sourceSMId != null) {
                renderContext.put("SOURCE_SM", sourceSMId.getId());
            }

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
            } else {
                destinationAASId = sourceAASId;
            }

            ISubmodel destinationSubmodel = null;
            try {
                // Check if destination SM exists
                var destinationAASDescriptor = this.aasRegistry.lookupAAS(destinationAASId);

                var existingSubmodelsOfDestinationAAS = this.aasRegistry.lookupSubmodels(destinationAASId);
                var destionationSMDescriptorOptional = existingSubmodelsOfDestinationAAS.stream().filter(
                                smd -> smd.getIdShort().equals(transformer.getDestination().getSmDestination().getIdShort()))
                        .findAny();

                if (destionationSMDescriptorOptional.isEmpty()) {
                    var newDestinationSubmodel = new Submodel();
                    newDestinationSubmodel.setIdShort(transformer.getDestination().getSmDestination().getIdShort());
                    newDestinationSubmodel.setParent(new Reference(destinationAASId, KeyElements.ASSETADMINISTRATIONSHELL, true));
                    if (transformer.getDestination().getSmDestination().getIdentifier() == null) {
                        newDestinationSubmodel.setIdentification(new CustomId(newDestinationSubmodel.getIdShort()));
                    } else {
                        newDestinationSubmodel.setIdentification(transformer.getDestination().getSmDestination().getIdentifier());
                    }
                    aasManager.createSubmodel(destinationAASId, newDestinationSubmodel);
                    destinationSubmodel = aasManager.retrieveSubmodel(destinationAASId, newDestinationSubmodel.getIdentification());
                }
                else {
                    destinationSubmodel = aasManager.retrieveSubmodel(destinationAASId, destionationSMDescriptorOptional.get().getIdentifier());
                }
            }
            catch (Exception e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }

            for (var transformerActionService : transformerActionServices) {
                transformerActionService.execute(sourceAASId, sourceSMId, destinationAASId, destinationSubmodel);
            }
        } catch (ResourceNotFoundException e) {
            LOG.error(e.getMessage());
        }
    }

    public boolean isSubmodelSourceOfTransformerActions(SubmodelDescriptor submodelDescriptor) {
        for (var transformerActionService : this.transformerActionServices) {
            if (transformerActionService.isSubmodelSourceOfTransformerAction(submodelDescriptor)) {
                return true;
            }
        }
        return false;
    }
}
