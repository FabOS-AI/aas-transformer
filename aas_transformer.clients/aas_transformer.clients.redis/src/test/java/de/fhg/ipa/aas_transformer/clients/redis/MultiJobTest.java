package de.fhg.ipa.aas_transformer.clients.redis;

import com.redis.testcontainers.RedisContainer;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJobAction;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.UUID;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultiJobTest {
    private static final Logger LOG = LoggerFactory.getLogger(MultiJobTest.class);

    @Autowired
    RedisContainer redisContainer;
    @Autowired
    RedisJobProducer redisJobProducer;
    @Autowired
    RedisJobConsumer redisJobConsumer;

    private void assertListCounts(int expectedJobCount, int expectedProcJobCount) throws InterruptedException {
        for(int i = 0; i < 10; i++) {
            int currentJobCount = redisJobConsumer.getJobCountInt();
            int currentProcJobCount = redisJobConsumer.getProcJobCountInt();

            LOG.info("CurrentJobCount: {} | CurrentProcJobCount: {}", currentJobCount, currentProcJobCount);
            try {
                assertEquals(
                        expectedJobCount,
                        redisJobConsumer.getJobCountInt()
                );

                assertEquals(
                        expectedProcJobCount,
                        redisJobConsumer.getProcJobCountInt()
                );
                break;
            } catch(AssertionError e) {
                sleep(10);
            }
        }
        assertEquals(expectedJobCount, redisJobConsumer.getJobCountInt());
        assertEquals(expectedProcJobCount, redisJobConsumer.getProcJobCountInt());
    }

    @Test
    @Order(01)
    public void testContextLoaded() {
        assertNotNull(redisJobProducer);
        assertNotNull(redisJobConsumer);
    }

    @Test
    @Order(10)
    public void testMultiJobProduceConsumeExpectNoLeftoverJobs() throws InterruptedException {
        int jobCount = 10;

        for(int i = 0; i < jobCount; i++) {
            LOG.info("Pushing job {}", i);
            String sourceSubmodelId = "submodelId"+i;
            String targetSubmodelId = "targetSubmodelId"+i;

            TransformationJob job = new TransformationJob(
                    UUID.randomUUID(),
                    TransformationJobAction.EXECUTE,
                    UUID.randomUUID(),
                    sourceSubmodelId,
                    null,
                    targetSubmodelId
            );


            redisJobProducer.pushJob(job);
            redisJobConsumer.moveJobInProcessingList();
            assertListCounts(0,1);

            redisJobConsumer.markJobAsFinished(job);
            assertListCounts(0,0);
        }
    }

    @Test
    @Order(20)
    public void testJobDeleteExpectNoLeftoverJobs() throws InterruptedException, SerializationException {
        int jobCount = 10;

        for(int i = 0; i < jobCount; i++) {
            LOG.info("Pushing job {}", i);
            String sourceSubmodelId = "submodelId"+i;
            String targetSubmodelId = "targetSubmodelId"+i;

            TransformationJob job = new TransformationJob(
                    UUID.randomUUID(),
                    TransformationJobAction.EXECUTE,
                    UUID.randomUUID(),
                    sourceSubmodelId,
                    null,
                    targetSubmodelId
            );

            redisJobProducer.pushJob(job);
        }

        assertListCounts(jobCount, 0);

        redisJobConsumer.deleteWaitingJobs();
        assertListCounts(0, 0);
    }
}
