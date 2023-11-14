package de.fhg.ipa.aas_transformer.service.mqtt;

import de.fhg.ipa.aas_transformer.service.TransformerHandler;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

@Component
public class SubmodelElementMessageListener extends MessageListener implements IMqttMessageListener {
    public static final String TOPIC = "aas-repository/aas-server/shells/+/submodels/+/submodelElements/#";

    public SubmodelElementMessageListener(
            TransformerHandler transformerHandler,
            ReceivedObjectFactory receivedObjectFactory
    ) {
        super(transformerHandler, receivedObjectFactory);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        topicCache.add(topic);
        messageCache.add(receivedObjectFactory.create(topic, message));
        printMsg(topic, message);
    }
}
