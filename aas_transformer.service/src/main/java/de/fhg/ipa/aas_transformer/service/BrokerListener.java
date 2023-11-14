package de.fhg.ipa.aas_transformer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.MqttVerb;
import de.fhg.ipa.aas_transformer.service.mqtt.SubmodelElementMessageListener;
import de.fhg.ipa.aas_transformer.service.mqtt.SubmodelMessageListener;
import org.eclipse.basyx.aas.manager.ConnectedAssetAdministrationShellManager;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.aas.registration.proxy.AASRegistryProxy;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.UUID;

@Component
public class BrokerListener implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(BrokerListener.class);

    private static final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
    private final MqttAsyncClient mqttClient;
    private String[] topics = new String[] {
        SubmodelMessageListener.TOPIC,
        SubmodelElementMessageListener.TOPIC
    };
    private IMqttMessageListener[] listeners;

    static {
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setConnectionTimeout(10);
    }

    public BrokerListener(
            @Value("${aas.broker.host}") String brokerHost,
            @Value("${aas.broker.port}") int brokerPort,
            @Value("${aas.registry.port}") int aasRegistryPort,
            SubmodelMessageListener submodelMessageListener,
            SubmodelElementMessageListener submodelElementMessageListener
    ) throws MqttException {
        this.mqttClient = new MqttAsyncClient(
                "tcp://"+brokerHost+":"+brokerPort,
                UUID.randomUUID().toString(),
                new MemoryPersistence()
        );

        this.listeners = new IMqttMessageListener[] {
                submodelMessageListener,
                submodelElementMessageListener
        };
    }

    @PostConstruct
    public void init() {
        var thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            mqttClient.connect(mqttConnectOptions);
        } catch (MqttException e) {
            LOG.error("MQTT client failed to connect: " + e.getMessage());
        }

        while(true) {
            try {
                mqttClient.subscribe(topics, new int[]{2, 2}, listeners);
                break;
            } catch(MqttException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                LOG.error("MQTT client failed to subscribe... sleeping and retry");
            }
        }
    }
}
