package de.fhg.ipa.aas_transformer.service.executor.config;

import de.fhg.ipa.aas_transformer.service.executor.converter.DefaultSubmodelConverter;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public HttpMessageConverter<Submodel> defaultSubmodelHttpMessageConverter() {
        return new DefaultSubmodelConverter();
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(defaultSubmodelHttpMessageConverter());
    }
}
