package cmc.aiq.aiq.repository;

import cmc.aiq.aiq.domain.AiResponse;
import cmc.aiq.aiq.domain.ENUM.ResponseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiResponseRepository extends JpaRepository<AiResponse, Long> {
    @Query("SELECT r FROM AiResponse r JOIN FETCH r.model WHERE r.id = :id")
    Optional<AiResponse> findByIdWithModel(@Param("id") Long id);

    // [추가] queryId로 모든 타입의 응답을 가져오는 메소드
    List<AiResponse> findAllByQueriesId(Long queryId);

    boolean existsByQueriesIdAndResponseType(Long queryId, ResponseType responseType);
    Optional<AiResponse> findByQueriesIdAndResponseType(Long queryId, ResponseType responseType);
}
