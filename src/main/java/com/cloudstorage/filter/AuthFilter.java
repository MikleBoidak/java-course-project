package com.cloudstorage.filter;

import com.cloudstorage.model.User;
import com.cloudstorage.util.JsonUtils;
import com.cloudstorage.util.SessionUtils;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Фильтр аутентификации для защищенных эндпоинтов
 * Проверяет наличие пользователя в сессии
 */
@WebFilter(urlPatterns = {
    "/api/files/*",
    "/api/folders/*",
    "/api/photos",
    "/api/shares/*",
    "/api/search",
    "/api/admin/*"
})
public class AuthFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("AuthFilter инициализирован");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String path = httpReq.getRequestURI();
        
        // Публичные эндпоинты не требуют аутентификации
        if (path.startsWith("/shared/")) {
            chain.doFilter(request, response);
            return;
        }

        // Проверка аутентификации
        User user = SessionUtils.getCurrentUser(httpReq);
        if (user == null) {
            httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResp.setContentType("application/json");
            httpResp.setCharacterEncoding("UTF-8");
            httpResp.getWriter().write(JsonUtils.errorJson("Требуется аутентификация"));
            return;
        }

        // Проверка CSRF для изменяющих запросов (минимальная защита)
        String method = httpReq.getMethod();
        if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
            String referer = httpReq.getHeader("Referer");
            if (referer != null && !referer.startsWith(httpReq.getScheme() + "://" + httpReq.getServerName())) {
                logger.warn("Подозрительный Referer: {} от {}", referer, httpReq.getRemoteAddr());
                // В строгом режиме можно блокировать
                // httpResp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                // return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("AuthFilter уничтожен");
    }
}
