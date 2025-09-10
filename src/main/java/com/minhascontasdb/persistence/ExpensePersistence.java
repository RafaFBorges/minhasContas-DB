package com.minhascontasdb.persistence;

import com.minhascontasdb.service.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpensePersistence extends JpaRepository<Expense, Long> {

  java.util.List<Expense> findByValue(double value);
}
