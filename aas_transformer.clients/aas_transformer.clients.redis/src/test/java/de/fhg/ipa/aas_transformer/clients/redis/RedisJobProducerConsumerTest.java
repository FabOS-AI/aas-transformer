package de.fhg.ipa.aas_transformer.clients.redis;

import com.redis.testcontainers.RedisContainer;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJobAction;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RedisJobProducerConsumerTest {

    @Autowired
    RedisJobProducer redisJobProducer;
    @Autowired
    RedisJobConsumer redisJobConsumer;

    // Test Objects:
    static UUID transformerId = UUID.randomUUID();
    RedisTransformationJob redisJob;

    @Autowired
    RedisContainer redisContainer;

    public RedisJobProducerConsumerTest() throws IOException, DeserializationException, SerializationException {
        redisJob = new RedisTransformationJob(new TransformationJob(
                UUID.randomUUID(),
                TransformationJobAction.EXECUTE,
                transformerId,
                getSimpleSubmodel().getId(),
                null,
                "targetSubmodelId"
        ));
    }


    public static Submodel getSimpleSubmodel() throws IOException, DeserializationException {
        File simpleSubmodelFile = new File("src/test/resources/submodels/simple-submodel.json");
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        var submodel = jsonDeserializer.read(new FileInputStream(simpleSubmodelFile), Submodel.class);

        return submodel;
    }

    @Test
    @Order(10)
    public void testContextLoaded() {
        assertNotNull(redisJobProducer);
        assertNotNull(redisJobConsumer);
    }

    @Test
    @Order(20)
    public void getJobCountExpectZero() {
        int jobCount = redisJobProducer.getJobCountInt();
        assertEquals(0, jobCount);
    }

    @Test
    @Order(30)
    public void createJobExpectOneJob() {
        redisJobProducer.pushJob(this.redisJob.getTransformationJob());
        assertEquals(
                1,
                redisJobProducer.getJobCountInt()
        );
    }

    @Test
    @Order(40)
    public void popJobExpectJobBeingEqualToCreatedJob()  {
        RedisTransformationJob poppedJob = redisJobConsumer.moveJobInProcessingList().get();
        assertEquals(
            this.redisJob,
            poppedJob
        );
    }

    @Test
    @Order(60)
    public void getJobCountAfterPopExpectZero() {
        int jobCount = redisJobConsumer.getJobCountInt();
        assertEquals(0, jobCount);
    }

    @Test
    @Order(70)
    public void getProcJobCountAfterPopExpectOne() {
        int jobCount = redisJobConsumer.getProcJobCountInt();
        assertEquals(1, jobCount);
    }

    @Test
    @Order(80)
    public void markProcJobAsProcessedExpectZeroProcJobs() {
        redisJobConsumer.markJobAsFinished(this.redisJob.getTransformationJob());
        assertEquals(
                0,
                redisJobConsumer.getProcJobCountInt()
        );
    }
}

