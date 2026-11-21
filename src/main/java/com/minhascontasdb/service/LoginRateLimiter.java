package com.minhascontasdb.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Limita tentativas de login, por usuário e (opcionalmente) por IP, numa janela deslizante.
//
// - Cada tentativa é RESERVADA antes de conferir a senha (o hash demora, então registrar só depois deixaria
//   requisições paralelas passarem todas pela checagem) e devolvida se o login der certo.
// - A chave do usuário é o nome exato, igual à identidade da conta: "Ana" e "ana" são contas diferentes, e nada
//   de uma conta pode zerar ou consumir o contador de outra.
// - Fica em memória: vale para uma única instância da API e zera a cada reinício. Com mais de uma instância, o
//   contador precisa ir para o banco. O número de chaves guardadas tem teto, para nomes inventados não esgotarem a memória
//   (o preço é que quem inventa milhares de nomes pode empurrar contadores antigos para fora)
// - Por usuário: segura quem adivinha a senha de uma conta. Alguém pode, de propósito, travar a conta de outra pessoa
//   por até 'window' minutos. É o custo conhecido deste desenho.
// - Por IP: segura quem testa uma senha em muitas contas. Vem DESLIGADO porque atrás do proxy o IP da requisição
//   não é confiável: com server.forward-headers-strategy=framework, getRemoteAddr() é a primeira entrada do
//   X-Forwarded-For, que o próprio cliente escreve. Só ligue depois de passar para um mecanismo que confie apenas nos
//   proxies da hospedagem (forward-headers-strategy=native com remoteip do Tomcat)
@Component
public class LoginRateLimiter {
  static final int MAX_TRACKED_KEYS = 10_000;
  private static final int MAX_KEY_LENGTH = 100;

  private final Clock clock;
  private final Duration window;
  private final int maxAttemptsPerUser;
  private final int maxAttemptsPerIp;

  private final Map<String, Deque<Instant>> attemptsByUser = boundedMap();
  private final Map<String, Deque<Instant>> attemptsByIp = boundedMap();

  @Autowired
  public LoginRateLimiter(
      @Value("${login.rate-limit.window-minutes:15}") long windowMinutes,
      @Value("${login.rate-limit.max-failures-per-user:5}") int maxFailuresPerUser,
      @Value("${login.rate-limit.max-failures-per-ip:0}") int maxFailuresPerIp) {
    this(Clock.systemUTC(), Duration.ofMinutes(windowMinutes), maxFailuresPerUser, maxFailuresPerIp);
  }

  public LoginRateLimiter(Clock clock, Duration window, int maxFailuresPerUser, int maxFailuresPerIp) {
    this.clock = clock;
    this.window = window;
    this.maxAttemptsPerUser = maxFailuresPerUser;
    this.maxAttemptsPerIp = maxFailuresPerIp;
  }

  // Confere e reserva numa só operação. Devolve 0 se a tentativa foi reservada (pode conferir a senha) ou os
  // segundos até poder tentar de novo (nada é reservado)
  public synchronized long reserveAttempt(String user, String ip) {
    Instant now = clock.instant();
    String userKey = key(user);
    String ipKey = key(ip);

    long wait = Math.max(
        secondsUntilAllowed(attemptsByUser, userKey, maxAttemptsPerUser, now),
        secondsUntilAllowed(attemptsByIp, ipKey, maxAttemptsPerIp, now));
    if (0 < wait)
      return wait;

    record(attemptsByUser, userKey, maxAttemptsPerUser, now);
    record(attemptsByIp, ipKey, maxAttemptsPerIp, now);
    return 0;
  }

  // Login correto: zera o contador da conta e devolve a reserva do IP. As outras tentativas do IP continuam
  // contando, para não dar para "lavar" tentativas com uma conta própria
  public synchronized void releaseOnSuccess(String user, String ip) {
    attemptsByUser.remove(key(user));

    Deque<Instant> byIp = attemptsByIp.get(key(ip));
    if (byIp != null) {
      byIp.pollLast();
      if (byIp.isEmpty())
        attemptsByIp.remove(key(ip));
    }
  }

  synchronized int trackedUserKeys() {
    return attemptsByUser.size();
  }

  // Nome exato (a identidade da conta). Acima de MAX_KEY_LENGTH vira o SHA-256 do nome inteiro, para uma chave enorme
  // não ocupar memória sem que dois nomes diferentes possam ser forjados para compartilhar o mesmo contador.
  // Os prefixos separam as duas famílias: nenhum nome digitado (sempre "n:...") se confunde com uma chave de hash ("h:...")
  private static String key(String value) {
    String key = value == null ? "" : value;
    return key.length() > MAX_KEY_LENGTH ? "h:" + sha256Hex(key) : "n:" + key;
  }

  private static String sha256Hex(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      // Todo JDK é obrigado a ter SHA-256
      throw new IllegalStateException(e);
    }
  }

  private static Map<String, Deque<Instant>> boundedMap() {
    return new LinkedHashMap<>() {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Deque<Instant>> eldest) {
        return size() > MAX_TRACKED_KEYS;
      }
    };
  }

  private long secondsUntilAllowed(Map<String, Deque<Instant>> attempts, String key, int max, Instant now) {
    if (max <= 0)
      return 0;

    Deque<Instant> recent = attempts.get(key);
    if (recent == null)
      return 0;

    prune(recent, now);
    if (recent.isEmpty()) {
      attempts.remove(key);
      return 0;
    }

    if (recent.size() < max)
      return 0;

    // Libera quando a tentativa mais antiga da janela sair dela
    long seconds = Duration.between(now, recent.peekFirst().plus(window)).toSeconds();
    return Math.max(1, seconds);
  }

  private void record(Map<String, Deque<Instant>> attempts, String key, int max, Instant now) {
    if (max <= 0)
      return;

    attempts.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(now);
  }

  private void prune(Deque<Instant> recent, Instant now) {
    Instant limit = now.minus(window);
    while (!recent.isEmpty() && !recent.peekFirst().isAfter(limit))
      recent.removeFirst();
  }
}
