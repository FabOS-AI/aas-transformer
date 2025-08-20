package de.fhg.ipa.aas_transformer.clients.redis;

import de.fhg.ipa.aas_transformer.clients.redis.serializer.MessageEvent2RedisSerializer;
import de.fhg.ipa.aas_transformer.model.message_event.MessageEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RedisMessageEventClient {
    private final static String REDIS_MESSAGE_QUEUE_KEY = "mqtt_message_queue";

    protected final RedisConnectionFactory redisConnectionFactory;
    private final ListOperations<String, MessageEvent> listOps;

    public RedisMessageEventClient(RedisConnectionFactory connectionFactory) {
        this.redisConnectionFactory = connectionFactory;
        RedisTemplate<String, MessageEvent> template = new RedisTemplate<>();
        this.listOps = template.opsForList();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new MessageEvent2RedisSerializer());
        template.afterPropertiesSet();
    }

    public boolean isConnected() {
        return Objects.requireNonNull(redisConnectionFactory.getConnection().ping()).equalsIgnoreCase("pong");
    }

    protected void leftPush(MessageEvent messageEvent) {
        listOps.leftPush(REDIS_MESSAGE_QUEUE_KEY, messageEvent);
    }

    protected MessageEvent rightPop() {
        return listOps.rightPop(REDIS_MESSAGE_QUEUE_KEY);
    }

    public List<MessageEvent> getMessageEvents() {
        return listOps.range(REDIS_MESSAGE_QUEUE_KEY, 0, -1);
    }

    public int getMessageEventCount() {
        return Objects.requireNonNull(listOps.size(REDIS_MESSAGE_QUEUE_KEY)).intValue();
    }

    public void deleteMessageEvents() {
        listOps.trim(REDIS_MESSAGE_QUEUE_KEY, 0, -1);
    }
}
