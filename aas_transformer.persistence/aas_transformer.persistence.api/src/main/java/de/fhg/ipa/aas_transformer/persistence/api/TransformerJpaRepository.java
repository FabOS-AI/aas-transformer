package de.fhg.ipa.aas_transformer.persistence.api;

import de.fhg.ipa.aas_transformer.model.Transformer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransformerJpaRepository extends JpaRepository<Transformer, UUID> {
}
