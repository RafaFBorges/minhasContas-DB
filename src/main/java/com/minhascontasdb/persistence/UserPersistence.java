package com.minhascontasdb.persistence;

import com.minhascontasdb.service.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPersistence extends JpaRepository<User, Long> {

  Optional<User> findByName(String name);

  Optional<User> findByEmail(String email);

  Optional<User> findByUser(String user);
}
