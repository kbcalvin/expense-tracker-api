package com.calvin.expensetracker.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.calvin.expensetracker.JwtUtil;
import com.calvin.expensetracker.model.User;
import com.calvin.expensetracker.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtUtil jwtUtil;

  public AuthController(UserRepository users, PasswordEncoder encoder, JwtUtil jwtUtil) {
    this.users = users;
    this.encoder = encoder;
    this.jwtUtil = jwtUtil;
  }

  @PostMapping("/register")
  public Map<String, String> register(@RequestBody AuthRequest body) {
    if (body.email() == null || body.password() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
    }
    if (users.findByEmail(body.email()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    User user = new User();
    user.setEmail(body.email());
    user.setPassword(encoder.encode(body.password()));
    users.save(user);
    return Map.of("token", jwtUtil.generate(user.getEmail()), "email", user.getEmail());
  }

  @PostMapping("/login")
  public Map<String, String> login(@RequestBody AuthRequest body) {
    if (body.email() == null || body.password() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
    }
    User user = users.findByEmail(body.email())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!encoder.matches(body.password(), user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    return Map.of("token", jwtUtil.generate(user.getEmail()));
  }

  public record AuthRequest(String email, String password) {}
}
