package cmc.aiq.aiq.repository;

import cmc.aiq.aiq.domain.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    Optional<PromptTemplate> findByIsActiveAndTemplateKey(boolean isActive, String templateKey);
}

