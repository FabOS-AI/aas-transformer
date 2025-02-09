package de.fhg.ipa.aas_transformer.clients.redis;

import de.fhg.ipa.aas_transformer.model.TransformationJob;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static de.fhg.ipa.aas_transformer.model.TransformationJobAction.EXECUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JobProducerStrictNonStrictTest {

    @Autowired
    RedisJobProducer redisJobProducer;

    public static TransformationJob jobNonStrict = new TransformationJob(
            EXECUTE,
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            null
    );

    public static TransformationJob jobStrict = new TransformationJob(
            EXECUTE,
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            new DefaultSubmodel()
    );

    @Test
    @Order(10)
    public void testNonStrictJobPush() throws SerializationException {
        assertEquals(
                0,
                redisJobProducer.getJobCountInt()
        );

        for(int i = 0; i < 2; i++) {
            // Push job without submodel to redis
            redisJobProducer.pushJob(jobNonStrict);

            // Assert job count stays at 1
            assertEquals(
                    1,
                    redisJobProducer.getJobCountInt()
            );
        }
    }

    @Test
    @Order(20)
    public void testStrictJobPush() throws SerializationException {
        int initialJobCount = redisJobProducer.getJobCountInt();

        for(int i = 0; i < 5; i++) {
            // Push job with submodel to redis
            redisJobProducer.pushJob(jobStrict);

            // Assert job count raises even jobs are the same
            assertEquals(
                    initialJobCount+1+i,
                    redisJobProducer.getJobCountInt()
            );
        }

    }
}
