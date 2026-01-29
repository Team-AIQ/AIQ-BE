package cmc.aiq.aiq.repository;

import cmc.aiq.aiq.domain.Queries;
import cmc.aiq.aiq.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QueriesRepository extends JpaRepository<Queries, Long> {

}
