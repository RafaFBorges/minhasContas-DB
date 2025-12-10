package com.minhascontasdb.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class Expense {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private double value;
  private List<Instant> date;

  @Column(name = "owner")
  private Long owner;

  @ManyToMany
  @JoinTable(name = "expense_category", joinColumns = @JoinColumn(name = "expense_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
  private List<Category> categories;

  public Expense() {
    this.date = new ArrayList<>();
    this.categories = new ArrayList<>();
    this.owner = (long) -1;
  }

  public Expense(double value, Instant date) {
    this.date = new ArrayList<>();
    this.categories = new ArrayList<>();
    this.value = value;
    this.date.add(date);
    this.owner = (long) -1;
  }

  public Expense(Long id, Double value, Instant date) {
    this(value, date);
    this.id = id;
    this.owner = (long) -1;
  }

  public Expense(Long id, Double value, Instant date, Long owner) {
    this(id, value, date);
    this.owner = owner;
  }

  public Long getId() {
    return this.id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public double getValue() {
    return this.value;
  }

  public Long getOwner() {
    return this.owner;
  }

  public List<Category> getCategories() {
    return this.categories;
  }

  public void setCategories(List<Category> newList) {
    this.categories = newList;
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
