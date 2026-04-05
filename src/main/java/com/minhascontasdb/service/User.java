package com.minhascontasdb.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String name;
  private String email;
  private String password;

  @Column(name = "username")
  private String user;

  @OneToMany(mappedBy = "owner")
  private List<Expense> expenses;

  @OneToMany(mappedBy = "owner")
  private List<Category> categories;

  public User() {
    this.name = "";
    this.email = "";
    this.user = "";
    this.password = "";
    this.expenses = new ArrayList<>();
    this.categories = new ArrayList<>();
  }

  public User(String name, String email, String password, String user) {
    this.name = name;
    this.email = email;
    this.user = user;
    this.password = password;
    this.expenses = new ArrayList<>();
    this.categories = new ArrayList<>();
  }

  public Long getId() {
    return this.id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public String getEmail() {
    return this.email;
  }

  public String getPassword() {
    return this.password;
  }

  public String getUser() {
    return this.user;
  }

  public void setName(String newName) {
    this.name = newName;
  }

  public void setEmail(String newEmail) {
    this.email = newEmail;
  }

  public void setPassword(String newPassword) {
    this.password = newPassword;
  }

  public void setUser(String newUser) {
    this.user = newUser;
  }

  public List<Expense> getExpenses() {
    return this.expenses;
  }

  public void setExpenses(List<Expense> expenses) {
    this.expenses = expenses;
  }

  public List<Category> getCategories() {
    return this.categories;
  }

  public void setCategories(List<Category> categories) {
    this.categories = categories;
  }
}
