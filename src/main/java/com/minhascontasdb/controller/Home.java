package com.minhascontasdb.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Home {

  @GetMapping("/")
  public String Info() {
    return "MinhasContasDB - HomePage";
  }
}
