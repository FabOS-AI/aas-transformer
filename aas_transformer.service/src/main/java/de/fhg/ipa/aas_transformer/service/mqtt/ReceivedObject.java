package de.fhg.ipa.aas_transformer.service.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.MqttVerb;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.aas.metamodel.map.descriptor.SubmodelDescriptor;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public class ReceivedObject {

    private ObjectMapper objectMapper = new ObjectMapper();

    private final AASRegistry aasRegistry;

    private String topic;

    private MqttMessage message;

    public ReceivedObject(String topic, MqttMessage message, AASRegistry aasRegistry) {
        this.topic = topic;
        this.message = message;
        this.aasRegistry = aasRegistry;
    }
    public MqttVerb getVerbFromTopic() {
        String[] topicSplit = topic.split("/");
        String verb = topicSplit[topicSplit.length - 1];

        Optional<@NotNull MqttVerb> optionalVerb = Arrays.stream(MqttVerb.values())
                .filter(v -> v.getMqttVerb().equals(verb))
                .findFirst();

        if(optionalVerb.isEmpty())
            return MqttVerb.UPDATED;

        return optionalVerb.get();
    }

    protected String getSmIdFromTopic(String topic) { return topic.split("/")[5]; }

    public Submodel getSubmodelFromTopic() {
        try {
            return objectMapper.readValue(message.toString(), Submodel.class);
        } catch (JsonMappingException e) {
            return getSubmodelFromRegistry();
        } catch (JsonProcessingException e) {
            return getSubmodelFromRegistry();
        }
    }

    private Submodel getSubmodelFromRegistry() {
        SubmodelDescriptor submodelDescriptor = aasRegistry.lookupSubmodel(
                new CustomId(getAASIdFromTopic(topic)),
                new CustomId(getSmIdFromTopic(topic))
        );
        Submodel submodel = new Submodel(
                submodelDescriptor.getIdShort(),
                submodelDescriptor.getIdentifier()
        );
        return submodel;
    }

    public String getAASIdFromTopic() {
        return topic.split("/")[3];
    }

    public String getAASIdFromTopic(String topic) {
        return topic.split("/")[3];
    }

    public String getTopic() {
        return topic;
    }

    public MqttMessage getMessage() {
        return message;
    }
}
