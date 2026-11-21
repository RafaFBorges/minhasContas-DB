package com.minhascontasdb.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minhascontasdb.persistence.RefreshTokenPersistence;
import com.minhascontasdb.persistence.UserPersistence;

// Refresh tokens opacos, rotativos e de uso único. O valor só existe na resposta que o entrega: no banco fica o hash.
// O token do login é aleatório; o sucessor de cada troca é derivado do token anterior com a chave do servidor
// (HMAC), então repetir um token dentro da tolerância devolve o MESMO sucessor, sem abrir uma segunda cadeia.
// Os métodos devolvem vazio em vez de lançar exceção porque a revogação por reuso precisa ser gravada (uma exceção desfaria a transação)
@Service
public class RefreshTokenService {
  // Um token legítimo tem 43 caracteres: o limite só evita calcular hash de entradas enormes
  private static final int MAX_TOKEN_LENGTH = 200;
  // Quanto tempo um token já trocado continua guardado para detectar a cópia dele (depois disso só dá 401, sem encerrar sessões)
  static final Duration ROTATED_RETENTION = Duration.ofDays(1);
  private static final SecureRandom RANDOM = new SecureRandom();

  public record Issued(String token, Instant expiresAt) {
  }

  public record Rotation(User user, Issued refresh) {
  }

  private final RefreshTokenPersistence tokens;
  private final UserPersistence users;
  // Subchave só para derivar sucessores, separada da que assina o JWT
  private final byte[] successorKey;
  private final Duration ttl;
  private final Duration maxChain;
  private final Duration reuseGrace;
  private final int maxSessions;
  private final Clock clock;

  @Autowired
  public RefreshTokenService(RefreshTokenPersistence tokens, UserPersistence users,
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.refresh-token-days:7}") long days,
      @Value("${jwt.refresh-chain-max-days:30}") long chainMaxDays,
      @Value("${jwt.refresh-reuse-grace-seconds:10}") long graceSeconds,
      @Value("${jwt.max-sessions-per-user:10}") int maxSessions) {
    this(tokens, users, secret, Duration.ofDays(days), Duration.ofDays(chainMaxDays), Duration.ofSeconds(graceSeconds), maxSessions,
        Clock.systemUTC());
  }

  public RefreshTokenService(RefreshTokenPersistence tokens, UserPersistence users, String secret, Duration ttl,
      Duration maxChain, Duration reuseGrace, int maxSessions, Clock clock) {
    // A mesma exigência do JwtService (o construtor público existe para testes)
    byte[] secretBytes = JwtService.requireStrongSecret(secret);
    // Configuração inválida derruba a subida: com prazo zero ou negativo cada refresh token nasceria vencido e todo /refresh daria 401
    if (ttl.isZero() || ttl.isNegative())
      throw new IllegalStateException("jwt.refresh-token-days must be positive");
    if (maxChain.isZero() || maxChain.isNegative())
      throw new IllegalStateException("jwt.refresh-chain-max-days must be positive");
    if (reuseGrace.isNegative())
      throw new IllegalStateException("jwt.refresh-reuse-grace-seconds must not be negative (0 disables the grace)");
    if (maxSessions < 1)
      throw new IllegalStateException("jwt.max-sessions-per-user must be at least 1");

    this.tokens = tokens;
    this.users = users;
    this.successorKey = hmac(secretBytes, "refresh-successor-key");
    this.ttl = ttl;
    this.maxChain = maxChain;
    this.reuseGrace = reuseGrace;
    this.maxSessions = maxSessions;
    this.clock = clock;
  }

  // Nova sessão (login). Passando do teto de sessões por usuário, são encerradas as menos usadas há mais tempo: cada troca cria uma
  // linha de id maior, então a ordem por id é a da última atividade (a sessão usada todo dia fica, a parada há mais tempo sai)
  @Transactional
  public Issued issue(Long userId) {
    Instant now = clock.instant();
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);

    Issued issued = store(userId, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes), now, UUID.randomUUID(), now);

    // Sem trava: logins simultâneos do mesmo usuário podem passar do teto por alguns tokens (o próximo login ajusta)
    List<RefreshToken> active = tokens.findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByIdDesc(userId, now);
    if (maxSessions < active.size())
      tokens.deleteChains(active.subList(maxSessions, active.size()).stream().map(RefreshToken::getChainId).toList());

    return issued;
  }

  // Cada troca concede mais um prazo (ttl), mas a cadeia tem vida total limitada: uma cópia rotacionada para sempre não vale para sempre
  private Issued store(Long userId, String token, Instant now, UUID chainId, Instant chainStartedAt) {
    Instant expiresAt = now.plus(ttl);
    Instant chainEnd = chainStartedAt.plus(maxChain);
    if (chainEnd.isBefore(expiresAt))
      expiresAt = chainEnd;

    tokens.save(new RefreshToken(userId, hash(token), chainId, chainStartedAt, now, expiresAt));

    return new Issued(token, expiresAt);
  }

  // Troca um refresh token por um novo (o antigo deixa de valer). Vazio = token inválido, vencido, já usado ou de conta sem login.
  //
  // Tolerância (jwt.refresh-reuse-grace-seconds): se a resposta da troca se perde, o cliente repete o token antigo. Dentro da
  // janela isso devolve o mesmo sucessor que a primeira troca criou (o mesmo vale para dois pedidos simultâneos); nada novo é
  // criado, então quem copiou o token não ganha uma cadeia própria. Fora da janela, usar de novo um token já trocado indica
  // cópia dele: as sessões do usuário são encerradas.
  // Limite assumido: dentro da janela, quem copiou o token também recebe o sucessor e nada é detectado (o retry legítimo e o
  // replay são indistinguíveis só pelo token). Fechar isso exige vincular o replay a IP/user agent, o que depende do BFF
  @Transactional
  public Optional<Rotation> rotate(String rawToken) {
    if (!isWellFormed(rawToken))
      return Optional.empty();

    Instant now = clock.instant();
    String hash = hash(rawToken);

    RefreshToken stored = tokens.findByTokenHash(hash).orElse(null);
    // Vencido (trocado ou não) não tem o que fazer: um replay antigo não pode encerrar as sessões atuais
    if (stored == null || !stored.getExpiresAt().isAfter(now))
      return Optional.empty();

    boolean alreadyRotated = stored.getRevokedAt() != null;
    if (alreadyRotated && !isWithinGrace(stored.getRevokedAt(), now)) {
      tokens.deleteAllOfUser(stored.getUserId());
      return Optional.empty();
    }

    User user = users.findById(stored.getUserId()).orElse(null);
    if (user == null || user.isSystem())
      return Optional.empty();

    String successor = successorOf(rawToken);

    // Só um pedido consegue gastar o token (e criar o sucessor); quem perde a corrida, ou repete o token, recebe o mesmo sucessor
    Issued issued = !alreadyRotated && tokens.revokeIfActive(hash, now) == 1
        ? store(user.getId(), successor, now, stored.getChainId(), stored.getChainStartedAt())
        : existingSuccessor(stored.getUserId(), successor, now).orElse(null);

    return issued == null ? Optional.empty() : Optional.of(new Rotation(user, issued));
  }

  private boolean isWithinGrace(Instant rotatedAt, Instant now) {
    return !reuseGrace.isZero() && now.isBefore(rotatedAt.plus(reuseGrace));
  }

  // O sucessor tem que existir e estar ativo: se o logout ou o encerramento das sessões o apagou a sessão não ressuscita, e se
  // ele já foi trocado de novo, entregá-lo faria o cliente parecer um reuso mais tarde
  private Optional<Issued> existingSuccessor(Long userId, String successor, Instant now) {
    if (reuseGrace.isZero())
      return Optional.empty();

    return tokens.findByTokenHash(hash(successor))
        .filter(row -> row.getUserId().equals(userId) && row.getRevokedAt() == null && row.getExpiresAt().isAfter(now))
        .map(row -> new Issued(successor, row.getExpiresAt()));
  }

  // Limpeza (sem ela a tabela só cresce: quem para de usar o app nunca mais passa por aqui)
  @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1H")
  @Transactional
  public int purgeStale() {
    Instant now = clock.instant();

    return tokens.deleteStale(now, now.minus(ROTATED_RETENTION));
  }

  // Logout: encerra a sessão deste token, apagando a cadeia dele (mesmo que o token já tenha sido trocado, o sucessor também sai).
  // Não falha se o token não existir ou já tiver sido encerrado.
  // Quem apresenta qualquer token da cadeia (inclusive um antigo ou copiado) encerra a sessão toda. Risco aceito: o token tem 256
  // bits e esse mesmo token, fora da tolerância, já dispara o encerramento das sessões por reuso em /refresh
  // Só os tokens já trocados (rotação) ficam marcados como usados, e é a reapresentação deles que indica cópia
  @Transactional
  public void revoke(String rawToken) {
    if (isWellFormed(rawToken))
      tokens.deleteChainOf(hash(rawToken));
  }

  private static boolean isWellFormed(String rawToken) {
    return rawToken != null && !rawToken.isBlank() && rawToken.length() <= MAX_TOKEN_LENGTH;
  }

  static String hash(String token) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  // HMAC com a chave do servidor: sem ela ninguém calcula o sucessor de um token que copiou
  private String successorOf(String token) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(successorKey, "refresh-successor:" + token));
  }

  private static byte[] hmac(byte[] key, String message) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));

      return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
