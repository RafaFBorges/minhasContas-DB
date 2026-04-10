package com.minhascontasdb.dto;

import java.time.Instant;
import java.util.List;

import com.minhascontasdb.service.Expense;

public class ExpenseResponseDTO {

  private Long id;
  private double value;
  private Instant date;
  private Long owner;
  private List<Long> categoryIds;

  public ExpenseResponseDTO(Expense expense) {
    this.id = expense.getId();
    this.value = expense.getValue();

    this.date = expense.getLastDate();
    this.owner = expense.getOwner() != null ? expense.getOwner().getId() : null;
    this.categoryIds = expense.getCategories() != null
        ? expense.getCategories().stream().map(c -> c.getId()).toList()
        : List.of();
  }

  public ExpenseResponseDTO(double value, Instant date) {
    this.value = value;
    this.date = date;
  }

  public double getValue() {
    return this.value;
  }

  public Instant getDate() {
    return this.date;
  }

  public void setValue(double value, Instant date) {
    this.value = value;
    this.date = date;
  }

  public Long getId() {
    return this.id;
  }

  public Long getOwner() {
    return this.owner;
  }

  public List<Long> getCategoryIds() {
    return this.categoryIds;
  }
}
