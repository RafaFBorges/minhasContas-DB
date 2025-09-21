package com.minhascontasdb.filter;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

public class CorsFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Inicialização do filtro (opcional)
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Adiciona os cabeçalhos de CORS
        httpResponse.setHeader("Access-Control-Allow-Origin", "*"); // Permite qualquer origem
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS"); // Métodos permitidos
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization"); // Cabeçalhos permitidos
        
        // Continua o processamento da requisição
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Limpeza do filtro (opcional)
    }
}
