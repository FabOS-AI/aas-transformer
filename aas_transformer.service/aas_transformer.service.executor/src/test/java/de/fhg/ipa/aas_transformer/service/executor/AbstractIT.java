package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.redis.RedisClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobReader;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.eclipse.digitaltwin.basyx.aasregistry.client.model.AssetAdministrationShellDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractIT {
    // Service Ports:
    static String aasRegistryPort = System.getProperty("aas.aas-registry.port");
    static String aasRepositoryPort = System.getProperty("aas.aas-repository.port");
    static String smRegistryPort = System.getProperty("aas.submodel-registry.port");
    static String smRepositoryPort = System.getProperty("aas.submodel-repository.port");
    String redisPort = System.getProperty("spring.data.redis.port");

    // AAS Service Clients:
    static AasRegistry aasRegistry;
    static AasRepository aasRepository;
    static SubmodelRegistry smRegistry;
    static SubmodelRepository smRepository;

    // Transformer Objects:
    @Autowired
    TransformationExecutionServiceCache transformationExecutionServiceCache;

    @Autowired
    RedisJobReader redisJobReader;

    RedisClient redisClient;

    static {
        // Init AAS Clients:
        aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort);
        aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort);
        smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort);
        smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort);
    }

    @PostConstruct
    public void init() {
        // Setup Redis Client:
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", Integer.parseInt(redisPort));
        connectionFactory.start();
        redisClient = new RedisClient(connectionFactory);
    }
}
