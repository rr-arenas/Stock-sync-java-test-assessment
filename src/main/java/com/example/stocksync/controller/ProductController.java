package com.example.stocksync.controller;

import com.example.stocksync.domain.Product;
import com.example.stocksync.dto.ProductResponse;
import com.example.stocksync.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
@Tag(name = "Products", description = "Product stock API")
public class ProductController {

	private final ProductRepository productRepository;

	public ProductController(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@GetMapping
	@Operation(summary = "List all products", description = "Returns the latest stock for all products from all vendors")
	public ResponseEntity<List<ProductResponse>> getAllProducts() {
		List<ProductResponse> products = productRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
		return ResponseEntity.ok(products);
	}

	private ProductResponse toResponse(Product p) {
		return new ProductResponse(
				p.getId(),
				p.getSku(),
				p.getName(),
				p.getStockQuantity(),
				p.getVendor()
		);
	}
}
