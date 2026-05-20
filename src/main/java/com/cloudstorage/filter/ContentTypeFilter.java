package com.cloudstorage.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Фильтр для установки заголовков безопасности и кодировки
 */
@WebFilter(urlPatterns = "/*")
public class ContentTypeFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(ContentTypeFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("ContentTypeFilter инициализирован");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        // Установка заголовков безопасности
        httpResp.setHeader("X-Content-Type-Options", "nosniff");
        httpResp.setHeader("X-Frame-Options", "DENY");
        httpResp.setHeader("X-XSS-Protection", "1; mode=block");
        httpResp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Установка кодировки для JSON ответов
        String contentType = httpReq.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            httpReq.setCharacterEncoding("UTF-8");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("ContentTypeFilter уничтожен");
    }
}
