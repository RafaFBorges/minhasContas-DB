package com.minhascontasdb.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class Expense {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private double value;
  private List<Instant> date;

  public Expense() {
    this.date = new ArrayList<>();
  }

  public Expense(double value, Instant date) {
    this.date = new ArrayList<>();
    this.value = value;
    this.date.add(date);
  }

  public Expense(Long id, Double value, Instant date) {
    this(value, date);
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value, Instant date) {
    this.value = value;
    this.date.add(date);
  }

  public void setValue(double value) {
    this.value = value;
    this.date.add(Instant.now());
  }

  public Instant getLastDate() {
    return this.date.get(this.date.size() - 1);
  }

  public List<Instant> getDates() {
    return this.date;
  }
}
