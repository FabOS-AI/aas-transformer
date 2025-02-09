package de.fhg.ipa.aas_transformer.persistence.api;

import de.fhg.ipa.aas_transformer.model.TransformationLog;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface TransformationLogJpaRepository extends R2dbcRepository<TransformationLog, Long> {
}
