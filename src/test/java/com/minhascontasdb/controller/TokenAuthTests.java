package com.minhascontasdb.controller;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhascontasdb.inicialization.UserInitialDataEnsurer;
import com.minhascontasdb.persistence.RefreshTokenPersistence;
import com.minhascontasdb.persistence.UserPersistence;
import com.minhascontasdb.service.JwtService;
import com.minhascontasdb.service.RefreshTokenService;
import com.minhascontasdb.service.Role;
import com.minhascontasdb.service.User;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Access token (JWT) + refresh token rotativo
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class TokenAuthTests {
  private static final String PASSWORD = "S3nha-forte";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserPersistence userPersistence;

  @Autowired
  private RefreshTokenPersistence refreshTokenPersistence;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private RefreshTokenService refreshTokens;

  @Value("${jwt.secret}")
  private String secret;

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  private record Tokens(Long userId, String access, String refresh) {
  }

  private User newUser(Role role) {
    String username = "tk-" + UUID.randomUUID().toString().substring(0, 8);
    User user = new User("Token", username + "@test.com", passwordEncoder.encode(PASSWORD), username);
    user.setRole(role);

    return userPersistence.save(user);
  }

  private Tokens login(User user) throws Exception {
    String response = mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("user", user.getUser(), "password", PASSWORD))))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    JsonNode json = objectMapper.readTree(response);

    return new Tokens(user.getId(), json.get("accessToken").asText(), json.get("refreshToken").asText());
  }

  private RefreshTokenService serviceWith(Clock clock, Duration grace, int maxSessions) {
    return new RefreshTokenService(refreshTokenPersistence, userPersistence, secret, Duration.ofDays(7), Duration.ofDays(30), grace, maxSessions, clock);
  }

  private ResultActions refresh(String refreshToken) throws Exception {
    return mockMvc.perform(post("/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))));
  }

  private ResultActions listExpenses(Long userId, String bearer) throws Exception {
    return mockMvc.perform(get("/expense/user/" + userId).header("Authorization", bearer));
  }

  // ---------- rotas protegidas ----------

  @Test
  void protectedRouteWithoutTokenIsUnauthorized() throws Exception {
    mockMvc.perform(get("/expense/user/1"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void validAccessTokenGrantsAccess() throws Exception {
    Tokens tokens = login(newUser(Role.USER));

    listExpenses(tokens.userId(), "Bearer " + tokens.access()).andExpect(status().isOk());
  }

  @Test
  void malformedAndNonBearerAuthorizationAreUnauthorized() throws Exception {
    Tokens tokens = login(newUser(Role.USER));

    // Token recusado leva error="invalid_token"; sem token (teste acima) o cabeçalho é só "Bearer"
    listExpenses(tokens.userId(), "Bearer lixo").andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer error=\"invalid_token\""));
    listExpenses(tokens.userId(), "Bearer ").andExpect(status().isUnauthorized());
    listExpenses(tokens.userId(), "Basic dXNlcjpwYXNz").andExpect(status().isUnauthorized());
    // O refresh token não é um access token
    listExpenses(tokens.userId(), "Bearer " + tokens.refresh()).andExpect(status().isUnauthorized());
  }

  @Test
  void oldHeaderNameIsNoLongerAccepted() throws Exception {
    Tokens tokens = login(newUser(Role.USER));

    mockMvc.perform(get("/expense/user/" + tokens.userId()).header("token", tokens.access()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void expiredAccessTokenIsUnauthorized() throws Exception {
    User user = newUser(Role.USER);
    // Mesmo segredo, mas com o relógio 1 hora atrás: o token já nasce vencido
    Clock past = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
    String expired = new JwtService(secret, Duration.ofMinutes(15), past).issue(user).value();

    listExpenses(user.getId(), "Bearer " + expired).andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer error=\"invalid_token\""));
  }

  @Test
  void tokenSignedWithAnotherSecretIsUnauthorized() throws Exception {
    User user = newUser(Role.USER);
    String forged = new JwtService("outro-segredo-com-mais-de-trinta-e-dois-bytes-aaaa", Duration.ofMinutes(15), Clock.systemUTC())
        .issue(user).value();

    listExpenses(user.getId(), "Bearer " + forged).andExpect(status().isUnauthorized());
  }

  @Test
  void tokenWithAlteredPayloadIsUnauthorized() throws Exception {
    Tokens tokens = login(newUser(Role.USER));
    String[] parts = tokens.access().split("\\.");
    // Troca o papel no payload mantendo a assinatura original
    String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
    String altered = java.util.Base64.getUrlEncoder().withoutPadding()
        .encodeToString(payload.replace("\"USER\"", "\"ADMIN\"").getBytes());
    assertNotEquals(parts[1], altered);

    listExpenses(tokens.userId(), "Bearer " + parts[0] + "." + altered + "." + parts[2]).andExpect(status().isUnauthorized());
  }

  @Test
  void unsignedTokenIsUnauthorized() throws Exception {
    Tokens tokens = login(newUser(Role.USER));
    java.util.Base64.Encoder enc = java.util.Base64.getUrlEncoder().withoutPadding();
    String header = enc.encodeToString("{\"alg\":\"none\"}".getBytes());
    String payload = enc.encodeToString(("{\"iss\":\"" + JwtService.ISSUER + "\",\"sub\":\"" + tokens.userId()
        + "\",\"role\":\"ADMIN\",\"exp\":" + (Instant.now().getEpochSecond() + 600) + "}").getBytes());

    listExpenses(tokens.userId(), "Bearer " + header + "." + payload + ".").andExpect(status().isUnauthorized());
  }

  // O cabeçalho vencido do navegador/proxy não pode impedir de entrar de novo
  @Test
  void publicRoutesIgnoreAnInvalidAuthorizationHeader() throws Exception {
    User user = newUser(Role.USER);

    mockMvc.perform(post("/login")
        .header("Authorization", "Bearer vencido")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("user", user.getUser(), "password", PASSWORD))))
        .andExpect(status().isOk());
  }

  // ---------- conteúdo do token ----------

  @Test
  void accessTokenCarriesSubjectUsernameRoleAndShortLifetime() throws Exception {
    User admin = newUser(Role.ADMIN);
    Tokens tokens = login(admin);

    JsonNode claims = objectMapper.readTree(java.util.Base64.getUrlDecoder().decode(tokens.access().split("\\.")[1]));
    assertEquals(String.valueOf(admin.getId()), claims.get("sub").asText());
    assertEquals(admin.getUser(), claims.get("username").asText());
    assertEquals("ADMIN", claims.get("role").asText());
    assertEquals(JwtService.ISSUER, claims.get("iss").asText());
    assertEquals(Duration.ofMinutes(15).getSeconds(), claims.get("exp").asLong() - claims.get("iat").asLong());
    // Nada que identifique mais do que o necessário
    assertFalse(claims.has("password"));
    assertFalse(claims.has("email"));
  }

  // ---------- refresh ----------

  @Test
  void refreshReturnsNewTokensAndInvalidatesTheOldRefreshToken() throws Exception {
    Tokens first = login(newUser(Role.USER));

    String body = refresh(first.refresh()).andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andReturn().getResponse().getContentAsString();
    JsonNode second = objectMapper.readTree(body);

    assertNotEquals(first.refresh(), second.get("refreshToken").asText());
    listExpenses(first.userId(), "Bearer " + second.get("accessToken").asText()).andExpect(status().isOk());

    // O token já usado não vale mais
    refresh(first.refresh()).andExpect(status().isUnauthorized());
  }

  @Test
  void reusingARotatedRefreshTokenRevokesTheWholeSession() throws Exception {
    Tokens first = login(newUser(Role.USER));
    String newest = objectMapper.readTree(refresh(first.refresh()).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString()).get("refreshToken").asText();

    // Quem tem uma cópia do token antigo o usa: todas as sessões do usuário caem, inclusive a do token novo
    refresh(first.refresh()).andExpect(status().isUnauthorized());
    refresh(newest).andExpect(status().isUnauthorized());
  }

  @Test
  void refreshTokensOfOtherUsersAreNotAffectedByReuse() throws Exception {
    Tokens a = login(newUser(Role.USER));
    Tokens b = login(newUser(Role.USER));
    refresh(a.refresh()).andExpect(status().isOk());

    refresh(a.refresh()).andExpect(status().isUnauthorized());
    refresh(b.refresh()).andExpect(status().isOk());
  }

  @Test
  void refreshWithInvalidTokensIsUnauthorized() throws Exception {
    refresh("nao-existe").andExpect(status().isUnauthorized());
    refresh("").andExpect(status().isUnauthorized());
    refresh("a".repeat(5000)).andExpect(status().isUnauthorized());
    // Corpo sem o campo
    mockMvc.perform(post("/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void expiredRefreshTokenIsUnauthorized() throws Exception {
    User user = newUser(Role.USER);
    Clock past = Clock.fixed(Instant.now().minus(Duration.ofDays(30)), ZoneOffset.UTC);
    // Emitido há 30 dias com validade de 7
    RefreshTokenService old = serviceWith(past, Duration.ZERO, 10);
    String token = old.issue(user.getId()).token();

    refresh(token).andExpect(status().isUnauthorized());
  }

  @Test
  void refreshTokenIsStoredOnlyAsHash() throws Exception {
    Tokens tokens = login(newUser(Role.USER));

    assertTrue(refreshTokenPersistence.findAll().stream().noneMatch(t -> t.getTokenHash().equals(tokens.refresh())));
    assertTrue(refreshTokenPersistence.findAll().stream()
        .anyMatch(t -> t.getUserId().equals(tokens.userId()) && t.getTokenHash().length() == 64));
  }

  @Test
  void refreshTokenOfADeletedUserIsUnauthorized() throws Exception {
    Tokens tokens = login(newUser(Role.USER));
    userPersistence.deleteById(tokens.userId());

    refresh(tokens.refresh()).andExpect(status().isUnauthorized());
  }

  @Test
  void systemAccountNeverGetsARefreshedSession() throws Exception {
    User system = userPersistence.findByUser(UserInitialDataEnsurer.SYSTEM_USERNAME).orElseThrow();
    assertEquals(Role.SYSTEM, system.getRole());

    // Mesmo com um refresh token emitido por engano, ele não é trocado
    String token = serviceWith(Clock.systemUTC(), Duration.ZERO, 10)
        .issue(system.getId()).token();

    refresh(token).andExpect(status().isUnauthorized());
  }

  @Test
  void parallelRefreshesWithTheSameTokenYieldOnlyOneSession() throws Exception {
    Tokens tokens = login(newUser(Role.USER));
    int threads = 6;
    java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
    java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
    java.util.List<java.util.concurrent.Future<Integer>> results = new java.util.ArrayList<>();

    for (int i = 0; i < threads; i++)
      results.add(pool.submit(() -> {
        start.await();
        return refresh(tokens.refresh()).andReturn().getResponse().getStatus();
      }));
    start.countDown();

    int ok = 0;
    for (java.util.concurrent.Future<Integer> r : results)
      if (r.get() == 200)
        ok++;
    pool.shutdown();

    assertEquals(1, ok);
  }

  // ---------- logout ----------

  @Test
  void logoutRevokesTheRefreshToken() throws Exception {
    Tokens tokens = login(newUser(Role.USER));

    mockMvc.perform(post("/logout").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("refreshToken", tokens.refresh()))))
        .andExpect(status().isNoContent());

    refresh(tokens.refresh()).andExpect(status().isUnauthorized());
  }

  @Test
  void logoutIsIdempotentAndIgnoresUnknownTokens() throws Exception {
    Tokens tokens = login(newUser(Role.USER));
    String body = objectMapper.writeValueAsString(Map.of("refreshToken", tokens.refresh()));

    for (int i = 0; i < 2; i++)
      mockMvc.perform(post("/logout").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isNoContent());

    mockMvc.perform(post("/logout").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"qualquer\"}"))
        .andExpect(status().isNoContent());
  }

  @Test
  void logoutOnlyEndsThatSession() throws Exception {
    User user = newUser(Role.USER);
    Tokens phone = login(user);
    Tokens laptop = login(user);

    mockMvc.perform(post("/logout").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("refreshToken", phone.refresh()))))
        .andExpect(status().isNoContent());

    refresh(phone.refresh()).andExpect(status().isUnauthorized());
    refresh(laptop.refresh()).andExpect(status().isOk());
  }

  // ---------- limpeza ----------

  @Test
  void purgeRemovesExpiredAndLongRotatedRefreshTokensButKeepsRecentOnes() throws Exception {
    User gone = newUser(Role.USER);
    Instant now = Instant.now();
    // Emitido há 30 dias (vencido)
    serviceAt(now.minus(Duration.ofDays(30)), Duration.ZERO).issue(gone.getId());
    // Trocado há 2 dias (guardado só para detectar cópia: já passou da retenção) e trocado agora (ainda vale guardar)
    User rotated = newUser(Role.USER);
    String old = serviceAt(now.minus(Duration.ofDays(2)), Duration.ZERO).issue(rotated.getId()).token();
    serviceAt(now.minus(Duration.ofDays(2)), Duration.ZERO).rotate(old).orElseThrow();
    User recent = newUser(Role.USER);
    String young = serviceAt(now, Duration.ZERO).issue(recent.getId()).token();
    serviceAt(now, Duration.ZERO).rotate(young).orElseThrow();

    refreshTokens.purgeStale();

    assertEquals(0, rowsOf(gone.getId()));
    // Do usuário "rotated" sobra só o sucessor ativo; do "recent", o token trocado e o sucessor
    assertEquals(1, rowsOf(rotated.getId()));
    assertEquals(2, rowsOf(recent.getId()));
  }

  private long rowsOf(Long userId) {
    return refreshTokenPersistence.findAll().stream().filter(t -> t.getUserId().equals(userId)).count();
  }

  // ---------- teto de sessões ----------

  @Test
  void loginsBeyondTheSessionCapEndTheOldestSessions() {
    User user = newUser(Role.USER);
    Instant now = Instant.now();
    RefreshTokenService service = serviceAt(now, Duration.ZERO, 3);
    java.util.List<String> issued = new java.util.ArrayList<>();

    for (int i = 0; i < 5; i++)
      issued.add(service.issue(user.getId()).token());

    assertEquals(3, activeTokensOf(user.getId()));
    assertTrue(service.rotate(issued.get(0)).isEmpty());
    assertTrue(service.rotate(issued.get(1)).isEmpty());
    assertTrue(service.rotate(issued.get(4)).isPresent());
  }

  // O teto derruba a sessão parada há mais tempo, não a mais antiga: quem renova todo dia não perde a sessão
  @Test
  void theSessionCapKeepsTheRecentlyUsedSessionAndDropsTheIdleOne() {
    User user = newUser(Role.USER);
    RefreshTokenService service = serviceAt(Instant.now(), Duration.ZERO, 2);

    String used = service.issue(user.getId()).token();
    String idle = service.issue(user.getId()).token();
    String usedRenewed = service.rotate(used).orElseThrow().refresh().token();
    String newest = service.issue(user.getId()).token();

    assertTrue(service.rotate(idle).isEmpty());
    assertTrue(service.rotate(usedRenewed).isPresent());
    assertTrue(service.rotate(newest).isPresent());
  }

  // Expulsar uma sessão apaga a cadeia toda: o token antigo dela, se o cliente expulso ainda o usar, não pode passar por reuso
  // e encerrar as outras sessões do usuário
  @Test
  void evictingASessionRemovesItsWholeChainSoAStaleTokenDoesNotWipeTheOthers() {
    User user = newUser(Role.USER);
    RefreshTokenService service = serviceAt(Instant.now(), Duration.ZERO, 2);

    String old = service.issue(user.getId()).token();
    service.rotate(old).orElseThrow();
    String b = service.issue(user.getId()).token();
    String c = service.issue(user.getId()).token();

    assertEquals(2, rowsOf(user.getId()));
    assertTrue(service.rotate(old).isEmpty());
    assertTrue(service.rotate(b).isPresent());
    assertTrue(service.rotate(c).isPresent());
  }

  // Linhas vencidas que a limpeza ainda não apagou não podem tomar o lugar de sessões válidas
  @Test
  void expiredRowsDoNotCountTowardsTheSessionCap() {
    User user = newUser(Role.USER);
    Instant now = Instant.now();
    String expired = serviceAt(now.minus(Duration.ofDays(30)), Duration.ZERO, 2).issue(user.getId()).token();
    RefreshTokenService service = serviceAt(now, Duration.ZERO, 2);

    String a = service.issue(user.getId()).token();
    String b = service.issue(user.getId()).token();

    assertTrue(service.rotate(a).isPresent());
    assertTrue(service.rotate(b).isPresent());
    assertTrue(service.rotate(expired).isEmpty());
  }

  @Test
  void aLoginLoopDoesNotGrowTheTableBeyondTheCap() throws Exception {
    User user = newUser(Role.USER);

    for (int i = 0; i < 25; i++)
      login(user);

    // O teto padrão é 10
    assertEquals(10, activeTokensOf(user.getId()));
    assertEquals(10, rowsOf(user.getId()));
  }

  // ---------- tolerância ao repetir um token recém-trocado ----------

  private RefreshTokenService serviceAt(Instant now, Duration grace, int maxSessions) {
    return serviceWith(Clock.fixed(now, ZoneOffset.UTC), grace, maxSessions);
  }

  private RefreshTokenService serviceAt(Instant now, Duration grace) {
    return serviceAt(now, grace, 10);
  }

  private long activeTokensOf(Long userId) {
    return refreshTokenPersistence.findAll().stream()
        .filter(t -> t.getUserId().equals(userId) && t.getRevokedAt() == null).count();
  }

  private static final Duration GRACE = Duration.ofSeconds(10);

  @Test
  void retryInsideTheGraceWindowReturnsTheSameSuccessorAndCreatesNothing() {
    User user = newUser(Role.USER);
    Instant t0 = Instant.now();
    String first = serviceAt(t0, GRACE).issue(user.getId()).token();
    // A troca acontece, mas a resposta se perde
    RefreshTokenService.Issued second = serviceAt(t0, GRACE).rotate(first).orElseThrow().refresh();
    long rows = rowsOf(user.getId());

    // O cliente repete o token antigo 5 s depois: recebe o MESMO sucessor, e nada novo é gravado
    RefreshTokenService.Issued again = serviceAt(t0.plusSeconds(5), GRACE).rotate(first).orElseThrow().refresh();

    assertEquals(second.token(), again.token());
    assertEquals(second.expiresAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
        again.expiresAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    assertEquals(rows, rowsOf(user.getId()));
    assertEquals(1, activeTokensOf(user.getId()));
  }

  // Quem copiou o token e quem o tem de direito dividem a mesma cadeia: nenhuma sessão paralela nasce, e o reuso tardio é pego
  @Test
  void aCopiedTokenUsedInsideTheWindowDoesNotStartAnIndependentChain() {
    User user = newUser(Role.USER);
    Instant t0 = Instant.now();
    String first = serviceAt(t0, GRACE).issue(user.getId()).token();
    String legit = serviceAt(t0, GRACE).rotate(first).orElseThrow().refresh().token();
    String thief = serviceAt(t0.plusSeconds(3), GRACE).rotate(first).orElseThrow().refresh().token();
    assertEquals(legit, thief);

    // Ambos seguem a mesma cadeia: o próximo passo de um dá o mesmo resultado para o outro
    RefreshTokenService.Issued next = serviceAt(t0.plusSeconds(4), GRACE).rotate(legit).orElseThrow().refresh();
    assertEquals(next.token(), serviceAt(t0.plusSeconds(5), GRACE).rotate(thief).orElseThrow().refresh().token());
    assertEquals(1, activeTokensOf(user.getId()));

    // Passada a janela, qualquer um que reapresente um token antigo derruba tudo
    assertTrue(serviceAt(t0.plusSeconds(60), GRACE).rotate(first).isEmpty());
    assertEquals(0, activeTokensOf(user.getId()));
    assertTrue(serviceAt(t0.plusSeconds(61), GRACE).rotate(next.token()).isEmpty());
  }

  @Test
  void successorOfATokenCannotBeComputedFromTheTokenAlone() {
    User user = newUser(Role.USER);
    Instant t0 = Instant.now();
    String first = serviceAt(t0, GRACE).issue(user.getId()).token();
    String second = serviceAt(t0, GRACE).rotate(first).orElseThrow().refresh().token();

    // Outro servidor (outro segredo) não chega ao mesmo sucessor
    RefreshTokenService other = new RefreshTokenService(refreshTokenPersistence, userPersistence,
        "outro-segredo-com-mais-de-trinta-e-dois-bytes-aaaa", Duration.ofDays(7), Duration.ofDays(30), GRACE, 10,
        Clock.fixed(t0.plusSeconds(1), ZoneOffset.UTC));
    // O token trocado foi gravado com o sucessor do segredo certo: com o segredo errado o sucessor não existe
    assertTrue(other.rotate(first).isEmpty());
    assertNotEquals(first, second);
  }

  @Test
  void reuseAfterTheGraceWindowRevokesEverySessionOfTheUser() {
    User user = newUser(Role.USER);
    Instant t0 = Instant.now();
    String first = serviceAt(t0, GRACE).issue(user.getId()).token();
    String second = serviceAt(t0, GRACE).rotate(first).orElseThrow().refresh().token();

    assertTrue(serviceAt(t0.plusSeconds(11), GRACE).rotate(first).isEmpty());

    assertEquals(0, rowsOf(user.getId()));
    assertTrue(serviceAt(t0.plusSeconds(12), GRACE).rotate(second).isEmpty());
  }

  // Logout (ou encerramento das sessões) logo depois da troca: repetir o token antigo não pode trazer a sessão de volta
  @Test
  void retryInsideTheWindowDoesNotResurrectALoggedOutSession() {
    User user = newUser(Role.USER);
    Instant t0 = Instant.now();
    String first = serviceAt(t0, GRACE).issue(user.getId()).token();
    String second = serviceAt(t0, GRACE).rotate(first).orElseThrow().refresh().token();

    serviceAt(t0.plusSeconds(1), GRACE).revoke(second);

    assertTrue(serviceAt(t0.plusSeconds(2), GRACE).rotate(first).isEmpty());
    assertEquals(0, activeTokensOf(user.getId()));
  }

  // Replay de um token já trocado e vencido não pode derrubar as sessões que estão valendo
  @Test
  void replayOfAnExpiredRotatedTokenIsIgnoredAndKeepsOtherSessions() {
    User user = newUser(Role.USER);
    Instant t0 = Instant.now();
    String old = serviceAt(t0, GRACE).issue(user.getId()).token();
    serviceAt(t0, GRACE).rotate(old).orElseThrow();
    // Outra sessão, aberta 6 dias depois, ainda válida aos 8 dias
    String other = serviceAt(t0.plus(Duration.ofDays(6)), GRACE).issue(user.getId()).token();

    assertTrue(serviceAt(t0.plus(Duration.ofDays(8)), GRACE).rotate(old).isEmpty());

    assertTrue(serviceAt(t0.plus(Duration.ofDays(8)), GRACE).rotate(other).isPresent());
  }

  // T -> S -> S2 e depois um retry de T: S já foi gasto, então não pode ser entregue (seria lido como reuso depois)
  @Test
  void retryDoesNotHandOutASuccessorThatWasAlreadySpent() {
    User user = newUser(Role.USER);
    Instant t0 = Instant.now();
    String t = serviceAt(t0, GRACE).issue(user.getId()).token();
    String s1 = serviceAt(t0, GRACE).rotate(t).orElseThrow().refresh().token();
    String s2 = serviceAt(t0.plusSeconds(3), GRACE).rotate(s1).orElseThrow().refresh().token();

    assertTrue(serviceAt(t0.plusSeconds(5), GRACE).rotate(t).isEmpty());

    // Sem encerrar as sessões: o sucessor atual da cadeia segue valendo
    assertEquals(1, activeTokensOf(user.getId()));
    assertTrue(serviceAt(t0.plusSeconds(6), GRACE).rotate(s2).isPresent());
  }

  // O cliente perdeu a resposta da troca e faz logout com o token antigo: a sessão (o sucessor) também tem que sair
  @Test
  void logoutWithAnAlreadyRotatedTokenEndsTheWholeChain() {
    User user = newUser(Role.USER);
    Instant t0 = Instant.now();
    RefreshTokenService service = serviceAt(t0, GRACE);
    String first = service.issue(user.getId()).token();
    String otherDevice = service.issue(user.getId()).token();
    String successor = service.rotate(first).orElseThrow().refresh().token();

    service.revoke(first);

    assertTrue(service.rotate(successor).isEmpty());
    assertTrue(service.rotate(first).isEmpty());
    // Outro dispositivo (outra cadeia) não é afetado
    assertTrue(service.rotate(otherDevice).isPresent());
  }

  // Usar todo dia não basta: a cadeia acaba 30 dias depois do login
  @Test
  void aChainEndsAtItsAbsoluteLifetimeEvenWhenRotatedRegularly() {
    User user = newUser(Role.USER);
    Instant t0 = Instant.now();
    String token = serviceAt(t0, GRACE).issue(user.getId()).token();

    // Troca a cada 5 dias: cada token valeria 7 dias, mas nenhum passa de t0 + 30 dias
    for (int day = 5; day <= 25; day += 5) {
      RefreshTokenService.Issued next = serviceAt(t0.plus(Duration.ofDays(day)), GRACE).rotate(token).orElseThrow().refresh();
      assertTrue(!next.expiresAt().isAfter(t0.plus(Duration.ofDays(30)).plusMillis(1)));
      token = next.token();
    }

    // Aos 29 dias ainda troca, mas o sucessor já nasce vencendo no fim da cadeia
    RefreshTokenService.Issued last = serviceAt(t0.plus(Duration.ofDays(29)), GRACE).rotate(token).orElseThrow().refresh();
    assertEquals(t0.plus(Duration.ofDays(30)).truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
        last.expiresAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));

    assertTrue(serviceAt(t0.plus(Duration.ofDays(30)).plusSeconds(1), GRACE).rotate(last.token()).isEmpty());
  }

  @Test
  void graceWindowDoesNotApplyToLoggedOutOrExpiredTokens() {
    User user = newUser(Role.USER);
    Instant t0 = Instant.now();
    RefreshTokenService service = serviceAt(t0, GRACE);

    String loggedOut = service.issue(user.getId()).token();
    service.revoke(loggedOut);
    assertTrue(service.rotate(loggedOut).isEmpty());

    String old = serviceAt(t0.minus(Duration.ofDays(8)), GRACE).issue(user.getId()).token();
    assertTrue(service.rotate(old).isEmpty());
  }

  @Test
  void zeroGraceKeepsTheStrictRule() {
    User user = newUser(Role.USER);
    Instant t0 = Instant.now();
    String first = serviceAt(t0, Duration.ZERO).issue(user.getId()).token();
    assertTrue(serviceAt(t0, Duration.ZERO).rotate(first).isPresent());

    assertTrue(serviceAt(t0, Duration.ZERO).rotate(first).isEmpty());
  }
}
