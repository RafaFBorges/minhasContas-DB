package com.minhascontasdb.filter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS fechado por padrão. Quem chama a API em produção é o servidor do Web (/api do Next), de servidor para servidor, e isso não
// passa por CORS: o navegador nunca fala com a API. Só se libera uma origem de navegador quando CORS_ALLOWED_ORIGINS a lista
// (ex.: http://localhost:3000 para um front local). Vazia, nenhuma resposta leva Access-Control-Allow-Origin.
@Configuration
public class CorsConfig {
  private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

  @Bean
  public WebMvcConfigurer corsConfigurer(@Value("${cors.allowed-origins:}") String allowedOrigins) {
    List<String> origins = parseOrigins(allowedOrigins);

    if (origins.isEmpty())
      log.info("CORS: fechado (CORS_ALLOWED_ORIGINS vazia). Nenhuma origem de navegador é liberada");
    else
      log.info("CORS: origens liberadas = {}", origins);

    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        if (origins.isEmpty())
          return;

        registry.addMapping("/**")
            .allowedOrigins(origins.toArray(String[]::new))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type", "Accept")
            // Fora os cabeçalhos "simples", o navegador só deixa o JavaScript ler o que for exposto aqui. O front precisa do
            // Retry-After no 429 do login, e do WWW-Authenticate para distinguir token vencido de token ausente
            .exposedHeaders("Retry-After", "WWW-Authenticate");
      }
    };
  }

  // Lista separada por vírgula. Cada origem precisa ser exatamente scheme://host[:port]: o navegador compara o texto da origem
  // sem path nem barra final, então qualquer outra forma subiria "funcionando" e responderia 403 a todas as requisições.
  // "*" também é recusado: liberar todas as origens é justamente o que se fechou
  static List<String> parseOrigins(String raw) {
    List<String> origins = Arrays.stream((raw == null ? "" : raw).split(","))
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .toList();

    for (String origin : origins)
      validateOrigin(origin);

    return origins;
  }

  private static void validateOrigin(String origin) {
    if (origin.contains("*"))
      throw new IllegalStateException("CORS_ALLOWED_ORIGINS não aceita curinga (*). Liste as origens, ex.: http://localhost:3000");

    URI uri;
    try {
      uri = new URI(origin);
    } catch (URISyntaxException e) {
      throw invalid(origin);
    }

    boolean httpScheme = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
    boolean bareOrigin = (uri.getRawPath() == null || uri.getRawPath().isEmpty())
        && uri.getRawQuery() == null && uri.getRawFragment() == null && uri.getRawUserInfo() == null;

    if (!httpScheme || uri.getHost() == null || !bareOrigin)
      throw invalid(origin);
  }

  private static IllegalStateException invalid(String origin) {
    return new IllegalStateException("CORS_ALLOWED_ORIGINS tem uma origem inválida: '" + origin
        + "'. Use scheme://host[:port], sem barra final, caminho ou aspas (ex.: http://localhost:3000)");
  }
}
