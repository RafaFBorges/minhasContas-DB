package com.minhascontasdb.service;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.minhascontasdb.dto.Errors.InvalidArgumentsError;

@Entity
@Table(name = "categories", uniqueConstraints = {
    @UniqueConstraint(name = "UC_CATEGORY_NAME_OWNER", columnNames = { "name", "owner" })
})
public class Category {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String name;
  private Instant date;

  @Column(name = "owner")
  private Long owner;

  public Category() {
    this.name = "";
    this.date = null;
    this.id = (long) -1;
    this.owner = (long) -1;
  }

  public Category(String name, Instant date) {
    this.name = name;
    this.date = date;
    this.id = (long) -1;
    this.owner = (long) -1;
  }

  public Category(String name, Instant date, Long owner) {
    this(name, date);
    this.owner = owner;
    String strMessage = "";

    if (this.owner == null)
      strMessage = "Owner is null";
    else if (this.owner == -1)
      strMessage = "Invalid owner value";
    else if (this.name == "")
      strMessage = "invalid name value";

    if (strMessage != "")
      throw new InvalidArgumentsError(strMessage);
  }

  public Category(String name, Long owner) {
    this(name, Instant.now(), owner);
  }

  public Category(Long id, String name, Instant date, Long owner) {
    this(name, date, owner);
    this.id = id;

    if (this.id == -1)
      throw new InvalidArgumentsError();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public Instant getDate() {
    return this.date;
  }

  public Long getOwner() {
    return this.owner;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setOwner(Long owner) {
    this.owner = owner;
  }
}
