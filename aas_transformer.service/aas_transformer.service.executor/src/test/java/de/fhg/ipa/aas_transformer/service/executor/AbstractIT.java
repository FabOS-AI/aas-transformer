package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractIT {
    // Service Ports:
    static String aasRegistryPort = System.getProperty("aas.aas-registry.port");
    static String aasRepositoryPort = System.getProperty("aas.aas-repository.port");
    static String smRegistryPort = System.getProperty("aas.submodel-registry.port");
    static String smRepositoryPort = System.getProperty("aas.submodel-repository.port");

    // AAS Service Clients:
    static AasRegistry aasRegistry;
    static AasRepository aasRepository;
    static SubmodelRegistry smRegistry;
    static SubmodelRepository smRepository;

    // Transformer Objects:
    @Autowired
    TransformationExecutionServiceCache transformationExecutionServiceCache;

    static {
        // Init AAS Clients:
        aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);
    }

}
