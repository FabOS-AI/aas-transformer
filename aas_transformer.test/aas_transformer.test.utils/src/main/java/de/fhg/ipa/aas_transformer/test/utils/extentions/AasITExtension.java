package de.fhg.ipa.aas_transformer.test.utils.extentions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.*;

import static de.fhg.ipa.aas_transformer.test.utils.extentions.AbstractAasInfraExtension.*;

public class AasITExtension extends AbstractExtension implements BeforeAllCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(AasITExtension.class);

    protected GenericContainer mqttBrokerContainer;
    protected GenericContainer mqttExporterContainer;
    protected GenericContainer mqttGuiContainer;
    protected GenericContainer aasGuiContainer;
    protected GenericContainer aasRegistryContainer;
    protected GenericContainer smRegistryContainer;
    protected GenericContainer aasEnvContainer;
    protected GenericContainer aasEnvMongoDbContainer;
    protected List<GenericContainer> runningContainers = new ArrayList<>();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (runningContainers.size() == 0) {

            mqttBrokerContainer = getMqttBrokerContainer();
            mqttExporterContainer = getMqttExporterContainer();
            mqttGuiContainer = getMqttGuiContainer();
            mqttExporterContainer.setPortBindings(List.of("9234:9234"));

            aasRegistryContainer = getAasRegistryContainer();
            smRegistryContainer = getSmRegistryContainer();
            aasEnvContainer = getAasEnvContainer("");
            aasEnvMongoDbContainer = getAasEnvMongoDbContainer();

            // Start AAS backend containers
            List<GenericContainer> containers = new ArrayList<>();
            containers.add(aasEnvContainer);
            containers.add(mqttBrokerContainer);
            containers.add(mqttGuiContainer);
            containers.add(mqttExporterContainer);
            containers.add(aasEnvMongoDbContainer);
            containers.add(aasRegistryContainer);
            containers.add(smRegistryContainer);
            startContainer(containers);

            runningContainers.add(aasEnvContainer);
            runningContainers.add(mqttBrokerContainer);
            runningContainers.add(mqttGuiContainer);
            runningContainers.add(mqttExporterContainer);
            runningContainers.add(aasEnvMongoDbContainer);
            runningContainers.add(aasRegistryContainer);
            runningContainers.add(smRegistryContainer);

            Integer aasRegistryPort = aasRegistryContainer.getMappedPort(8080);
            Integer submodelRegistryPort = smRegistryContainer.getMappedPort(8080);
            Integer aasEnvPort = aasEnvContainer.getMappedPort(8081);
            Integer aasEnvMongoDbPort = aasEnvMongoDbContainer.getMappedPort(27017);
            Integer mqttBrokerPort = mqttBrokerContainer.getMappedPort(1883);
            Integer mqttBrokerWsPort = mqttBrokerContainer.getMappedPort(9001);
            Integer mqttGuiPort = mqttGuiContainer.getMappedPort(80);

            // Start AAS GUI container
            aasGuiContainer = getAasGuiContainer(aasRegistryPort, submodelRegistryPort, aasEnvPort);
            aasGuiContainer.start();
            runningContainers.add(aasGuiContainer);

            var aasGuiPort = aasGuiContainer.getMappedPort(3000);

            setSystemProperties(aasRegistryPort, submodelRegistryPort, aasEnvPort, mqttBrokerPort);
            printUrls(aasRegistryPort, submodelRegistryPort, aasEnvPort, aasEnvMongoDbPort, mqttBrokerPort, mqttBrokerWsPort, mqttGuiPort, aasGuiPort);

            super.beforeAll(context);
        }
    }

    private void setSystemProperties(
            Integer aasRegistryPort,
            Integer submodelRegistryPort,
            Integer aasEnvPort,
            Integer mqttBrokerPort
    ) {
        System.setProperty("aas.broker.host", HOSTNAME);
        System.setProperty("aas.broker.port", mqttBrokerPort.toString());
        System.setProperty("aas.aas-registry.port", aasRegistryPort.toString());
        System.setProperty("aas.submodel-registry.port", submodelRegistryPort.toString());
        System.setProperty("aas.aas-registry.url", "http://" + HOSTNAME + ":" + aasRegistryPort);
        System.setProperty("aas.submodel-registry.url", "http://" + HOSTNAME + ":" + submodelRegistryPort);
        System.setProperty("aas.aas-repository.port", String.valueOf(aasEnvPort));
        System.setProperty("aas.aas-repository.path", "");
        System.setProperty("aas.aas-repository.url", "http://" + HOSTNAME + ":" + String.valueOf(aasEnvPort));
        System.setProperty("aas.submodel-repository.port", String.valueOf(aasEnvPort));
        System.setProperty("aas.submodel-repository.path", "");
        System.setProperty("aas.submodel-repository.url", "http://" + HOSTNAME + ":" + String.valueOf(aasEnvPort));
    }

    private void printUrls(
            Integer aasRegistryPort,
            Integer submodelRegistryPort,
            Integer aasEnvPort,
            Integer aasEnvMongoDbPort,
            Integer mqttBrokerPort,
            Integer mqttBrokerWsPort,
            Integer mqttGuiPort,
            Integer aasGuiPort
    ) {
        System.out.println("AAS Registry: http://" + HOSTNAME + ":" + aasRegistryPort);
        System.out.println("Submodel Registry: http://" + HOSTNAME + ":" + submodelRegistryPort);
        System.out.println("AAS Environment: http://" + HOSTNAME + ":" + aasEnvPort);
        System.out.println("AAS Env Db: http://" + HOSTNAME + ":" + aasEnvMongoDbPort);
        System.out.println("MQTT Broker: tcp://" + HOSTNAME + ":" + mqttBrokerPort);
        System.out.println("MQTT WS Broker: ws://" + HOSTNAME + ":" + mqttBrokerWsPort);
        System.out.println("MQTT GUI: http://" + HOSTNAME + ":" + mqttGuiPort);
        System.out.println("AAS GUI: http://" + HOSTNAME + ":" + aasGuiPort);
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
