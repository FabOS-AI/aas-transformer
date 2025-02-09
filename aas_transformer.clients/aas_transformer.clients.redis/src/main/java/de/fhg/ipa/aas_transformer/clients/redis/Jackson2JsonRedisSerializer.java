package de.fhg.ipa.aas_transformer.clients.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Jackson2JsonRedisSerializer implements RedisSerializer<RedisTransformationJob> {

    private final ObjectMapper objectMapper;

    public Jackson2JsonRedisSerializer() {
        this.objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Submodel.class, new SubmodelDeserializer());
        objectMapper.registerModule(module);
    }

    @Override
    public byte[] serialize(RedisTransformationJob job) throws SerializationException {
        return job.toString().getBytes(StandardCharsets.UTF_8);

    }

    @Override
    public RedisTransformationJob deserialize(byte[] bytes) throws SerializationException {
        try {
            return bytes == null ? null : objectMapper.readValue(bytes, RedisTransformationJob.class);
        } catch (IOException e) {
            throw new SerializationException("Could not deserialize: " + bytes, e);
        }
    }
}
