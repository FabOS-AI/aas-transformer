package de.fhg.ipa.aas_transformer.clients.redis;

import com.redis.testcontainers.RedisContainer;
import de.fhg.ipa.aas_transformer.model.SubmodelChangeEventType;
import de.fhg.ipa.aas_transformer.model.message_event.MessageEvent;
import de.fhg.ipa.aas_transformer.model.message_event.SubmodelMessageEvent;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.GenericTestConfig.getSimpleSubmodel;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisMessageEventQueueTest {
    @Autowired
    RedisContainer redisContainer;
    @Autowired
    RedisMessageEventProducer redisMessageEventProducer;
    @Autowired
    RedisMessageEventConsumer redisMessageEventConsumer;

    @Test
    @Order(10)
    public void testGetMessageEventExpectNull() {
        MessageEvent event = redisMessageEventConsumer.popMessage();
        // Expect no message event
        assertNull(event);
    }

    @Test
    @Order(20)
    public void testGetMessageEventCountExpectZero() {
        long count = redisMessageEventConsumer.getMessageEventCount();
        // Expect no message event
        assertEquals(0, count);
    }

    @Test
    @Order(30)
    public void testPushMessageEventExpectCountOne() {
        redisMessageEventProducer.pushMessage(new SubmodelMessageEvent(
                SubmodelChangeEventType.CREATED,
                getSimpleSubmodel()
        ));

        // Expect one message event

        long count = redisMessageEventConsumer.getMessageEventCount();
        // Expect no message event
        assertEquals(1, count);
    }

    @Test
    @Order(40)
    public void testPopMessageEventExpectNotNullAndCountZero() {
        MessageEvent event = redisMessageEventConsumer.popMessage();
        // Expect one message event
        assertNotNull(event);
        assertEquals(SubmodelChangeEventType.CREATED, event.getSubmodelChangeEventType());
        assertEquals(getSimpleSubmodel(), ((SubmodelMessageEvent) event).getSubmodel());

        long count = redisMessageEventConsumer.getMessageEventCount();
        // Expect no message event
        assertEquals(0, count);
    }

    @Test
    @Order(50)
    public void testPushMultipleMessageEventsExpectPopByFifoAndCountMinusOne() {
        List<Submodel> submodels = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            // Create a submodel with a unique ID and ID short
            Submodel submodel = getSimpleSubmodel();
            submodel.setId(String.valueOf(i));
            submodel.setIdShort(String.valueOf(i));
            submodels.add(submodel);

            // Push the submodel as a message event
            redisMessageEventProducer.pushMessage(new SubmodelMessageEvent(
                    SubmodelChangeEventType.CREATED,
                    submodel
            ));
        }

        MessageEvent event = redisMessageEventConsumer.popMessage();
        // Expect the first submodel to be popped
        assertEquals(submodels.get(0), ((SubmodelMessageEvent) event).getSubmodel());

        // Check the count after popping one message event
        long count = redisMessageEventConsumer.getMessageEventCount();
        assertEquals(submodels.size()-1, count);
    }

    @Test
    @Order(60)
    public void testDeleteMessageEventsExpectCountZero() {
        redisMessageEventConsumer.deleteMessageEvents();
        long count = redisMessageEventConsumer.getMessageEventCount();
        // Expect no message event
        assertEquals(0, count);
    }
}
