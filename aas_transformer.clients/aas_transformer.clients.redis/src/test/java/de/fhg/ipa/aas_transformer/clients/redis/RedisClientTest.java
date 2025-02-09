package de.fhg.ipa.aas_transformer.clients.redis;

import com.redis.testcontainers.RedisContainer;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJobAction;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisClientTest {

    RedisClient redisClient;

    // Test Objects:
    static UUID transformerId = UUID.randomUUID();
    RedisTransformationJob redisJob;

    @Autowired
    RedisContainer redisContainer;

    public RedisClientTest() throws IOException, DeserializationException, SerializationException {
        redisJob = new RedisTransformationJob(new TransformationJob(
                TransformationJobAction.EXECUTE,
                transformerId,
                getSimpleSubmodel().getId(),
                null
        ));
    }


    public static Submodel getSimpleSubmodel() throws IOException, DeserializationException {
        File simpleSubmodelFile = new File("src/test/resources/submodels/simple-submodel.json");
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        var submodel = jsonDeserializer.read(new FileInputStream(simpleSubmodelFile), Submodel.class);

        return submodel;
    }

    @PostConstruct
    public void init() {
        Integer port = redisContainer.getFirstMappedPort();
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", port);
        connectionFactory.start();
        redisClient = new RedisClient(connectionFactory);
    }

    @Test
    @Order(10)
    public void testContextLoaded() {
        assertNotNull(redisClient);
    }

    @Test
    @Order(20)
    public void getJobCountExpectZero() {
        int jobCount = redisClient.getJobCountInt();
        assertEquals(0, jobCount);
    }

    @Test
    @Order(30)
    public void createJobExpectOneJob() {
        redisClient.leftPushJob(this.redisJob);
        assertEquals(
                1,
                redisClient.getJobCountInt()
        );
    }

    @Test
    @Order(40)
    public void popJobExpectJobBeingEqualToCreatedJob()  {
        RedisTransformationJob poppedJob = redisClient.moveJobInProcessingList().get();
        assertEquals(
            this.redisJob,
            poppedJob
        );
    }

    @Test
    @Order(60)
    public void getJobCountAfterPopExpectZero() {
        int jobCount = redisClient.getJobCountInt();
        assertEquals(0, jobCount);
    }

    @Test
    @Order(70)
    public void getProcJobCountAfterPopExpectOne() {
        int jobCount = redisClient.getProcJobCountInt();
        assertEquals(1, jobCount);
    }

    @Test
    @Order(80)
    public void markProcJobAsProcessedExpectZeroProcJobs() throws SerializationException {
        redisClient.markJobAsProcessed(this.redisJob);
        assertEquals(
                0,
                redisClient.getProcJobCountInt()
        );
    }
}

