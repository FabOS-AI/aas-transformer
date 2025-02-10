package de.fhg.ipa.aas_transformer.service.listener.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class BrokerListener implements Runnable, MqttCallback {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerListener.class);

    private static final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
    private static final int MQTT_QOS = 2;
    private final MqttAsyncClient mqttClient;

    private List<MqttListener> listeners = new ArrayList<>();

    static {
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setConnectionTimeout(10);
    }

    public BrokerListener(
            @Value("${aas.broker.host}") String brokerHost,
            @Value("${aas.broker.port}") int brokerPort,
            SubmodelMqttListener submodelMqttListener,
            SubmodelElementMqttListener submodelElementMqttListener
    ) throws MqttException {
        this.mqttClient = new MqttAsyncClient(
                "tcp://" + brokerHost + ":" + brokerPort,
                UUID.randomUUID().toString(),
                new MemoryPersistence()
        );

        this.listeners.add(submodelMqttListener);
        this.listeners.add(submodelElementMqttListener);
    }

    public boolean isConnected() {
        return this.mqttClient.isConnected();
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

        // Subscribe to all topics; break while loop if successful:
        while(true) {
            try {
                for (var listener : this.listeners) {
                    for (var topic : listener.getTopics()) {
                        LOG.info("Subscribing to MQTT topic: " + topic);
                        this.mqttClient.subscribe(topic, MQTT_QOS, listener);
                    }
                }
                this.mqttClient.setCallback(this);
                LOG.info("MQTT topic subscriptions are successful.");
                break;
            } catch(MqttException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                LOG.error("MQTT client for broker '" + this.mqttClient.getServerURI() + "' failed to subscribe... sleeping and retry: " + e.getMessage());
            } catch (Exception e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        for(var listener : this.listeners) {
            listener.messageArrived(topic, mqttMessage);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
