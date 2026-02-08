package com.example.stocksync.controller;

import com.example.stocksync.domain.Product;
import com.example.stocksync.dto.ProductResponse;
import com.example.stocksync.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductControllerIntegrationTest {

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ProductRepository productRepository;

	@Test
	void getProducts_returnsOkAndList() {
		String url = "http://localhost:" + port + "/products";
		ResponseEntity<List<ProductResponse>> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<>() {}
		);

		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void getProducts_returnsProductsWhenDataExists() {
		productRepository.save(new Product("TEST-SKU", "Test Product", 42, "VendorA"));

		String url = "http://localhost:" + port + "/products";
		ResponseEntity<List<ProductResponse>> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<>() {}
		);

		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		List<ProductResponse> body = response.getBody();
		assertThat(body).isNotNull().isNotEmpty();
		assertThat(body).anyMatch(p ->
				p.sku().equals("TEST-SKU") && p.stockQuantity() == 42 && p.vendor().equals("VendorA")
		);
	}
}
