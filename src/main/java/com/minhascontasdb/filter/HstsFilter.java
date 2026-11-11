package com.minhascontasdb.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Manda o navegador usar sempre HTTPS com esta API (HSTS). O cabeçalho só vale sobre uma conexão segura:
// atrás do proxy, request.isSecure() depende de server.forward-headers-strategy=framework
@Component
public class HstsFilter extends OncePerRequestFilter {
  public static final String HEADER = "Strict-Transport-Security";
  public static final String VALUE = "max-age=31536000; includeSubDomains";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.isSecure())
      response.setHeader(HEADER, VALUE);

    chain.doFilter(request, response);
  }
}
