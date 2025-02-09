package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.model.DestinationAAS;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationLog;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionService;
import de.fhg.ipa.aas_transformer.transformation.actions.TransformerActionServiceFactory;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class TransformationExecutionService {
    private static final Logger LOG = LoggerFactory.getLogger(TransformationExecutionService.class);

    private final Transformer transformer;
    private final TemplateRenderer templateRenderer;
    private final AasRegistry aasRegistry;
    private final AasRepository aasRepository;
    private final SubmodelRegistry submodelRegistry;
    private final SubmodelRepository submodelRepository;
    private final TransformerActionServiceFactory transformerActionServiceFactory;
    private final MetricsClient metricsClient;
    private List<TransformerActionService> transformerActionServices = new ArrayList<>();

    public TransformationExecutionService(
            Transformer transformer,
            TemplateRenderer templateRenderer,
            AasRegistry aasRegistry,
            AasRepository aasRepository,
            SubmodelRegistry submodelRegistry,
            SubmodelRepository submodelRepository,
            TransformerActionServiceFactory transformerActionServiceFactory,
            MetricsClient metricsClient
    ) {
        this.transformer = transformer;
        this.templateRenderer = templateRenderer;
        this.aasRegistry = aasRegistry;
        this.aasRepository = aasRepository;
        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;
        this.transformerActionServiceFactory = transformerActionServiceFactory;
        this.metricsClient = metricsClient;

        for (var transformerAction : transformer.getTransformerActions()) {
            var transformerActionService = this.transformerActionServiceFactory.create(transformerAction);
            this.transformerActionServices.add(transformerActionService);
        }
    }

    public UUID getTransformerId() {
        return this.transformer.getId();
    }

    public static List<AssetAdministrationShell> lookupDestinationShells(
            AasRepository aasRepository,
            String sourceSubmodelId,
            DestinationAAS destinationAas
    ) {
        List<String> destinationShellIds = lookupDestinationShellIds(aasRepository, sourceSubmodelId, destinationAas);
        List<AssetAdministrationShell> destinationShells = new ArrayList<>();
        for(String shellId: destinationShellIds) {
            destinationShells.add(aasRepository.getAas(shellId));
        }
        return destinationShells;
    }

    public static List<String> lookupDestinationShellIds(
            AasRepository aasRepository,
            String sourceSubmodelId,
            DestinationAAS destinationAas
    ) {
        List<String> destinationShellIds;
        if(destinationAas != null) {
            destinationShellIds = List.of(destinationAas.getId());
        } else {
            destinationShellIds =  aasRepository
                    .getAllAasContainingSubmodelBySubmodelId(sourceSubmodelId)
                    .stream()
                    .map(shell -> shell.getId())
                    .toList();
        }
        return destinationShellIds;
    }

    public void execute(TransformationJob job, UUID executorId) {
        Submodel sourceSubmodel;
        Instant startLookupSource = Instant.now();
        if(job.getSubmodel() == null) {
            // Lookup Source Submodel by ID
            sourceSubmodel = this.submodelRepository.getSubmodel(job.getSubmodelId());
        } else {
            sourceSubmodel = job.getSubmodel();
        }
        Instant endLookupSource = Instant.now();

        // Destination Shells:
        List<AssetAdministrationShell> destinationShells = lookupDestinationShells(
                this.aasRepository,
                sourceSubmodel.getId(),
                transformer.getDestination().getAasDestination()
        );

        // Set context for template rendering
        Map<String, Object> context = templateRenderer.getTemplateContext(
                this.transformer.getId(),
                destinationShells,
                sourceSubmodel
        );

        try {
            // Create or get destination submodel:
            Submodel intermediateResult = createDestinationSubmodel(context);
            boolean isFirstAction = true;

            Instant startTransformation = Instant.now();
            // Execute transformer actions
            for (var transformerActionService : this.transformerActionServices) {
                intermediateResult = transformerActionService.execute(
                        sourceSubmodel,
                        intermediateResult,
                        context,
                        isFirstAction
                );
                isFirstAction = false;
            }
            Instant endTransformation = Instant.now();

            // Set ID and IdShort for destination submodel:
            var destinationSubmodelId = this.templateRenderer.render(
                    this.transformer.getDestination().getSubmodelDestination().getId(),
                    context
            );
            var destinationSubmodelIdShort = this.templateRenderer.render(
                    this.transformer.getDestination().getSubmodelDestination().getIdShort(),
                    context
            );
            intermediateResult.setId(destinationSubmodelId);
            intermediateResult.setIdShort(destinationSubmodelIdShort);

            Instant startSaveDestination = Instant.now();
            // Write intermediate result to Submodel Repository
            this.submodelRepository.createOrUpdateSubmodel(intermediateResult);

            addSubmodelReferenceToShells(sourceSubmodel.getId(), intermediateResult, context);
            Instant endSaveDestination = Instant.now();

            logTransformation(
                    destinationShells.get(0).getId(),
                    destinationSubmodelId,
                    sourceSubmodel.getId(),
                    executorId,
                    Duration.between(startTransformation, endTransformation),
                    Duration.between(startLookupSource, endLookupSource),
                    Duration.between(startSaveDestination, endSaveDestination)
            );
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void logTransformation(
            String destinationShellId,
            String destinationSubmodelId,
            String sourceSubmodelId,
            UUID executorId,
            Duration transformationDuration,
            Duration lookupSourceDuration,
            Duration saveDestinationDuration
    ) {
        TransformationLog transformationLog = new TransformationLog(
                destinationShellId,
                destinationSubmodelId,
                sourceSubmodelId,
                executorId,
                transformationDuration.toMillis(),
                lookupSourceDuration.toMillis(),
                saveDestinationDuration.toMillis()
        );

        LOG.info("Send transformation log to metrics service | {}", transformationLog.toString());

        metricsClient.addTransformationLog(transformationLog).block();
    }

    private void addSubmodelReferenceToShells(
            String sourceSubmodelId,
            Submodel intermediateResult,
            Map<String, Object> context
    ) {
        // Add reference to new submodel in destination AAS
        if (transformer.getDestination().getAasDestination() != null) {
            try {
                addReferenceToNewSubmodelInDestinationAas(
                        intermediateResult,
                        context
                );
            } catch (ApiException e) {
                LOG.error("Could not add reference to new submodel in destination AAS | {}", e.getMessage());
            }
            // Add reference to new submodel in source AAS
        } else {
            addReferenceToNewSubmodelInOriginAAS(
                    sourceSubmodelId,
                    intermediateResult
            );
        }
    }

    private void addReferenceToNewSubmodelInOriginAAS(String sourceSubmodelId, Submodel destinationSubmodel) {
        String destinationSubmodelId = destinationSubmodel.getId();
        var destinationSubmodelDescriptorOptional = this.submodelRegistry.findSubmodelDescriptor(destinationSubmodelId);
        this.aasRepository.getAllAasContainingSubmodelBySubmodelId(sourceSubmodelId)
                .stream()
                .forEach(shell -> {
                    try {
                        this.aasRegistry.addSubmodelDescriptorToAas(
                                shell.getId(),
                                destinationSubmodelDescriptorOptional.get()
                        );
                    } catch (ApiException e) {
                        throw new RuntimeException(e);
                    }
                    this.aasRepository.addSubmodelReferenceToAas(
                            shell.getId(),
                            this.submodelRepository.getSubmodel(destinationSubmodelId)
                    );
                });
    }

    private void addReferenceToNewSubmodelInDestinationAas(
            Submodel destinationSubmodel,
            Map<String, Object> context
    ) throws ApiException {
        var destinationAasId = this.templateRenderer.render(
                transformer.getDestination().getAasDestination().getId(),
                context
        );
        // Check if destination AAS exists
        var destinationAASDescriptorOptional = this.aasRegistry.getAasDescriptor(destinationAasId);

        // Create destination AAS if it does not exist:
        if (destinationAASDescriptorOptional.isEmpty()) {
            var newDestinationAAS = new DefaultAssetAdministrationShell.Builder()
                    .id(destinationAasId)
                    .build();
            this.aasRepository.createOrUpdateAas(newDestinationAAS);
        }

        // Add submodel descriptor to destination AAS
        var destinationAas = this.aasRepository.getAas(destinationAasId);
        var destinationSubmodelDescriptorOptional = this.submodelRegistry.findSubmodelDescriptor(
                destinationSubmodel.getId()
        );
        this.aasRegistry.addSubmodelDescriptorToAas(
                destinationAas.getId(),
                destinationSubmodelDescriptorOptional.get()
        );
        this.aasRepository.addSubmodelReferenceToAas(
                destinationAas.getId(),
                destinationSubmodel
        );
    }

    private Submodel createDestinationSubmodel(Map<String, Object> context) {
        // Create ID and IdShort for destination submodel:
        var destinationSubmodelId = this.templateRenderer.render(
                this.transformer.getDestination().getSubmodelDestination().getId(),
                context
        );
        var destinationSubmodelIdShort = this.templateRenderer.render(
                this.transformer.getDestination().getSubmodelDestination().getIdShort(),
                context
        );

        // Create new destination submodel :
        var newDestinationSubmodel = new DefaultSubmodel();
        newDestinationSubmodel.setIdShort(destinationSubmodelIdShort);
        newDestinationSubmodel.setId(destinationSubmodelId);

        return newDestinationSubmodel;
    }
}
