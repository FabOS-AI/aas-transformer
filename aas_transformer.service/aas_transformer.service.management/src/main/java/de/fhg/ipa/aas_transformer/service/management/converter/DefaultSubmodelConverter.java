package de.fhg.ipa.aas_transformer.service.management.converter;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.util.List;

public class DefaultSubmodelConverter implements HttpMessageConverter<Submodel> {
    private static JsonDeserializer jsonDeserializer = new JsonDeserializer();
    private static JsonSerializer jsonSerializer = new JsonSerializer();

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return Submodel.class.isAssignableFrom(clazz) && (mediaType == null || MediaType.APPLICATION_JSON.includes(mediaType));
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return Submodel.class.isAssignableFrom(clazz) && (mediaType == null || MediaType.APPLICATION_JSON.includes(mediaType));
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.of(MediaType.APPLICATION_JSON);
    }

    @Override
    public Submodel read(Class<? extends Submodel> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        try {
            return jsonDeserializer.read(inputMessage.getBody(), Submodel.class);
        } catch (DeserializationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(Submodel submodel, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            outputMessage.getBody().write(jsonSerializer.write(submodel).getBytes());
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

    }
}
