package com.cloudstorage.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // Регистрируем кастомный сериализатор для Instant
        SimpleModule module = new SimpleModule();
        module.addSerializer(Instant.class, new StdSerializer<Instant>(Instant.class) {
            @Override
            public void serialize(Instant value, com.fasterxml.jackson.core.JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value));
            }
        });
        MAPPER.registerModule(module);
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка десериализации JSON", e);
        }
    }

    public static String errorJson(String message) {
        return "{\"error\": \"" + message + "\"}";
    }
}