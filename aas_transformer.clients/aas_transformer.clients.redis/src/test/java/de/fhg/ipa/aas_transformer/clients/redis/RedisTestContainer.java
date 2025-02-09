package de.fhg.ipa.aas_transformer.clients.redis;

import com.redis.testcontainers.RedisContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.utility.DockerImageName;

@SpringBootApplication
public class RedisTestContainer {
    @Bean
    @ServiceConnection(name = "redis")
    static RedisContainer redisContainer() {
        return new RedisContainer(DockerImageName.parse("redis:7"));
    }
}
