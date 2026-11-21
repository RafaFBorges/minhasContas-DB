package com.minhascontasdb.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginRateLimiterTests {
  private static final Duration WINDOW = Duration.ofMinutes(15);
  private static final String IP = "203.0.113.7";

  private MutableClock clock;

  // Relógio que os testes avançam à mão
  private static class MutableClock extends Clock {
    private Instant now = Instant.parse("2026-01-01T00:00:00Z");

    void advance(Duration d) {
      now = now.plus(d);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  @BeforeEach
  void setUp() {
    clock = new MutableClock();
  }

  private LoginRateLimiter limiter(int perUser, int perIp) {
    return new LoginRateLimiter(clock, WINDOW, perUser, perIp);
  }

  private void attempts(LoginRateLimiter limiter, String user, int times) {
    for (int i = 0; i < times; i++)
      limiter.reserveAttempt(user, IP);
  }

  @Test
  void blocksAUserAfterTheLimitAndReportsWhenToRetry() {
    LoginRateLimiter limiter = limiter(3, 0);

    assertEquals(0, limiter.reserveAttempt("ana", IP));
    assertEquals(0, limiter.reserveAttempt("ana", IP));
    assertEquals(0, limiter.reserveAttempt("ana", IP));

    assertEquals(WINDOW.toSeconds(), limiter.reserveAttempt("ana", IP));

    clock.advance(Duration.ofMinutes(5));
    assertEquals(Duration.ofMinutes(10).toSeconds(), limiter.reserveAttempt("ana", IP));
  }

  @Test
  void blockedAttemptsAreNotRecordedSoTheBlockDoesNotExtend() {
    LoginRateLimiter limiter = limiter(2, 0);
    attempts(limiter, "ana", 2);

    for (int i = 0; i < 50; i++)
      assertTrue(0 < limiter.reserveAttempt("ana", IP));

    clock.advance(WINDOW);
    assertEquals(0, limiter.reserveAttempt("ana", IP));
  }

  @Test
  void userKeyIsTheExactAccountName() {
    LoginRateLimiter limiter = limiter(2, 0);
    attempts(limiter, "ana", 2);

    // "Ana", "ANA" e " ana" são contas diferentes de "ana": não compartilham nem consomem o contador dela
    assertEquals(0, limiter.reserveAttempt("Ana", IP));
    assertEquals(0, limiter.reserveAttempt("ANA", IP));
    assertEquals(0, limiter.reserveAttempt(" ana", IP));
    assertTrue(0 < limiter.reserveAttempt("ana", IP));
  }

  @Test
  void successOnAnotherAccountCannotResetTheCounter() {
    LoginRateLimiter limiter = limiter(3, 0);
    attempts(limiter, "victim", 2);

    // Login correto da conta do atacante, com nome parecido
    limiter.reserveAttempt("VICTIM", IP);
    limiter.releaseOnSuccess("VICTIM", IP);

    assertEquals(0, limiter.reserveAttempt("victim", IP));
    assertTrue(0 < limiter.reserveAttempt("victim", IP));
  }

  @Test
  void successResetsTheUserCounterAndReturnsOnlyItsOwnIpReservation() {
    LoginRateLimiter limiter = limiter(2, 3);
    attempts(limiter, "ana", 2);          // 2 reservas no IP
    limiter.releaseOnSuccess("ana", IP);   // zera "ana" e devolve 1 do IP

    assertEquals(0, limiter.reserveAttempt("ana", "198.51.100.1"));

    // Sobrou 1 reserva no IP; com mais 2 chega ao limite de 3
    assertEquals(0, limiter.reserveAttempt("bia", IP));
    assertEquals(0, limiter.reserveAttempt("carla", IP));
    assertTrue(0 < limiter.reserveAttempt("dani", IP));
  }

  @Test
  void ipLimitBlocksAnyUserFromThatIpButNotOtherIps() {
    LoginRateLimiter limiter = limiter(0, 3);
    limiter.reserveAttempt("a", IP);
    limiter.reserveAttempt("b", IP);
    limiter.reserveAttempt("c", IP);

    assertTrue(0 < limiter.reserveAttempt("qualquer", IP));
    assertEquals(0, limiter.reserveAttempt("qualquer", "198.51.100.1"));
  }

  @Test
  void zeroDisablesALimit() {
    LoginRateLimiter limiter = limiter(0, 0);
    attempts(limiter, "ana", 50);

    assertEquals(0, limiter.reserveAttempt("ana", IP));
    assertEquals(0, limiter.trackedUserKeys());
  }

  // O ponto da reserva: pedidos simultâneos não podem passar todos pela checagem
  @Test
  void parallelAttemptsNeverExceedTheLimit() throws Exception {
    LoginRateLimiter limiter = limiter(5, 0);
    int threads = 64;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);

    List<Future<Long>> results = new ArrayList<>();
    for (int i = 0; i < threads; i++)
      results.add(pool.submit(() -> {
        start.await();
        return limiter.reserveAttempt("alvo", IP);
      }));

    start.countDown();
    int allowed = 0;
    for (Future<Long> result : results)
      if (result.get() == 0)
        allowed++;
    pool.shutdown();

    assertEquals(5, allowed);
  }

  @Test
  void trackedKeysHaveAHardCap() {
    LoginRateLimiter limiter = limiter(2, 0);
    attempts(limiter, "vitima", 2);
    assertTrue(0 < limiter.reserveAttempt("vitima", IP));

    for (int i = 0; i < LoginRateLimiter.MAX_TRACKED_KEYS + 100; i++)
      limiter.reserveAttempt("inventado-" + i, IP);

    // A memória não cresce sem limite; o custo conhecido é perder os contadores mais antigos
    assertEquals(LoginRateLimiter.MAX_TRACKED_KEYS, limiter.trackedUserKeys());
    assertEquals(0, limiter.reserveAttempt("vitima", IP));
  }

  @Test
  void hugeKeysAreStillAccepted() {
    LoginRateLimiter limiter = limiter(1, 1);
    String huge = "x".repeat(100_000);

    assertEquals(0, limiter.reserveAttempt(huge, huge));
    assertTrue(0 < limiter.reserveAttempt(huge, huge));
  }

  // Nomes longos diferentes (mesmo prefixo, mesmo tamanho) não podem compartilhar contador
  @Test
  void differentLongNamesHaveIndependentCounters() {
    LoginRateLimiter limiter = limiter(1, 0);
    String prefix = "p".repeat(150);

    assertEquals(0, limiter.reserveAttempt(prefix + "A", IP));
    assertTrue(0 < limiter.reserveAttempt(prefix + "A", IP));

    assertEquals(0, limiter.reserveAttempt(prefix + "B", IP));
  }

  // "Aa" e "BB" têm o mesmo String.hashCode(): um nome longo forjado com essa colisão não pode atingir outro
  @Test
  void longNamesWithTheSameJavaHashCodeDoNotCollide() {
    assertEquals("Aa".hashCode(), "BB".hashCode());

    LoginRateLimiter limiter = limiter(1, 0);
    String prefix = "p".repeat(150);

    assertEquals(0, limiter.reserveAttempt(prefix + "Aa", IP));
    assertEquals(0, limiter.reserveAttempt(prefix + "BB", IP));
  }

  // Quem conhece um nome longo não pode registrar um nome curto que caia na mesma chave dele
  @Test
  void shortNamesCannotImpersonateTheKeyOfALongName() throws Exception {
    LoginRateLimiter limiter = limiter(1, 0);
    String longName = "p".repeat(150);
    String hash = java.util.HexFormat.of().formatHex(
        java.security.MessageDigest.getInstance("SHA-256").digest(longName.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

    assertEquals(0, limiter.reserveAttempt(longName, IP));
    assertTrue(0 < limiter.reserveAttempt(longName, IP));

    // O hash tem 64 caracteres, então cabe num nome normal, com qualquer prefixo
    for (String lookalike : new String[] { hash, "h:" + hash, "n:" + hash, "sha256:" + hash }) {
      assertEquals(0, limiter.reserveAttempt(lookalike, IP), lookalike);
      limiter.releaseOnSuccess(lookalike, IP);
    }

    // E o login "correto" do sósia não zerou o contador do nome longo
    assertTrue(0 < limiter.reserveAttempt(longName, IP));
  }
}
