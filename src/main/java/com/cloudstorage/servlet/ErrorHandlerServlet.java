package com.cloudstorage.servlet;

import com.cloudstorage.util.JsonUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Сервлет обработки ошибок
 */
@WebServlet("/error")
public class ErrorHandlerServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlerServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleError(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleError(req, resp);
    }

    private void handleError(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer statusCode = (Integer) req.getAttribute("jakarta.servlet.error.status_code");
        String message = (String) req.getAttribute("jakarta.servlet.error.message");
        String uri = (String) req.getAttribute("jakarta.servlet.error.request_uri");

        logger.error("Ошибка {}: {} для URI {}", statusCode, message, uri);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(statusCode != null ? statusCode : 500);
        resp.getWriter().write(JsonUtils.errorJson(message != null ? message : "Неизвестная ошибка"));
    }
}
