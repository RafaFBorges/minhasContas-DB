package com.minhascontasdb.persistence;

import com.minhascontasdb.service.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpensePersistence extends JpaRepository<Expense, Long> {

  @Query("SELECT DISTINCT e FROM Expense e " +
      "LEFT JOIN FETCH e.owner " +
      "LEFT JOIN FETCH e.categories " +
      "WHERE e.owner.id = :ownerId")
  List<Expense> findByOwnerIdWithDetails(@Param("ownerId") Long ownerId);
}
