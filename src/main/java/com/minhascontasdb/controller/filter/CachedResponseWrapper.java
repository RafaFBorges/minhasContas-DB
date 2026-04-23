package com.minhascontasdb.controller.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class CachedResponseWrapper extends HttpServletResponseWrapper {

  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private final ServletOutputStream outputStream = new CachedServletOutputStream(buffer);
  private PrintWriter writer;

  public CachedResponseWrapper(HttpServletResponse response) {
    super(response);
  }

  @Override
  public ServletOutputStream getOutputStream() {
    return outputStream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (writer == null)
      writer = new PrintWriter(buffer);
    return writer;
  }

  @Override
  public void flushBuffer() throws IOException {
    if (writer != null)
      writer.flush();
    buffer.flush();
  }

  public byte[] getBody() {
    return buffer.toByteArray();
  }

  private static class CachedServletOutputStream extends ServletOutputStream {

    private final ByteArrayOutputStream buffer;

    public CachedServletOutputStream(ByteArrayOutputStream buffer) {
      this.buffer = buffer;
    }

    @Override
    public void write(int b) {
      buffer.write(b);
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setWriteListener(WriteListener listener) {
      // não necessário para uso síncrono
    }
  }
}
