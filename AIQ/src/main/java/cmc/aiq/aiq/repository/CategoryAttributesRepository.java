package cmc.aiq.aiq.repository;

import cmc.aiq.aiq.domain.CategoryAttributes;
import cmc.aiq.aiq.dto.Quration.CategoryDistanceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoryAttributesRepository extends JpaRepository<CategoryAttributes,Long> {

    @Query(value = """
        SELECT 
            id, 
            category_name as categoryName, 
            display_name as displayName, 
            attributes, 
            (embedding <=> cast(:vector as vector)) as distance 
        FROM category_attributes 
        ORDER BY distance ASC 
        LIMIT 1
        """, nativeQuery = true)
    Optional<CategoryDistanceResult> findClosestCategory(@Param("vector") float[] vector);
}
