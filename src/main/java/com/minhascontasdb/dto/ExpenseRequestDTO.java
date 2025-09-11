package com.minhascontasdb.dto;

import java.time.Instant;

public class ExpenseRequestDTO {

  private double value;
  private Instant date;

  public ExpenseRequestDTO() {
    this(0.0, null);
  }

  public ExpenseRequestDTO(double value, Instant date) {
    this.value = value;
    this.date = date;
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
}
