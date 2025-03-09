package de.fhg.ipa.aas_transformer.persistence.api;

import de.fhg.ipa.aas_transformer.model.TransformationDescription;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TransformationDescriptionJpaRepository extends R2dbcRepository<TransformationDescription, Long> {
    Flux<TransformationDescription> findBySourceSubmodelId(String sourceSubmodelId);
    Mono<TransformationDescription> findByTargetSubmodelId(String targetSubmodelId);
    Flux<TransformationDescription> findByTransformerId(UUID transformerId);
}
