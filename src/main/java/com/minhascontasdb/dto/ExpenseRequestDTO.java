package com.minhascontasdb.dto;

import java.time.Instant;
import java.util.List;

public class ExpenseRequestDTO {

  private double value;
  private Instant date;
  private List<Long> categoryIds;

  public ExpenseRequestDTO() {
    this(-1.0, null);
  }

  public ExpenseRequestDTO(double value, Instant date) {
    this.value = value;
    this.date = date;
    this.categoryIds = null;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public Instant getDate() {
    return date;
  }

  public void setDate(Instant date) {
    this.date = date;
  }

  public Boolean isValidValue() {
    return 0 <= this.value;
  }

  public List<Long> getCategoryIds() {
    return categoryIds;
  }

  public void setCategoryIds(List<Long> categoryIds) {
    this.categoryIds = categoryIds;
  }
}
