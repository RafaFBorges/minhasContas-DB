package com.minhascontasdb.persistence;

import com.minhascontasdb.service.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryPersistence extends JpaRepository<Category, Long> {
}
