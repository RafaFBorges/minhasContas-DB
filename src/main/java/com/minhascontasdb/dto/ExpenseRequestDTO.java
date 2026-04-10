package com.minhascontasdb.dto;

import java.time.Instant;
import java.util.List;

public class ExpenseRequestDTO {

  private Long id;
  private double value;
  private Instant date;
  private Long owner;
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
    return this.value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public Instant getDate() {
    return this.date;
  }

  public void setDate(Instant date) {
    this.date = date;
  }

  public Long getOwner() {
    return this.owner;
  }

  public Long getId() {
    return this.id;
  }

  public void setOwner(Long owner) {
    this.owner = owner;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Boolean isValidValue() {
    return 0 <= this.value;
  }

  public List<Long> getCategoryIds() {
    return this.categoryIds;
  }

  public void setCategoryIds(List<Long> categoryIds) {
    this.categoryIds = categoryIds;
  }

  public boolean isValidDate() {
    return this.date != null;
  }

  public boolean isValidOwner() {
    return this.owner != null && 0 < this.owner;
  }
}
