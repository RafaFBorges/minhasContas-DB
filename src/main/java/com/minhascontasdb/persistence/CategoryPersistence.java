package com.minhascontasdb.persistence;

import com.minhascontasdb.service.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryPersistence extends JpaRepository<Category, Long> {

  Optional<Category> findByName(String name);

  Optional<Category> findByNameAndOwner(String name, Long owner);

  List<Category> findByOwner(Long owner);
}
