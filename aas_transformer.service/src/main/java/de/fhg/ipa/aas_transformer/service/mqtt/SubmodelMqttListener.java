package de.fhg.ipa.aas_transformer.service.mqtt;

import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.events.ChangeEventType;
import de.fhg.ipa.aas_transformer.service.events.MessageEvent;
import de.fhg.ipa.aas_transformer.service.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.producers.ISubmodelMessageEventProducer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
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

    public SubmodelMqttListener(SubmodelRegistry submodelRegistry, SubmodelRepository submodelRepository) {
        super(submodelRegistry, submodelRepository);
        this.topics.add("sm-repository/+/submodels/+");
    }

    @Override
    public LinkedBlockingQueue<? extends MessageEvent> getMessageEventCache() {
        return this.submodelEventCache;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            // Get info from topic
            var topicRegEx = "sm-repository/([-\\w.]+)/submodels/([a-zA-Z]+)";
            var regExPattern = Pattern.compile(topicRegEx);
            var matcher = regExPattern.matcher(topic);
            matcher.find();
            var submodelRepositoryId = matcher.group(1);
            var changeEventTypeString = matcher.group(2);
            var changeEventType = ChangeEventType.valueOf(changeEventTypeString.toUpperCase());
            // Parse submodel
            JsonDeserializer jsonDeserializer = new JsonDeserializer();
            var submodel = jsonDeserializer.read(message.toString(), Submodel.class);

            var submodelMessageEvent = new SubmodelMessageEvent(changeEventType, submodel);
            submodelEventCache.add(submodelMessageEvent);
        } catch (Exception e) {
            LOG.error(e.getMessage() + " | TOPIC: "  + topic + " | MESSAGE: " + message.toString());
            e.printStackTrace();
        }
    }
}
