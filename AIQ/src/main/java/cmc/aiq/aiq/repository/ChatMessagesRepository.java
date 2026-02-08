package cmc.aiq.aiq.repository;

import cmc.aiq.aiq.domain.ChatMessages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessagesRepository extends JpaRepository<ChatMessages , Long> {
    boolean existsByQueriesIdAndRoundCount(Long queryId , Integer roundCount);
    Optional<ChatMessages> findTopByQueriesIdOrderByCreatedAtDesc(Long queryId);
    List<ChatMessages> findAllByQueriesIdOrderByCreatedAtAsc(Long queryId);
}
