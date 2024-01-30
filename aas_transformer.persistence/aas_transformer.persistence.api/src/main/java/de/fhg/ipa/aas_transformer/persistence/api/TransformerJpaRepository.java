package de.fhg.ipa.aas_transformer.persistence.api;

import de.fhg.ipa.aas_transformer.model.Transformer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransformerJpaRepository extends JpaRepository<Transformer, UUID> {

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    Optional<Transformer> findById(UUID id);

}
