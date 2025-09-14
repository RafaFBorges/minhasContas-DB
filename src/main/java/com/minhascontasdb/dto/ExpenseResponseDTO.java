package com.minhascontasdb.dto;

import java.time.Instant;
import java.util.List;

public class ExpenseResponseDTO {

  private double value;
  private List<Instant> date;

  public ExpenseResponseDTO(double value, Instant date) {
    this.value = value;
    this.date.add(date);
  }

  public ExpenseResponseDTO(double value, List<Instant> dateList) {
    this.value = value;
    this.date = dateList;
  }

  public double getValue() {
    return value;
  }

  public List<Instant> getDates() {
    return date;
  }

  public void setValue(double value, Instant date) {
    this.value = value;
    this.date.add(date);
  }
}
