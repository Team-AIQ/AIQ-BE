package cmc.aiq.aiq.repository;

import cmc.aiq.aiq.domain.AiResponse;
import cmc.aiq.aiq.domain.ENUM.ResponseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiResponseRepository extends JpaRepository<AiResponse, Long> {
    @Query("SELECT r FROM AiResponse r JOIN FETCH r.model WHERE r.id = :id")
    Optional<AiResponse> findByIdWithModel(@Param("id") Long id);
    Optional<AiResponse> findByQueriesIdAndResponseType(Long queryId, ResponseType responseType);
    boolean existsByQueriesIdAndResponseType(Long queryId, ResponseType responseType);
}
