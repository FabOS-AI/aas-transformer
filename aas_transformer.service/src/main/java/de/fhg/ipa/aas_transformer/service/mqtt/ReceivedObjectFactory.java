package de.fhg.ipa.aas_transformer.service.mqtt;

import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

@Component
public class ReceivedObjectFactory {

    private final AASRegistry aasRegistry;

    public ReceivedObjectFactory(AASRegistry aasRegistry) {
        this.aasRegistry = aasRegistry;
    }

    public ReceivedObject create(String topic, MqttMessage message) {
        return new ReceivedObject(topic, message, aasRegistry);
    }
}
