package com.minhascontasdb.service;

import java.time.Instant;

public class ExpenseResponseDTO {

  private double value;
  private Instant date;

  public ExpenseResponseDTO(double value) {
    this.value = value;
    this.date = Instant.now();
  }

  public ExpenseResponseDTO(double value, Instant date) {
    this.value = value;
    this.date = date;
  }

  public double getValue() {
    return value;
  }

  public Instant getDate() {
    return date;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public void setDate(Instant date) {
    this.date = date;
  }
}
