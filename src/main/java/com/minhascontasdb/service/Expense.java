package com.minhascontasdb.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
  private Instant date;

  @ManyToOne
  @JoinColumn(name = "owner")
  private User owner;

  @ManyToMany
  @JoinTable(name = "expense_category", joinColumns = @JoinColumn(name = "expense_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
  private List<Category> categories;

  public Expense() {
    this.date = Instant.now();
    this.categories = new ArrayList<>();
    this.owner = null;
  }

  public Expense(double value, Instant date) {
    this.date = date;
    this.categories = new ArrayList<>();
    this.value = value;
    this.owner = null;
  }

  public Expense(Long id, Double value, Instant date) {
    this(value, date);
    this.id = id;
    this.owner = null;
  }

  public Expense(Long id, Double value, Instant date, User owner) {
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

  public User getOwner() {
    return this.owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }

  public List<Category> getCategories() {
    return this.categories;
  }

  public void setCategories(List<Category> newList) {
    this.categories = newList;
  }

  public void setValue(double value, Instant date) {
    this.date = date;
    this.value = value;
  }

  public void setValue(double value) {
    this.date = Instant.now();
    this.value = value;
  }

  public Instant getLastDate() {
    return this.date;
  }
}
