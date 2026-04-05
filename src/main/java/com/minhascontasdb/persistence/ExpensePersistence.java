package com.minhascontasdb.persistence;

import com.minhascontasdb.service.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpensePersistence extends JpaRepository<Expense, Long> {

  List<Expense> findByOwner_id(Long owner);
}
