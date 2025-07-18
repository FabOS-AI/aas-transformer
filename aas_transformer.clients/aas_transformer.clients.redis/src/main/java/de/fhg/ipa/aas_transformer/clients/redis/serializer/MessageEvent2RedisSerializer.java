package de.fhg.ipa.aas_transformer.clients.redis.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.fhg.ipa.aas_transformer.aas.deserializer.SubmodelDeserializer;
import de.fhg.ipa.aas_transformer.aas.serializer.SubmodelSerializer;
import de.fhg.ipa.aas_transformer.model.message_event.MessageEvent;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MessageEvent2RedisSerializer implements RedisSerializer<MessageEvent> {

    private final ObjectMapper objectMapper;

    public MessageEvent2RedisSerializer() {
        this.objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Submodel.class, new SubmodelDeserializer());
        module.addSerializer(Submodel.class, new SubmodelSerializer());
        objectMapper.registerModule(module);
    }

    @Override
    public byte[] serialize(MessageEvent messageEvent) throws SerializationException {
        try {
            String eventAsString = objectMapper.writeValueAsString(messageEvent);

            return eventAsString.getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public MessageEvent deserialize(byte[] bytes) throws SerializationException {
        try {
            return bytes == null ? null : objectMapper.readValue(bytes, MessageEvent.class);
        } catch (IOException e) {
            throw new SerializationException("Could not deserialize: " + bytes, e);
        }
    }
}
