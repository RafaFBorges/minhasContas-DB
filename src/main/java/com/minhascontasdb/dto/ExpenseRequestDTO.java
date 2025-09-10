package com.minhascontasdb.dto;

import java.time.Instant;

public class ExpenseRequestDTO {

  private double value;
  private Instant date;

  public ExpenseRequestDTO() {
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
