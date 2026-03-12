package cmc.aiq.aiq.repository;

import cmc.aiq.aiq.domain.CategoryAttributes;
import cmc.aiq.aiq.dto.Quration.CategoryAttributesDTO;
import cmc.aiq.aiq.dto.Quration.CategoryDistanceResult;
import cmc.aiq.aiq.dto.Quration.CategoryInfoDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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


    @Query("SELECT new cmc.aiq.aiq.dto.Quration.CategoryInfoDTO(c.categoryName, c.displayName, c.attributes) " +
            "FROM CategoryAttributes c WHERE c.categoryName = :categoryName")
    Optional<CategoryInfoDTO> findSimpleByCategoryName(@Param("categoryName") String categoryName);

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO category_attributes 
    (category_name, display_name, embedding, attributes, is_ai_generated, created_at) 
    VALUES (:name, :display, cast(:embedding as vector), cast(:attributes as jsonb), :isAi, now())
    """, nativeQuery = true)
    void insertCategoryWithVector(
            @Param("name") String name,
            @Param("display") String display,
            @Param("embedding") String embedding,
            @Param("attributes") String attributes,
            @Param("isAi") boolean isAi
    );

    Optional<CategoryAttributes> findByCategoryName(String categoryName);
}
