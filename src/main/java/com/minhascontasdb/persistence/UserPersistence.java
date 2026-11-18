package com.minhascontasdb.persistence;

import com.minhascontasdb.service.Role;
import com.minhascontasdb.service.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPersistence extends JpaRepository<User, Long> {
  boolean existsByUser(String user);

  boolean existsByEmail(String email);

  boolean existsByRole(Role role);

  Optional<User> findById(Long id);

  Optional<User> findByUser(String user);
}
