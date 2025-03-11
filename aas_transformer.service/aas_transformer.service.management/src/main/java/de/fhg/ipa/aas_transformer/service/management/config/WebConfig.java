package de.fhg.ipa.aas_transformer.service.management.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.fhg.ipa.aas_transformer.service.management.converter.DefaultSubmodelConverter;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.basyx.http.Aas4JHTTPSerializationExtension;
import org.eclipse.digitaltwin.basyx.http.SerializationExtension;
import org.eclipse.digitaltwin.basyx.submodelrepository.http.SubmodelRepositoryHTTPSerializationExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
//    @Bean
//    public HttpMessageConverter<Submodel> defaultSubmodelHttpMessageConverter() {
//        return new DefaultSubmodelConverter();
//    }
//
//    @Override
//    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//        converters.add(defaultSubmodelHttpMessageConverter());
//    }

    @Bean
    public SubmodelRepositoryHTTPSerializationExtension submodelRepositoryHTTPSerializationExtension() {
        return new SubmodelRepositoryHTTPSerializationExtension();
    }

    @Bean
    public Aas4JHTTPSerializationExtension aas4JHTTPSerializationExtension() {
        return new Aas4JHTTPSerializationExtension();
    }

    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder(List<SerializationExtension> serializationExtensions) {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder().serializationInclusion(JsonInclude.Include.NON_NULL);

        for (SerializationExtension serializationExtension : serializationExtensions) {
            serializationExtension.extend(builder);
        }
        return builder;
    }
}
