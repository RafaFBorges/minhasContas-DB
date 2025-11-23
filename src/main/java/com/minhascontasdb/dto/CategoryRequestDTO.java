package com.minhascontasdb.dto;

public class CategoryRequestDTO {

  private Long id;
  private String name;

  public CategoryRequestDTO(long id, String name) {
    this.id = id;
    this.name = name;
  }

  public long getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public void setValue(long id, String name) {
    this.id = id;
    this.name = name;
  }
}
