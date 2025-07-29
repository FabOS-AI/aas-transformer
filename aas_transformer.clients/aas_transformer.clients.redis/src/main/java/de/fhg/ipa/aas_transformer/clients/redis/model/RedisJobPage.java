package de.fhg.ipa.aas_transformer.clients.redis.model;

public class RedisJobPage {
    private final int startIndex;
    private final int endIndex;

    public RedisJobPage(int startIndex, int endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }
}
