package com.minhascontasdb.controller.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Order(2)
public class CryptoFilter implements Filter {

  private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

  @Value("${crypto.secret-key}")
  private String secretKey;

  @Value("${crypto.iv}")
  private String iv;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
      chain.doFilter(request, response);
      return;
    }

    CachedRequestWrapper decryptedRequest = new CachedRequestWrapper(httpRequest);
    String encryptedBody = new String(decryptedRequest.getBody(), StandardCharsets.UTF_8);

    if (!encryptedBody.isBlank()) {
      String decryptedBody = decrypt(encryptedBody);
      decryptedRequest.setBody(decryptedBody.getBytes(StandardCharsets.UTF_8));
    }

    CachedResponseWrapper cachedResponse = new CachedResponseWrapper(httpResponse);
    chain.doFilter(decryptedRequest, cachedResponse);

    byte[] responseBody = cachedResponse.getBody();
    if (0 < responseBody.length) {
      String encryptedResponse = encrypt(new String(responseBody, StandardCharsets.UTF_8));
      byte[] encryptedBytes = encryptedResponse.getBytes(StandardCharsets.UTF_8);

      httpResponse.setContentLength(encryptedBytes.length);
      httpResponse.getOutputStream().write(encryptedBytes);
    }
  }

  private String encrypt(String plainText) {
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, buildKey(), buildIv());
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
      throw new RuntimeException("Erro ao criptografar", e);
    }
  }

  private String decrypt(String encryptedText) {
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, buildKey(), buildIv());

      byte[] decoded = Base64.getDecoder().decode(encryptedText);

      return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Erro ao descriptografar", e);
    }
  }

  private SecretKeySpec buildKey() {
    return new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
  }

  private IvParameterSpec buildIv() {
    return new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
  }
}
