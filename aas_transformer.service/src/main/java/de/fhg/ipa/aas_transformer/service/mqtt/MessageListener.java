package de.fhg.ipa.aas_transformer.service.mqtt;

import de.fhg.ipa.aas_transformer.service.BrokerListener;
import de.fhg.ipa.aas_transformer.service.TransformerHandler;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerListener.class);

    protected final TransformerHandler transformerHandler;

    protected static ConcurrentLinkedQueue<ReceivedObject> messageCache = new ConcurrentLinkedQueue();

    protected static LinkedList<String> topicCache = new LinkedList<>();

    protected final ReceivedObjectFactory receivedObjectFactory;

    public MessageListener(
            TransformerHandler transformerHandler,
            ReceivedObjectFactory receivedObjectFactory) {
        this.transformerHandler = transformerHandler;
        this.receivedObjectFactory = receivedObjectFactory;
    }

    protected void printMsg(String topic, MqttMessage message) {
        LOG.info("TOPIC: " + topic);
        LOG.info("AAS ID: " + getAASIdFromTopic(topic));
        LOG.info("MESSAGE: " + message.toString());
    }

    public String getAASIdFromTopic(String topic) {
        return topic.split("/")[3];
    }

    public static ConcurrentLinkedQueue<ReceivedObject> getMessageCache() {
        return messageCache;
    }

    public static LinkedList<String> getTopicCache() {
        return topicCache;
    }

}
