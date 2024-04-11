package de.fhg.ipa.aas_transformer.service.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.events.ChangeEventType;
import de.fhg.ipa.aas_transformer.service.events.MessageEvent;
import de.fhg.ipa.aas_transformer.service.events.SubmodelElementMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.producers.ISubmodelElementMessageProducer;
import org.eclipse.digitaltwin.basyx.http.Base64UrlEncoder;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
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

    public SubmodelElementMqttListener(SubmodelRegistry submodelRegistry, SubmodelRepository submodelRepository) {
        super(submodelRegistry, submodelRepository);
        this.topics.add("sm-repository/+/submodels/+/submodelElements/#");
    }

    @Override
    public LinkedBlockingQueue<? extends MessageEvent> getMessageEventCache() {
        return this.submodelElementEventCache;
    }


    @Override
    public void messageArrived(String topic, MqttMessage message) {
        var objectMapper = new ObjectMapper();
        try {
            // Get info from topic
            var topicRegEx = "sm-repository/([-\\w.]+)/submodels/([a-zA-Z0-9]+)/submodelElements/([-\\w.]+)/([a-zA-Z]+)";
            var regExPattern = Pattern.compile(topicRegEx);
            var matcher = regExPattern.matcher(topic);
            matcher.find();
            var submodelRepositoryId = matcher.group(1);
            var submodelIdEncoded = matcher.group(2);
            var submodelId = Base64UrlEncoder.decode(submodelIdEncoded);
            var submodelElementId = matcher.group(3);
            var changeEventTypeString = matcher.group(4);
            var changeEventType = ChangeEventType.valueOf(changeEventTypeString.toUpperCase());

            var submodelElementMessageEvent = new SubmodelElementMessageEvent(changeEventType, submodelId, submodelElementId);
            this.submodelElementEventCache.add(submodelElementMessageEvent);

        } catch (Exception e) {
            LOG.error(e.getMessage() + " | TOPIC: "  + topic + " | MESSAGE: " + message.toString());
            e.printStackTrace();
        }
    }
}
