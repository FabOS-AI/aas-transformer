package de.fhg.ipa.aas_transformer.test.utils;

import de.fhg.ipa.aas_transformer.clients.redis.RedisJobReader;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RedisTestObjects {


    public static void assertExpectedJobCount(
            RedisJobReader redisJobReader,
            int expectedJobCount
    ) throws InterruptedException {
        int tryCount = 0;
        int maxTries = 1000;
        int sleepInMs = 100;

        while(tryCount <= maxTries && redisJobReader.getTotalJobCount() != expectedJobCount) {
            sleep(sleepInMs);
            tryCount++;
        }

        assertEquals(
                expectedJobCount,
                redisJobReader.getTotalJobCount()
        );
    }
}
