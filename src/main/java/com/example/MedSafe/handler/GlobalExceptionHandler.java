package com.example.MedSafe.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleGeneralException(RuntimeException ex) {
        return new ResponseEntity<>("Ошибка сервера: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
