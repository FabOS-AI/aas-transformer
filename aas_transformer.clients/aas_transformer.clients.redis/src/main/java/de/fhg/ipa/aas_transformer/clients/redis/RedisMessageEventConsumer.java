package de.fhg.ipa.aas_transformer.clients.redis;

import de.fhg.ipa.aas_transformer.model.message_event.MessageEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

@Component
public class RedisMessageEventConsumer extends RedisMessageEventClient {
    public RedisMessageEventConsumer(RedisConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Nullable
    public MessageEvent popMessage() {
        return rightPop();
    }
}
