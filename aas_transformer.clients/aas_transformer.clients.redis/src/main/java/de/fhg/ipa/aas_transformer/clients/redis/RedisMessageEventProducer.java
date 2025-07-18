package de.fhg.ipa.aas_transformer.clients.redis;

import de.fhg.ipa.aas_transformer.model.message_event.MessageEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisMessageEventProducer extends RedisMessageEventClient {
    public RedisMessageEventProducer(RedisConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public void pushMessage(MessageEvent messageEvent) {
        leftPush(messageEvent);
    }
}
