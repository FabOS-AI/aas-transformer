package de.fhg.ipa.aas_transformer.service.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.events.ChangeEventType;
import de.fhg.ipa.aas_transformer.service.events.MessageEvent;
import de.fhg.ipa.aas_transformer.service.events.SubmodelElementMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.producers.ISubmodelElementMessageProducer;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

@Component
public class SubmodelElementMqttListener extends MqttListener implements IMqttMessageListener, ISubmodelElementMessageProducer {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelElementMqttListener.class);

    private LinkedBlockingQueue<SubmodelElementMessageEvent> submodelElementEventCache = new LinkedBlockingQueue();

    public SubmodelElementMqttListener(AASManager aasManager, AASRegistry aasRegistry) {
        super(aasManager, aasRegistry);
        this.topics.add("aas-repository/aas-server/shells/+/submodels/+/submodelElements/#");
    }

    @Override
    public LinkedBlockingQueue<? extends MessageEvent> getMessageEventCache() {
        return this.submodelElementEventCache;
    }


    @Override
    public void messageArrived(String topic, MqttMessage message) {
        var objectMapper = new ObjectMapper();
        try {
            var topicRegEx = ".*/shells/([-\\w.]+)/submodels/([-\\w.]+)/.*/([-\\w.]+)";
            var regExPattern = Pattern.compile(topicRegEx);
            var matcher = regExPattern.matcher(topic);
            matcher.find();

            var aasId = matcher.group(1);
            var smId = matcher.group(2);
            var action = matcher.group(3);
            ChangeEventType changeEventType;
            var submodelDescriptor = this.aasRegistry.lookupSubmodel(new CustomId(aasId), new CustomId(smId));
            if (action.equals("value")) {
                changeEventType = ChangeEventType.UPDATED;
            } else {
                changeEventType = ChangeEventType.valueOf(action.toUpperCase());
            }
            var submodelElementMessageEvent = new SubmodelElementMessageEvent(changeEventType, submodelDescriptor, new CustomId(aasId));
            this.submodelElementEventCache.add(submodelElementMessageEvent);

        } catch (Exception e) {
            LOG.error(e.getMessage() + " | TOPCI: "  + topic + " MESSAGE: " + message.toString());
            e.printStackTrace();
        }
    }
}
