package com.cloudstorage.exception;

/**
 * Исключение при превышении квоты дискового пространства
 */
public class QuotaExceededException extends AppException {
    public QuotaExceededException(String message) {
        super(message, 403);
    }
}
