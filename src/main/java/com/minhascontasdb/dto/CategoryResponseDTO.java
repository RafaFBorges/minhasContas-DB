package com.minhascontasdb.dto;

import java.time.Instant;

import com.minhascontasdb.service.Category;

public class CategoryResponseDTO {

  private Long id;
  private String name;
  private Instant date;
  private Long owner;

  public CategoryResponseDTO(Category category) {
    this.id = category.getId();
    this.name = category.getName();
    this.date = category.getDate();
    this.owner = category.getOwner().getId();
  }

  public CategoryResponseDTO(Long id, String name, Instant date, Long owner) {
    this.id = id;
    this.name = name;
    this.date = date;
    this.owner = owner;
  }

  public Long getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public Instant getDate() {
    return date;
  }

  public Long getOwner() {
    return this.owner;
  }
}
