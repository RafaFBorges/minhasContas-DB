package com.minhascontasdb.persistence;

import com.minhascontasdb.service.Category;
import com.minhascontasdb.service.Expense;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpensePersistence extends JpaRepository<Expense, Long> {

  /**
   * Busca todas as categorias cujo 'owner' ID é o mesmo do 'owner' da despesa
   * fornecida.
   * * @param expenseId O ID da despesa que queremos as categorias.
   * 
   * @return Uma lista de objetos Category.
   */
  @Query("""
          SELECT c
          FROM Category c, Expense e
          WHERE e.id = :expenseId AND c.owner = e.owner
      """)
  List<Category> findCategoriesByExpenseIdJoiningOnOwner(@Param("expenseId") Long expenseId);
}
