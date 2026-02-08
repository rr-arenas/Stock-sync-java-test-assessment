package com.example.stocksync.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
		log.warn("Bad request: {}", e.getMessage());
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(errorBody(HttpStatus.BAD_REQUEST, e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
		log.error("Unhandled error", e);
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
	}

	private Map<String, Object> errorBody(HttpStatus status, String message) {
		return Map.of(
				"timestamp", Instant.now().toString(),
				"status", status.value(),
				"error", status.getReasonPhrase(),
				"message", message != null ? message : ""
		);
	}
}
