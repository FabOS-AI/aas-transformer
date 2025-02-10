package de.fhg.ipa.aas_transformer.test.utils.extentions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.util.ArrayList;
import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.extentions.AbstractAasInfraExtension.*;

public class AasITLbExtension extends AbstractExtension implements BeforeAllCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(AasITLbExtension.class);

    protected GenericContainer traefikContainer;
    protected GenericContainer mqttBrokerContainer;
    protected GenericContainer mqttExporterContainer;
    protected GenericContainer mqttGuiContainer;
    protected GenericContainer aasGuiContainer;
    protected GenericContainer aasRegistryContainer;
    protected GenericContainer smRegistryContainer;
    protected List<GenericContainer> aasEnvContainers;
    protected GenericContainer aasEnvMongoDbContainer;
    protected List<GenericContainer> runningContainers = new ArrayList<>();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (runningContainers.size() == 0) {
            traefikContainer = getTraefikContainer();
            Integer traefikHttpPort = traefikContainer.getMappedPort(80);
            Integer traefikHttpsPort = traefikContainer.getMappedPort(443);
            Integer traefikApiPort = traefikContainer.getMappedPort(8080);

            mqttBrokerContainer = getMqttBrokerContainer();
            mqttExporterContainer = getMqttExporterContainer();
            mqttGuiContainer = getMqttGuiContainer();
            mqttExporterContainer.setPortBindings(List.of("9234:9234"));

            aasRegistryContainer = getAasRegistryContainer();
            smRegistryContainer = getSmRegistryContainer();
            aasEnvContainers = getAasEnvContainerList(traefikHttpPort);
            aasEnvMongoDbContainer = getAasEnvMongoDbContainer();

            // Start AAS backend containers
            List<GenericContainer> containers = new ArrayList<>();
            containers.add(traefikContainer);
            containers.addAll(aasEnvContainers);
            containers.add(mqttBrokerContainer);
            containers.add(mqttGuiContainer);
            containers.add(mqttExporterContainer);
            containers.add(aasEnvMongoDbContainer);
            containers.add(aasRegistryContainer);
            containers.add(smRegistryContainer);
            startContainer(containers);

            runningContainers.add(traefikContainer);
            runningContainers.addAll(aasEnvContainers);
            runningContainers.add(mqttBrokerContainer);
            runningContainers.add(mqttGuiContainer);
            runningContainers.add(mqttExporterContainer);
            runningContainers.add(aasEnvMongoDbContainer);
            runningContainers.add(aasRegistryContainer);
            runningContainers.add(smRegistryContainer);

            Integer aasRegistryPort = aasRegistryContainer.getMappedPort(8080);
            Integer submodelRegistryPort = smRegistryContainer.getMappedPort(8080);
            Integer aasEnvPort = aasEnvContainers.get(0).getMappedPort(8081);
            Integer aasEnvMongoDbPort = aasEnvMongoDbContainer.getMappedPort(27017);
            Integer mqttBrokerPort = mqttBrokerContainer.getMappedPort(1883);
            Integer mqttBrokerWsPort = mqttBrokerContainer.getMappedPort(9001);
            Integer mqttGuiPort = mqttGuiContainer.getMappedPort(80);

            // Start AAS GUI container
            aasGuiContainer = getAasGuiContainer(aasRegistryPort, submodelRegistryPort, aasEnvPort);
            aasGuiContainer.start();
            runningContainers.add(aasGuiContainer);

            var aasGuiPort = aasGuiContainer.getMappedPort(3000);

            setSystemProperties(aasRegistryPort, submodelRegistryPort, mqttBrokerPort);
            printUrls(aasRegistryPort, submodelRegistryPort, aasEnvMongoDbPort, mqttBrokerPort, mqttBrokerWsPort, mqttGuiPort, aasGuiPort, traefikHttpPort, traefikHttpsPort, traefikApiPort);

            super.beforeAll(context);
        }
    }

    private void setSystemProperties(
            Integer aasRegistryPort,
            Integer submodelRegistryPort,
            Integer mqttBrokerPort
    ) {
        System.setProperty("aas.broker.host", HOSTNAME);
        System.setProperty("aas.broker.port", mqttBrokerPort.toString());
        System.setProperty("aas.aas-registry.port", aasRegistryPort.toString());
        System.setProperty("aas.submodel-registry.port", submodelRegistryPort.toString());
        System.setProperty("aas.aas-registry.url", "http://" + HOSTNAME + ":" + aasRegistryPort);
        System.setProperty("aas.submodel-registry.url", "http://" + HOSTNAME + ":" + submodelRegistryPort);
        System.setProperty("aas.aas-repository.port", String.valueOf(traefikWebPort));
        System.setProperty("aas.aas-repository.path", aasEnvUrlPrefix);
        System.setProperty("aas.aas-repository.url", "http://" + HOSTNAME + ":" + traefikWebPort + aasEnvUrlPrefix);
        System.setProperty("aas.submodel-repository.port", String.valueOf(traefikWebPort));
        System.setProperty("aas.submodel-repository.path", aasEnvUrlPrefix);
        System.setProperty("aas.submodel-repository.url", "http://" + HOSTNAME + ":" + traefikWebPort + aasEnvUrlPrefix);
    }

    private void printUrls(
            Integer aasRegistryPort,
            Integer submodelRegistryPort,
            Integer aasEnvMongoDbPort,
            Integer mqttBrokerPort,
            Integer mqttBrokerWsPort,
            Integer mqttGuiPort,
            Integer aasGuiPort,
            Integer traefikHttpPort,
            Integer traefikHttpsPort,
            Integer traefikApiPort
    ) {
        System.out.println("Traefik (http): http://" + HOSTNAME + ":" + traefikHttpPort);
        System.out.println("Traefik (https): http://" + HOSTNAME + ":" + traefikHttpsPort);
        System.out.println("Traefik Dashboard: http://" + HOSTNAME + ":" + traefikApiPort+ "/dashboard/");
        System.out.println("AAS Registry: http://" + HOSTNAME + ":" + aasRegistryPort);
        System.out.println("Submodel Registry: http://" + HOSTNAME + ":" + submodelRegistryPort);
        System.out.println("AAS Environment: http://" + HOSTNAME + ":" + traefikWebPort + aasEnvUrlPrefix);
        System.out.println("AAS Env Db: http://" + HOSTNAME + ":" + aasEnvMongoDbPort);
        System.out.println("MQTT Broker: tcp://" + HOSTNAME + ":" + mqttBrokerPort);
        System.out.println("MQTT WS Broker: ws://" + HOSTNAME + ":" + mqttBrokerWsPort);
        System.out.println("MQTT GUI: http://" + HOSTNAME + ":" + mqttGuiPort);
        System.out.println("AAS GUI: http://" + HOSTNAME + ":" + aasGuiPort);
    }


    private List<GenericContainer> getAasEnvContainerList(Integer traefikHttpPort) {
        String externalUrl = "http://" + HOSTNAME + ":" + traefikHttpPort + aasEnvUrlPrefix;
        return List.of(
                getAasEnvContainer(externalUrl),
                getAasEnvContainer(externalUrl),
                getAasEnvContainer(externalUrl),
                getAasEnvContainer(externalUrl),
                getAasEnvContainer(externalUrl)
        );
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
