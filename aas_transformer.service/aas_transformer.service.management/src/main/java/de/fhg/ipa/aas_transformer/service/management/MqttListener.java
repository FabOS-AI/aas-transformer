package de.fhg.ipa.aas_transformer.service.management;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;

@Component
public class MqttListener implements Runnable, MqttCallback {
    private static final Logger LOG = LoggerFactory.getLogger(MqttListener.class);

    private final Counter counter;
    private static final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
    private static final int MQTT_QOS = 2;
    private final MqttAsyncClient mqttClient;
    private final List<String> topics = List.of(
            "sm-repository/+/submodels/+",
            "sm-repository/+/submodels/+/submodelElements/#"
    );

    static {
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setConnectionTimeout(10);
    }

    public MqttListener(
            @Value("${aas.broker.host}") String brokerHost,
            @Value("${aas.broker.port}") int brokerPort,
            MeterRegistry meterRegistry
    ) throws MqttException {
        this.mqttClient = new MqttAsyncClient(
                "tcp://" + brokerHost + ":" + brokerPort,
                UUID.randomUUID().toString(),
                new MemoryPersistence()
        );

        this.counter = Counter.builder("aas_submodel_change_count")
                .description("a number of submodel changes during runtime of this service")
                .tag("name", "aas_submodel_change_count")
                .register(meterRegistry);
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
                for (String topic : this.topics) {
                    LOG.info("Subscribing to MQTT topic: " + topic);
                    this.mqttClient.subscribe(topic, MQTT_QOS);
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
    public void connectionLost(Throwable cause) {
        LOG.error("MQTT connection lost: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        this.counter.increment();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    public Counter getCounter() {
        return counter;
    }
}
