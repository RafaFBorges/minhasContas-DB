package com.minhascontasdb.persistence;

import com.minhascontasdb.dto.CategoryResponseDTO;
import com.minhascontasdb.service.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryPersistence extends JpaRepository<Category, Long> {

  Optional<Category> findByName(String name);

  Optional<Category> findByNameAndOwner_Id(String name, Long owner);

  @Query("SELECT new com.minhascontasdb.dto.CategoryResponseDTO(c.id, c.name, c.date, c.owner.id) FROM Category c WHERE c.owner.id = :ownerId")
  List<CategoryResponseDTO> findCategoryDTOsByOwnerId(@Param("ownerId") Long ownerId);
}
