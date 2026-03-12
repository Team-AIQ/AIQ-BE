package cmc.aiq.aiq.repository;

import cmc.aiq.aiq.domain.Models;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModelsRepository extends JpaRepository<Models, Long> {
    Optional<Models> findByName(String name);
}
