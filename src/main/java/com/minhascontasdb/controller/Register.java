package com.minhascontasdb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minhascontasdb.dto.RegisterRequestDTO;
import com.minhascontasdb.dto.RegisterResponseDTO;
import com.minhascontasdb.dto.Errors.InvalidArgumentsError;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.User;

@RestController
@RequestMapping("/register")
@CrossOrigin(origins = "*")
public class Register {

  @Autowired
  private UserPersistence userPersistence;

  @PostMapping
  public ResponseEntity<?> register(@RequestBody RegisterRequestDTO userData) {
    try {
      User newCategory = new User(userData.getName(), userData.getEmail(), userData.getPassword(), userData.getUser());

      User savedUser = userPersistence.save(newCategory);

      return ResponseEntity.ok(new RegisterResponseDTO(savedUser != null));
    } catch (InvalidArgumentsError error) {
      return ResponseEntity.badRequest().body(error.getResponse());
    } catch (InvalidDataAccessResourceUsageException error) {
      return ResponseEntity.badRequest().body(error);
    } catch (DataIntegrityViolationException error) {
      return ResponseEntity.badRequest().body(new InvalidArgumentsError(error.getMessage()).getResponse());
    }
  }
}
