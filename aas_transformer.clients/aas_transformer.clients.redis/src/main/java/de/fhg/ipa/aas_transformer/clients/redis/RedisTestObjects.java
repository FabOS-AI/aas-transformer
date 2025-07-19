package de.fhg.ipa.aas_transformer.clients.redis;

import static java.lang.Thread.sleep;

public class RedisTestObjects {


    public static void assertExpectedJobCount(
            RedisJobReader redisJobReader,
            int expectedJobCount
    ) throws InterruptedException {
        int tryCount = 0;
        int maxTries = 10000;
        int sleepInMs = 100;

        while(tryCount <= maxTries && redisJobReader.getTotalJobCount() != expectedJobCount) {
            sleep(sleepInMs);
            tryCount++;
        }

        assert expectedJobCount == redisJobReader.getTotalJobCount();
    }
}
