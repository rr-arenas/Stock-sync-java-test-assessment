package com.example.stocksync.dto;

import java.util.UUID;

/**
 * API response DTO for product with current stock.
 */
public record ProductResponse(UUID id, String sku, String name, Integer stockQuantity, String vendor) {
}
