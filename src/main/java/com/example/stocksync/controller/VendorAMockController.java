package com.example.stocksync.controller;

import com.example.stocksync.dto.VendorProductRow;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Simulates Vendor A's REST API (same-app mock for development and testing).
 * Loads product data from classpath resource vendor-a/products.json.
 */
@RestController
@RequestMapping("/api/vendor-a")
public class VendorAMockController {

	private static final Logger log = LoggerFactory.getLogger(VendorAMockController.class);
	private static final String PRODUCTS_RESOURCE = "vendor-a/products.json";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@GetMapping("/products")
	public ResponseEntity<List<VendorProductRow>> getProducts() {
		List<VendorProductRow> products = loadFromResource();
		return ResponseEntity.ok(products);
	}

	private List<VendorProductRow> loadFromResource() {
		try {
			ClassPathResource resource = new ClassPathResource(PRODUCTS_RESOURCE);
			try (InputStream in = resource.getInputStream()) {
				return objectMapper.readValue(in, new TypeReference<>() {});
			}
		} catch (IOException e) {
			log.warn("Could not load {}, using fallback data: {}", PRODUCTS_RESOURCE, e.getMessage());
			return List.of(
					new VendorProductRow("ABC123", "Product A", 8),
					new VendorProductRow("LMN789", "Product C", 0)
			);
		}
	}
}
