package de.fhg.ipa.aas_transformer.service;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.service.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.service.aas.AasRepository;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.actions.TransformerActionService;
import de.fhg.ipa.aas_transformer.service.actions.TransformerActionServiceFactory;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TransformerService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerService.class);

    private final Transformer transformer;

    private final TemplateRenderer templateRenderer;

    private final AasRegistry aasRegistry;

    private final AasRepository aasRepository;

    private final SubmodelRegistry submodelRegistry;

    private final SubmodelRepository submodelRepository;

    private final TransformerActionServiceFactory transformerActionServiceFactory;

    private List<TransformerActionService> transformerActionServices = new ArrayList<>();

    public TransformerService(Transformer transformer,
                              TemplateRenderer templateRenderer,
                              AasRegistry aasRegistry, AasRepository aasRepository,
                              SubmodelRegistry submodelRegistry, SubmodelRepository submodelRepository,
                              TransformerActionServiceFactory transformerActionServiceFactory) {
        this.transformer = transformer;
        this.templateRenderer = templateRenderer;
        this.aasRegistry = aasRegistry;
        this.aasRepository = aasRepository;
        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;
        this.transformerActionServiceFactory = transformerActionServiceFactory;

        for (var transformerAction : transformer.getTransformerActions()) {
            var transformerActionService = this.transformerActionServiceFactory.create(transformerAction);
            this.transformerActionServices.add(transformerActionService);
        }
    }

    public void execute(Submodel sourceSubmodel) {
        // Set context for template rendering
        var context = new HashMap<String, Object>();
        var jsonSerializer = new JsonSerializer();
        try {
            var serializedSourceSubmodel = jsonSerializer.write(sourceSubmodel);
            context.put("SOURCE_SUBMODEL", serializedSourceSubmodel);
        } catch (SerializationException e) {
            LOG.error(e.getMessage());
        }

        try {
            // Create or get destination submodel
            var destinationSubmodelId = this.templateRenderer
                    .render(this.transformer.getDestination().getSubmodelDestination().getId(), context);
            var destinationSubmodelIdShort = this.templateRenderer
                    .render(this.transformer.getDestination().getSubmodelDestination().getIdShort(), context);

            var optionalSubmodelDescriptor = this.submodelRegistry.findSubmodelDescriptor(destinationSubmodelId);
            if (optionalSubmodelDescriptor.isEmpty()) {
                var newDestinationSubmodel = new DefaultSubmodel();
                newDestinationSubmodel.setIdShort(destinationSubmodelIdShort);
                newDestinationSubmodel.setId(destinationSubmodelId);
                this.submodelRepository.createOrUpdateSubmodel(newDestinationSubmodel);
            }
            var destinationSubmodel = this.submodelRepository.getSubmodel(destinationSubmodelId);

            // Execute transformer actions
            for (var transformerActionService : this.transformerActionServices) {
                transformerActionService.execute(sourceSubmodel, destinationSubmodel, context);
            }

            // Add reference to submodel in destination AAS
            if (transformer.getDestination().getAasDestination() != null) {
                var destinationAasId = this.templateRenderer
                        .render(transformer.getDestination().getAasDestination().getId(), context);
                // Check if destination AAS exists
                var destinationAASDescriptorOptional = this.aasRegistry.getAasDescriptor(destinationAasId);
                if (destinationAASDescriptorOptional.isEmpty()) {
                    var newDestinationAAS = new DefaultAssetAdministrationShell.Builder()
                            .id(destinationAasId)
                            .build();
                    this.aasRepository.createOrUpdateAas(newDestinationAAS);
                }

                var destinationAas = this.aasRepository.getAas(destinationAasId);
                var destinationSubmodelDescriptorOptional = this.submodelRegistry.findSubmodelDescriptor(destinationSubmodel.getId());
                this.aasRegistry.addSubmodelDescriptorToAas(destinationAas.getId(), destinationSubmodelDescriptorOptional.get());
                this.aasRepository.addSubmodelReferenceToAas(destinationAas.getId(), destinationSubmodel);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
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
