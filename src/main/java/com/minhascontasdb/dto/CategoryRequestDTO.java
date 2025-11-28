package com.minhascontasdb.dto;

public class CategoryRequestDTO {

  private Long id;
  private String name;
  private Long owner;

  public CategoryRequestDTO(Long id, String name, Long owner) {
    this.id = id;
    this.name = name;
    this.owner = owner;
  }

  public Long getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public Long getOwner() {
    return this.owner;
  }

  public void setValue(Long id, String name, Long owner) {
    this.id = id;
    this.name = name;
    this.owner = owner;
  }
}
