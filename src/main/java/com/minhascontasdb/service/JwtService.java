package com.minhascontasdb.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

// Emite e valida o access token (JWT HS256, curto). Quem renova a sessão é o refresh token (RefreshTokenService)
@Service
public class JwtService {
  public static final String ISSUER = "minhascontas-db";
  public static final String CLAIM_ROLE = "role";
  public static final String CLAIM_USERNAME = "username";
  // HS256 pede uma chave de pelo menos 256 bits
  public static final int MIN_SECRET_BYTES = 32;
  private static final int MIN_DISTINCT_SECRET_CHARS = 10;

  public record AccessToken(String value, Instant expiresAt) {
  }

  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final Duration accessTtl;
  private final Clock clock;

  @Autowired
  public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.access-token-minutes:15}") long accessMinutes) {
    this(secret, Duration.ofMinutes(accessMinutes), Clock.systemUTC());
  }

  // Única definição de segredo aceitável (usada também pelo RefreshTokenService). Barra o óbvio ("aaaa...", "abababab..."); não mede
  // entropia de verdade, por isso o README manda gerar com openssl. A mensagem nunca traz o valor
  public static byte[] requireStrongSecret(String secret) {
    byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < MIN_SECRET_BYTES)
      throw new IllegalStateException("JWT_SECRET must have at least " + MIN_SECRET_BYTES + " bytes");
    if (secret.chars().distinct().count() < MIN_DISTINCT_SECRET_CHARS)
      throw new IllegalStateException("JWT_SECRET is too repetitive; generate one with: openssl rand -base64 48");

    return bytes;
  }

  public JwtService(String secret, Duration accessTtl, Clock clock) {
    byte[] bytes = requireStrongSecret(secret);
    if (accessTtl.isZero() || accessTtl.isNegative())
      throw new IllegalStateException("jwt.access-token-minutes must be positive");

    SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
    this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));

    // Sem tolerância de relógio: um token vencido é recusado na hora
    JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
    timestamps.setClock(clock);
    NimbusJwtDecoder nimbus = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator(ISSUER)));
    this.decoder = nimbus;

    this.accessTtl = accessTtl;
    this.clock = clock;
  }

  public AccessToken issue(User user) {
    Instant now = clock.instant();
    // O claim exp só tem segundos inteiros e o validador não tem tolerância: o expiresAt devolvido ao cliente tem que ser o mesmo
    // instante, senão quem renova exatamente nele manda um token vencido até 1 s antes
    Instant expiresAt = now.plus(accessTtl).truncatedTo(ChronoUnit.SECONDS);

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(ISSUER)
        .subject(String.valueOf(user.getId()))
        .issuedAt(now)
        .expiresAt(expiresAt)
        .claim(CLAIM_USERNAME, user.getUser())
        .claim(CLAIM_ROLE, user.getRole().name())
        .build();

    String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();

    return new AccessToken(token, expiresAt);
  }

  public JwtDecoder decoder() {
    return decoder;
  }
}
