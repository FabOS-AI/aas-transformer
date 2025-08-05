package de.fhg.ipa.aas_transformer.clients.redis;

import de.fhg.ipa.aas_transformer.clients.redis.serializer.MessageEvent2RedisSerializer;
import de.fhg.ipa.aas_transformer.model.message_event.MessageEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.UUID;

public class RedisMessageEventClient {
//    private final static String REDIS_MESSAGE_QUEUE_KEY_PREFIX = "message_queue";
//    private final static UUID CONSUMER_ID = UUID.randomUUID();
    private final static String REDIS_MESSAGE_QUEUE_KEY = "mqtt_message_queue";

    protected final RedisConnectionFactory redisConnectionFactory;
    private final ListOperations<String, MessageEvent> listOps;
    private final RedisTemplate<String, MessageEvent> template = new RedisTemplate<>();

    public RedisMessageEventClient(RedisConnectionFactory connectionFactory) {
        this.redisConnectionFactory = connectionFactory;
        this.listOps = template.opsForList();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new MessageEvent2RedisSerializer());
        template.afterPropertiesSet();
    }

    public boolean isConnected() {
        return redisConnectionFactory.getConnection().ping().toLowerCase().equals("pong");
    }

    protected void leftPush(MessageEvent messageEvent) {
        listOps.leftPush(REDIS_MESSAGE_QUEUE_KEY, messageEvent);
    }

    protected MessageEvent rightPop() {
        return listOps.rightPop(REDIS_MESSAGE_QUEUE_KEY);
    }

    public int getMessageEventCount() {
        return listOps.size(REDIS_MESSAGE_QUEUE_KEY).intValue();
    }
}
