package de.fhg.ipa.aas_transformer.service.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.MySqlDialect;

import java.util.List;

@Configuration
public class ConverterConfig {

    @Bean
    public R2dbcCustomConversions customConversions(List<Converter> converters) {
        return R2dbcCustomConversions.of(MySqlDialect.INSTANCE, converters);
    }
}
