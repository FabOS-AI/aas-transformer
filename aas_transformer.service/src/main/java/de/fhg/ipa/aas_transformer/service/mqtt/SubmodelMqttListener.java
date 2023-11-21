package de.fhg.ipa.aas_transformer.service.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.events.ChangeEventType;
import de.fhg.ipa.aas_transformer.service.events.MessageEvent;
import de.fhg.ipa.aas_transformer.service.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.producers.ISubmodelMessageEventProducer;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.aas.metamodel.map.descriptor.SubmodelDescriptor;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

@Component
public class SubmodelMqttListener extends MqttListener implements IMqttMessageListener, ISubmodelMessageEventProducer {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelMqttListener.class);

    private LinkedBlockingQueue<SubmodelMessageEvent> submodelEventCache = new LinkedBlockingQueue();

    public SubmodelMqttListener(AASManager aasManager, AASRegistry aasRegistry) {
        super(aasManager, aasRegistry);
        this.topics.add("aas-registry/aas-registry/shells/+/submodels/+");
        this.topics.add("aas-repository/aas-server/shells/+/submodels/+");
    }

    @Override
    public LinkedBlockingQueue<? extends MessageEvent> getMessageEventCache() {
        return this.submodelEventCache;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        var objectMapper = new ObjectMapper();
        try {
            var submodelDescriptor = objectMapper.readValue(message.toString(), SubmodelDescriptor.class);

            var topicRegEx = ".*/shells/(.*)/submodels/([a-zA-Z]*)";
            var regExPattern = Pattern.compile(topicRegEx);
            var matcher = regExPattern.matcher(topic);
            matcher.find();

            var aasId = matcher.group(1);
            var operationType = matcher.group(2);
            var changeEventType = ChangeEventType.valueOf(operationType.toUpperCase());

            var submodelMessageEvent = new SubmodelMessageEvent(changeEventType, submodelDescriptor, new CustomId(aasId));
            submodelEventCache.add(submodelMessageEvent);
        } catch (Exception e) {
            LOG.error(e.getMessage() + " | " + message.toString());
            e.printStackTrace();
        }
    }
}
