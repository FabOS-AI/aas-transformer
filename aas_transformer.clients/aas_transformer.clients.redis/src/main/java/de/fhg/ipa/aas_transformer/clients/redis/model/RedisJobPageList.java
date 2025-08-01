package de.fhg.ipa.aas_transformer.clients.redis.model;

import de.fhg.ipa.aas_transformer.clients.redis.RedisJobClient;

import java.util.ArrayList;

public class RedisJobPageList extends ArrayList<RedisJobPage> {
    private final int PAGE_SIZE_MIN = 10; // Default page size; count of jobs per page
    private final int PAGE_SIZE_MAX = 200; // Default page size; count of jobs per page
    private final RedisJobClient jobClient;

    public RedisJobPageList(RedisJobClient redisJobClient) {
        this.jobClient = redisJobClient;
        int totalJobCount = redisJobClient.getJobCountInt();
        int pageSize = getPageSize();
        int pageCount = (int) Math.ceil((double) totalJobCount / pageSize);

        for (int i = 0; i < pageCount; i++) {
            int startIndex = i * pageSize;
            int endIndex = Math.min(startIndex + pageSize - 1, totalJobCount - 1);
            this.add(new RedisJobPage(startIndex, endIndex));
        }
    }

    private int getPageSize() {
        int pageSize = (int) Math.ceil(jobClient.getJobCountInt()*0.1);
        if (pageSize < PAGE_SIZE_MIN) {
            pageSize = PAGE_SIZE_MIN;
        } else if (pageSize > PAGE_SIZE_MAX) {
            pageSize = PAGE_SIZE_MAX;
        }
        return pageSize;
    }
}
