package com.cloudstorage.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;

/**
 * Утилиты для работы с JSON
 */
public class JsonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtils() {}

    /**
     * Сериализовать объект в JSON
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации JSON", e);
        }
    }

    /**
     * Десериализовать JSON в объект
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка десериализации JSON", e);
        }
    }

    /**
     * Создать JSON объект ошибки
     */
    public static String errorJson(String message) {
        return toJson(Map.of("error", message));
    }

    /**
     * Создать JSON объект успеха
     */
    public static String successJson(Object data) {
        return toJson(Map.of("success", true, "data", data));
    }
}
