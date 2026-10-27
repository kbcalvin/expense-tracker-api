package com.calvin.expensetracker.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.calvin.expensetracker.model.Expense;
import com.calvin.expensetracker.model.User;
import com.calvin.expensetracker.repository.ExpenseRepository;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
  private final ExpenseRepository expenses;

  public ExpenseController(ExpenseRepository expenses) {
    this.expenses = expenses;
  }

  @PostMapping
  public Expense create(@RequestBody Expense expense, @AuthenticationPrincipal User user) {
    requireUser(user);
    if (expense == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expense payload is required");
    }
    if (expense.getTitle() == null || expense.getTitle().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expense title is required");
    }
    if (expense.getAmount() == null || expense.getAmount() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expense amount must be positive");
    }
    if (expense.getDate() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expense date is required");
    }

    expense.setUser(user);
    return expenses.save(expense);
  }

  @GetMapping
  public List<Expense> list(@AuthenticationPrincipal User user) {
    requireUser(user);
    return expenses.findByUser(user);
  }

  @GetMapping("/summary")
  public Map<String, Double> summary(@AuthenticationPrincipal User user) {
    requireUser(user);
    return expenses.findByUser(user).stream()
        .collect(Collectors.groupingBy(
            expense -> expense.getCategory() == null || expense.getCategory().isBlank()
                ? "uncategorized"
                : expense.getCategory(),
            Collectors.summingDouble(Expense::getAmount)));
  }

  private void requireUser(User user) {
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
  }
}
