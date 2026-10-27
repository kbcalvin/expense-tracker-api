package com.calvin.expensetracker.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.calvin.expensetracker.model.Expense;
import com.calvin.expensetracker.model.User;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
  List<Expense> findByUser(User user);
}
