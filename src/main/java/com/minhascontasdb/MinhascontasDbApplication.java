package com.minhascontasdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class MinhascontasDbApplication {
  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    System.out.println("Loading from .env");
    dotenv.entries().forEach(entry -> {
      System.out.println("Loaded from .env: " + entry.getKey() + "=" + entry.getValue());
      System.setProperty(entry.getKey(), entry.getValue());
    });

    SpringApplication.run(MinhascontasDbApplication.class, args);
  }
}
