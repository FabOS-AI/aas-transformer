package de.fhg.ipa.aas_transformer.persistence.api;

import de.fhg.ipa.aas_transformer.model.Transformer;
import jakarta.persistence.LockModeType;
//import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

//@Repository
//public interface TransformerJpaRepository extends JpaRepository<Transformer, UUID> {
public interface TransformerJpaRepository extends R2dbcRepository<Transformer, UUID> {

//    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
//    Mono<Transformer> findById(UUID id);
    @Override
//    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    Mono<Transformer> findById(UUID uuid);
}
