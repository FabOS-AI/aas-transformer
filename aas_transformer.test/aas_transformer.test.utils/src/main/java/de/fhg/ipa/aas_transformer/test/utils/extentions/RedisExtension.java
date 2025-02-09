package de.fhg.ipa.aas_transformer.test.utils.extentions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RedisExtension extends AbstractExtension implements BeforeAllCallback, AfterAllCallback {

    protected Network containerNetwork = Network.newNetwork();
    protected GenericContainer redisContainer;
    protected GenericContainer redisInsightContainer;
    protected GenericContainer redisExporterContainer;
    protected List<GenericContainer> runningContainers = new ArrayList<>();

    Map<String, String> containerLabels = Map.of(
            "de.fhg.ipa.aas-transformer.container-group","transformer"
    );

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        if(runningContainers.size() == 0) {
            redisContainer = new GenericContainer("redis:7")
                    .withLabels(containerLabels)
                    .withNetwork(containerNetwork)
                    .withNetworkAliases("redis")
                    .withExposedPorts(6379);

            redisInsightContainer = new GenericContainer("redis/redisinsight:2.64")
                    .withLabels(containerLabels)
                    .withNetwork(containerNetwork)
                    .withNetworkAliases("redis-insight")
                    .withExposedPorts(5540)
                    .withNetwork(redisContainer.getNetwork());

            redisExporterContainer = new GenericContainer("bitnami/redis-exporter:1.67.0")
                    .withLabels(containerLabels)
                    .withExposedPorts(9121)
                    .withNetwork(containerNetwork)
                    .withNetworkAliases("redis-exporter")
//                    .withEnv("REDIS_ADDR", "redis://"+redisContainer.getNetworkAliases().get(0)+":6379")
                    .withEnv("REDIS_ADDR", "redis://redis:6379")
                    .withEnv("REDIS_EXPORTER_CHECK_SINGLE_KEYS", "jobs")
                    .withEnv("REDIS_EXPORTER_COUNT_KEYS", "proc_jobs*");

            redisExporterContainer.setPortBindings(List.of("9121:9121"));

            List<GenericContainer> containers = List.of(
                    redisContainer,
                    redisInsightContainer,
                    redisExporterContainer
            );
            startContainer(containers);

            runningContainers.add(redisContainer);
            runningContainers.add(redisInsightContainer);
            runningContainers.add(redisExporterContainer);
        }

        var redisPort = redisContainer.getMappedPort(6379);
        var redisInsightPort = redisInsightContainer.getMappedPort(5540);
        var redisExporterPort = redisExporterContainer.getMappedPort(9121);

        System.setProperty("spring.data.redis.host", "localhost");
        System.setProperty("spring.data.redis.port", redisPort.toString());
        System.setProperty("spring.data.redis.username", "");
        System.setProperty("spring.data.redis.password", "");

        System.out.println("Redis Address: http://host.docker.internal:" + redisPort);
        System.out.println("Redis Insight Address: http://localhost:" + redisInsightPort);
        System.out.println("Redis Exporter Address: http://localhost:" + redisExporterPort);

        super.beforeAll(extensionContext);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (runningContainers.size() > 0 && extensionContext.getParent().get().getParent().isEmpty()) {
            for (var container : runningContainers) {
                container.stop();
            }
            runningContainers = new ArrayList<>();
        }

        super.afterAll(extensionContext);
    }
}
