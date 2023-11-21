package de.fhg.ipa.aas_transformer.service.mqtt;

import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.events.MessageEvent;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class MqttListener implements IMqttMessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(MqttListener.class);

    protected final AASManager aasManager;

    protected final AASRegistry aasRegistry;

    protected List<String> topics = new ArrayList<>();

    public MqttListener(
            AASManager aasManager,
            AASRegistry aasRegistry) {
        this.aasManager = aasManager;
        this.aasRegistry = aasRegistry;
    }

    public List<String> getTopics() {
        return topics;
    }

    public abstract LinkedBlockingQueue<? extends MessageEvent> getMessageEventCache();

    protected void printMsg(String topic, MqttMessage message) {
        LOG.info("TOPIC: " + topic);
        LOG.info("AAS ID: " + getAASIdFromTopic(topic));
        LOG.info("MESSAGE: " + message.toString());
    }

    protected String getAASIdFromTopic(String topic) {
        return topic.split("/")[3];
    }
}
