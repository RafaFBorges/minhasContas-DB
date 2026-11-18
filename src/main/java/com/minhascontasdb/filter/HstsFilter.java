package com.minhascontasdb.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Manda o navegador usar sempre HTTPS com esta API (HSTS). O cabeçalho só vale sobre uma conexão segura:
// atrás do proxy, request.isSecure() depende de server.forward-headers-strategy=framework
@Component
// Antes da cadeia do Spring Security: ela responde sozinha aos pedidos sem autenticação (401) e o HSTS não pode faltar neles.
// Depois do ForwardedHeaderFilter (HIGHEST_PRECEDENCE), que é quem faz isSecure() refletir o X-Forwarded-Proto
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
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
