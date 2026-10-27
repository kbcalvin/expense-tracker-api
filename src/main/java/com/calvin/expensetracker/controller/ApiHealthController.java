package com.calvin.expensetracker.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiHealthController {
  @GetMapping("/api/health")
  public Map<String, String> health() {
    return Map.of("status", "up");
  }
}
