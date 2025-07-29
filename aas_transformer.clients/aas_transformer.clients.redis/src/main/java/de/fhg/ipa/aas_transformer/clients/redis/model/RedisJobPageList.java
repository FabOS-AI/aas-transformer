package de.fhg.ipa.aas_transformer.clients.redis.model;

import de.fhg.ipa.aas_transformer.clients.redis.RedisJobClient;

import java.util.ArrayList;

public class RedisJobPageList extends ArrayList<RedisJobPage> {
    private final int pageSize = 10; // Default page size; count of jobs per page
    public RedisJobPageList(RedisJobClient redisJobClient) {
        int start = 0;
        int totalJobCount = redisJobClient.getJobCountInt();
        int pageCount = (int) Math.ceil((double) totalJobCount / pageSize);

        for (int i = 0; i < pageCount; i++) {
            this.add(new RedisJobPage(
                    i*pageSize,
                    Math.min(start + pageSize - 1, totalJobCount - 1)
            ));
        }
    }
}
