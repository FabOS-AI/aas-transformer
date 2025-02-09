package de.fhg.ipa.aas_transformer.test.utils.extentions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.ArrayList;
import java.util.List;

public class MqttExtension extends SpringExtension implements BeforeAllCallback, AfterAllCallback {

    protected Network containerNetwork = Network.newNetwork();
    protected GenericContainer mqttBrokerContainer;
    protected GenericContainer mqttExporterContainer;
    protected GenericContainer mqttGuiContainer;
    protected List<GenericContainer> runningContainers = new ArrayList<>();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (runningContainers.size() == 0) {
            mqttBrokerContainer = new GenericContainer<>(
                    DockerImageName.parse("eclipse-mosquitto:2.0.20"))
                    .withNetwork(containerNetwork)
                    .withNetworkAliases("mqtt")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("mosquitto.conf"),
                            "/mosquitto/config/mosquitto.conf"
                    )
                    .dependsOn()
                    .withExposedPorts(1883, 9001);

            mqttExporterContainer = new GenericContainer(
                    DockerImageName.parse("jryberg/mosquitto-exporter:v0.7.4"))
                    .withNetwork(containerNetwork)
                    .withNetworkAliases("mqtt-exporter")
                    .withCommand("--endpoint tcp://mqtt:1883")
                    .withExposedPorts(9234);


            mqttGuiContainer = new GenericContainer(
                    DockerImageName.parse("emqx/mqttx-web"))
                    .withNetwork(containerNetwork)
                    .withNetworkAliases("mqtt-gui")
                    .withExposedPorts(80);

            mqttExporterContainer.setPortBindings(List.of("9234:9234"));

            // Start AAS backend containers
            mqttBrokerContainer.start();
            mqttGuiContainer.start();
            mqttExporterContainer.start();

            runningContainers.add(mqttBrokerContainer);
            runningContainers.add(mqttGuiContainer);
            runningContainers.add(mqttExporterContainer);

            var mqttBrokerPort = mqttBrokerContainer.getMappedPort(1883);

            System.setProperty("aas.broker.host", "localhost");
            System.setProperty("aas.broker.port", mqttBrokerPort.toString());
            super.beforeAll(context);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (runningContainers.size() > 0 && context.getParent().get().getParent().isEmpty()) {
            for (var container : runningContainers) {
                container.stop();
            }
            runningContainers = new ArrayList<>();
        }

        super.afterAll(context);
    }
}
