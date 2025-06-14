package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.model.DestinationAAS;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            String sourceSubmodelId,
            DestinationAAS destinationAas,
            Map<String, Object> templateContext
    ) {
        List<String> destinationShellIds = lookupDestinationShellIds(
                sourceSubmodelId,
                destinationAas,
                templateContext
        );
        List<AssetAdministrationShell> destinationShells = new ArrayList<>();
        for(String shellId: destinationShellIds) {
            AssetAdministrationShell destinationShell = aasRepository.getExtAas(aasRegistry, shellId);
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
            String sourceSubmodelId,
            DestinationAAS destinationAas,
            Map<String, Object> templateContext
    ) {
        List<String> destinationShellIds;
        if(destinationAas != null) {
            try {
                String destinationShellId = templateRenderer.render(
                        destinationAas.getId(),
                        templateContext
                );
                destinationShellIds = List.of(destinationShellId);
            } catch (Exception e) {
                LOG.warn("Failed to render destination shell ID based on template {}", destinationAas.getId());
                destinationShellIds = List.of();
            }
        } else {
            destinationShellIds = aasRepository
                    .getAllAasContainingSubmodelBySubmodelId(sourceSubmodelId)
                    .stream()
                    .map(shell -> shell.getId())
                    .toList();
        }
        return destinationShellIds;
    }
}
