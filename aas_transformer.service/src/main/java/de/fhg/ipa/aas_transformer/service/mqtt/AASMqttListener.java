package de.fhg.ipa.aas_transformer.service.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.events.AASMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.ChangeEventType;
import de.fhg.ipa.aas_transformer.service.events.MessageEvent;
import de.fhg.ipa.aas_transformer.service.events.producers.IAASMessageEventProducer;
import org.eclipse.basyx.aas.metamodel.map.descriptor.AASDescriptor;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;

@Component
public class AASMqttListener extends MqttListener implements IMqttMessageListener, IAASMessageEventProducer {

    private static final Logger LOG = LoggerFactory.getLogger(AASMqttListener.class);

    private LinkedBlockingQueue<AASMessageEvent> aasEventCache = new LinkedBlockingQueue();

    public AASMqttListener(AASManager aasManager,AASRegistry aasRegistry) {
        super(aasManager, aasRegistry);
        this.topics.add("aas-registry/aas-registry/shells/+");
        this.topics.add("aas-repository/aas-server/shells/updated");
    }

    @Override
    public LinkedBlockingQueue<? extends MessageEvent> getMessageEventCache() {
        return this.aasEventCache;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        var objectMapper = new ObjectMapper();
        try {
            var aasDescriptor = objectMapper.readValue(message.toString(), AASDescriptor.class);

            var topicSplit = topic.split("/");
            var operationType = topicSplit[topicSplit.length-1];
            var changeEventType = ChangeEventType.valueOf(operationType.toUpperCase());

            var aasMessageEvent = new AASMessageEvent(changeEventType, aasDescriptor);
            aasEventCache.add(aasMessageEvent);

        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
    }
}
