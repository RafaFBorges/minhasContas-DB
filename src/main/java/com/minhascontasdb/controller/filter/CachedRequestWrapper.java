package com.minhascontasdb.controller.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CachedRequestWrapper extends HttpServletRequestWrapper {

  private byte[] body;

  public CachedRequestWrapper(HttpServletRequest request) throws IOException {
    super(request);
    this.body = request.getInputStream().readAllBytes();
  }

  public byte[] getBody() {
    return body;
  }

  public void setBody(byte[] body) {
    this.body = body;
  }

  @Override
  public ServletInputStream getInputStream() {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);

    return new ServletInputStream() {
      @Override
      public int read() {
        return byteArrayInputStream.read();
      }

      @Override
      public boolean isFinished() {
        return byteArrayInputStream.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener listener) {
      }
    };
  }
}