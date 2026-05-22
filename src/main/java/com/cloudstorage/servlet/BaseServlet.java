package com.cloudstorage.servlet;

import com.cloudstorage.util.JsonUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Базовый сервлет с общими методами обработки ошибок
 */
public abstract class BaseServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(BaseServlet.class);

    protected void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(JsonUtils.errorJson(message));
    }

    protected void handleError(HttpServletResponse resp, Exception e) throws IOException {
        if (e instanceof com.cloudstorage.exception.AppException appEx) {
            resp.setStatus(appEx.getStatusCode());
            resp.getWriter().write(JsonUtils.errorJson(appEx.getMessage()));
        } else {
            logger.error("Ошибка в {}: {}", getClass().getSimpleName(), e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtils.errorJson("Внутренняя ошибка сервера"));
        }
    }
}
