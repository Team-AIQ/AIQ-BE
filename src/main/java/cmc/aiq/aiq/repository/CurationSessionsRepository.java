package cmc.aiq.aiq.repository;

import cmc.aiq.aiq.domain.CurationSessions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurationSessionsRepository extends JpaRepository<CurationSessions, Long> {
    Optional<CurationSessions> findByQueryId(Long queryId);
}
