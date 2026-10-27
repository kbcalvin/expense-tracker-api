package com.calvin.expensetracker.security;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.calvin.expensetracker.JwtUtil;
import com.calvin.expensetracker.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtUtil jwtUtil;
  private final UserRepository users;

  public JwtFilter(JwtUtil jwtUtil, UserRepository users) {
    this.jwtUtil = jwtUtil;
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && !header.isBlank()) {
      String trimmedHeader = header.trim();
      if (trimmedHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
        String token = trimmedHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
          SecurityContextHolder.clearContext();
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid bearer token");
          return;
        }

        try {
          String email = jwtUtil.extract(token);
          if (email == null || email.isBlank()) {
            throw new JwtException("Token subject missing");
          }

          if (SecurityContextHolder.getContext().getAuthentication() == null) {
            users.findByEmail(email).ifPresent(user -> {
              UsernamePasswordAuthenticationToken authentication =
                  new UsernamePasswordAuthenticationToken(
                      user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
              SecurityContextHolder.getContext().setAuthentication(authentication);
            });
          }
        } catch (JwtException exception) {
          SecurityContextHolder.clearContext();
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid bearer token");
          return;
        }
      }
    }

    chain.doFilter(request, response);
  }
}
