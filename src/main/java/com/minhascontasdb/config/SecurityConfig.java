package com.minhascontasdb.config;

import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhascontasdb.dto.AuthUser;
import com.minhascontasdb.dto.Errors.ErrorResponseDTO;
import com.minhascontasdb.dto.Errors.ErrorResponseDTOObject;
import com.minhascontasdb.dto.Errors.ForbiddenError;
import com.minhascontasdb.dto.Errors.InvalidAccessError;
import com.minhascontasdb.service.JwtService;
import com.minhascontasdb.service.Role;

import org.springframework.web.util.UrlPathHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// A API é um resource server: toda rota exige "Authorization: Bearer <access token>", exceto as públicas abaixo.
// Os papéis (Role, RoleHierarchy) já vêm no token, mas nenhuma rota exige papel ainda: hoje todo usuário autenticado tem o mesmo acesso.
// Ao usar hasRole(...), lembre que o papel do token só muda quando ele expira (15 min)
@Configuration
public class SecurityConfig {
  // Rotas sem login. O cabeçalho Authorization é ignorado nelas (um token vencido não pode bloquear o /login ou o /refresh)
  private static final Set<String> PUBLIC_PATHS = Set.of("/", "/login", "/refresh", "/logout", "/register", "/error");

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
    // RFC 6750: sem token o cabeçalho é só "Bearer"; com token recusado (vencido, forjado, malformado) leva error="invalid_token".
    // Vencido e forjado não se distinguem aqui de propósito: o cliente tenta o /refresh em qualquer 401 e, se ele falhar, pede login
    AuthenticationEntryPoint unauthorized = (request, response, e) -> writeError(response, mapper,
        HttpServletResponse.SC_UNAUTHORIZED, new InvalidAccessError().getResponse(),
        e instanceof OAuth2AuthenticationException ? "Bearer error=\"invalid_token\"" : "Bearer");
    AccessDeniedHandler forbidden = (request, response, e) -> writeError(response, mapper,
        HttpServletResponse.SC_FORBIDDEN, new ForbiddenError().getResponse(), null);

    http
        // Sem cookie nem sessão na API, então não há o que falsificar entre sites: o CSRF fica a cargo de quem guarda o cookie (o Web)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(Customizer.withDefaults())
        // O /logout é nosso (revoga o refresh token): o filtro de logout padrão redirecionaria a requisição
        .logout(AbstractHttpConfigurer::disable)
        // O HSTS é do HstsFilter, que também decide pelo X-Forwarded-Proto
        .headers(h -> h.httpStrictTransportSecurity(hsts -> hsts.disable()))
        .authorizeHttpRequests(a -> a
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(SecurityConfig::isPublic).permitAll()
            .anyRequest().authenticated())
        .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
        .oauth2ResourceServer(o -> o
            .bearerTokenResolver(bearerTokenResolver())
            .authenticationEntryPoint(unauthorized)
            .accessDeniedHandler(forbidden)
            .jwt(j -> j.jwtAuthenticationConverter(SecurityConfig::toAuthentication)));

    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder(JwtService jwtService) {
    return jwtService.decoder();
  }

  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(Role.hierarchy());
  }

  // Caminho dentro da aplicação, sem depender de como o servlet foi mapeado (no MockMvc o servletPath vem vazio)
  private static boolean isPublic(HttpServletRequest request) {
    return PUBLIC_PATHS.contains(UrlPathHelper.defaultInstance.getPathWithinApplication(request));
  }

  private static BearerTokenResolver bearerTokenResolver() {
    DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();

    return request -> isPublic(request) ? null : resolver.resolve(request);
  }

  // Do JWT já validado para o usuário que os controllers recebem (@AuthenticationPrincipal AuthUser)
  static AbstractAuthenticationToken toAuthentication(Jwt jwt) {
    try {
      Role role = Role.valueOf(jwt.getClaimAsString(JwtService.CLAIM_ROLE));
      AuthUser principal = new AuthUser(Long.valueOf(jwt.getSubject()), jwt.getClaimAsString(JwtService.CLAIM_USERNAME), role);

      return new UsernamePasswordAuthenticationToken(principal, jwt, List.of(new SimpleGrantedAuthority(role.authority())));
    } catch (RuntimeException e) {
      throw new InvalidBearerTokenException("Invalid token claims", e);
    }
  }

  private static void writeError(HttpServletResponse response, ObjectMapper mapper, int status, ErrorResponseDTO body,
      String authenticateHeader) throws java.io.IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    if (authenticateHeader != null)
      response.setHeader("WWW-Authenticate", authenticateHeader);

    mapper.writeValue(response.getOutputStream(), body);
  }
}
