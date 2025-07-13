package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.model.Destination;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TransformationUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TransformationUtils.class);

    private final AasRegistry aasRegistry;
    private final AasRepository aasRepository;
    private final TemplateRenderer templateRenderer;

    public TransformationUtils(
            AasRegistry aasRegistry,
            AasRepository aasRepository,
            TemplateRenderer templateRenderer
    ) {
        this.aasRegistry = aasRegistry;
        this.aasRepository = aasRepository;
        this.templateRenderer = templateRenderer;
    }

    public List<AssetAdministrationShell> lookupDestinationShells(
            UUID transformerId,
            Destination transformerDestination,
            Submodel sourceSubmodel
    ) {
        List<String> destinationShellIds = lookupDestinationShellIds(
                transformerId,
                transformerDestination,
                sourceSubmodel
        );
        List<AssetAdministrationShell> destinationShells = new ArrayList<>();
        for(String shellId: destinationShellIds) {
            AssetAdministrationShell destinationShell = AasRepository.getExtAas(aasRegistry, shellId);
            if(destinationShell == null) {
                aasRepository.createAasOrDoNothing(
                        new DefaultAssetAdministrationShell.Builder()
                                .id(shellId)
                                .build()
                );
                destinationShell = aasRepository.getAas(shellId);
            }

            destinationShells.add(destinationShell);
        }
        return destinationShells;
    }

    public List<String> lookupDestinationShellIds(
            UUID transformerId,
            Destination transformerDestination,
            Submodel sourceSubmodel
    ) {
        List<String> destinationShellIds;
        if(transformerDestination.getAasDestination() != null) {
            try {
                Map<String, Object> templateContext = templateRenderer.getTemplateContext(
                        transformerId,
                        List.of(),
                        sourceSubmodel
                );

                String destinationShellId = templateRenderer.render(
                        transformerDestination.getAasDestination().getId(),
                        templateContext
                );

                destinationShellIds = List.of(destinationShellId);
            } catch (Exception e) {
                LOG.warn("Failed to render destination shell ID based on template {}", transformerDestination.getAasDestination().getId());
                destinationShellIds = List.of();
            }
        } else {
            destinationShellIds = aasRepository
                    .getAllAasContainingSubmodelBySubmodelId(sourceSubmodel.getId())
                    .stream()
                    .map(shell -> shell.getId())
                    .toList();
        }
        return destinationShellIds;
    }
}
