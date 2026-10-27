package com.calvin.expensetracker.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.calvin.expensetracker.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);
}
