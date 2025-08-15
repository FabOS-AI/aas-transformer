package de.fhg.ipa.aas_transformer.test.system.performance.executor;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJobAction;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.utils.creator.HistoricDataCreator;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

public class HistoricDataJobCreator extends HistoricDataCreator {

    private static final String REDIS_HOST = "aas-transformer.local";
    private static final int REDIS_PORT = 1883;
    private final Transformer transformer;

    RedisJobProducer jobProducer;

    public HistoricDataJobCreator(
            AasRegistry aasRegistry,
            AasRepository aasRepository,
            SubmodelRegistry smRegistry,
            SubmodelRepository smRepository,
            int submodelCount,
            int sleepInMs,
            Transformer transformer
    ) {
        super(aasRegistry, aasRepository, smRegistry, smRepository, submodelCount, sleepInMs);
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(REDIS_HOST, REDIS_PORT);
        connectionFactory.start();
        this.jobProducer = new RedisJobProducer(connectionFactory);
        this.transformer = transformer;
    }

    @Override
    protected Submodel createShellWithTimeseriesSubmodel() {
        Submodel s = super.createShellWithTimeseriesSubmodel();
        createJob(s);
        return s;
    }

    private void createJob(Submodel s) {
        createJob(s.getId(), s,s.getId()+"_avg");
    }

    private void createJob(String submodelId, @Nullable Submodel submodel, String targetSubmodelId) {
        // Create a job for the given submodel ID
        System.out.println("Creating job for submodel: " + submodelId + " with transformer: " + transformer.getId());
        jobProducer.pushJob(new TransformationJob(
                UUID.randomUUID(),
                TransformationJobAction.EXECUTE,
                transformer.getId(),
                submodelId,
                submodel,
                targetSubmodelId
        ));
    }
}
