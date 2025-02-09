package de.fhg.ipa.aas_transformer.clients.redis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJobAction;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class RedisTransformationJob implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(RedisTransformationJob.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final JsonSerializer jsonSerializer = new JsonSerializer();
    private static final JsonDeserializer jsonDeserializer = new JsonDeserializer();

    public TransformationJobAction transformationJobAction;
    public UUID transformerId;
    public String sourceSubmodelId;
    public String sourceSubmodel = null;

    // Default constructor is required for (De)serialization
    public RedisTransformationJob() {}

    public RedisTransformationJob(@NotNull TransformationJob transformationJob) {
        this.transformationJobAction = transformationJob.getTransformationJobAction();
        this.transformerId = transformationJob.getTransformerId();
        this.sourceSubmodelId = transformationJob.getSubmodelId();

        if(transformationJob.getSubmodel() == null) {
            this.sourceSubmodel = null;
            return;
        }

        try {
            this.sourceSubmodel = jsonSerializer.write(transformationJob.getSubmodel());
        } catch (SerializationException e) {
            LOG.error("Failed to serialize submodel | {}", e.getMessage());
            this.sourceSubmodel = null;
        }
    }

    @JsonIgnore
    public TransformationJob getTransformationJob() {
        Submodel submodel;
        try {
            submodel = jsonDeserializer.read(this.sourceSubmodel, Submodel.class);
        } catch (DeserializationException e) {
            LOG.error("Failed to deserialize submodel | {}", e.getMessage());
            submodel = null;
        } catch(IllegalArgumentException e) {
            LOG.info("Submodel is null | {}", e.getMessage());
            submodel = null;
        }

        return new TransformationJob(
                this.transformationJobAction,
                this.transformerId,
                this.sourceSubmodelId,
                submodel
        );
    }

    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RedisTransformationJob that)) return false;
        return transformationJobAction == that.transformationJobAction && Objects.equals(transformerId, that.transformerId) && Objects.equals(sourceSubmodelId, that.sourceSubmodelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transformationJobAction, transformerId, sourceSubmodelId);
    }
}
