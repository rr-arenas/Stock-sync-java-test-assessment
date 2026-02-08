package com.example.stocksync.controller;

import com.example.stocksync.dto.VendorProductRow;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VendorAMockControllerTest {

	private static final String VENDOR_A_JSON = "/vendor-a/products.json";
	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private RestTemplate restTemplate;

	/** Expected data from JSON file – count and items are dynamic. */
	private List<VendorProductRow> loadExpectedProducts() throws IOException {
		try (InputStream in = getClass().getResourceAsStream(VENDOR_A_JSON)) {
			if (in == null) {
				throw new IllegalStateException("Test resource not found: " + VENDOR_A_JSON);
			}
			return objectMapper.readValue(in, new TypeReference<>() {});
		}
	}

	@Test
	void getProducts_returnsOkAndProductArray() throws IOException {
		List<VendorProductRow> expected = loadExpectedProducts();
		String url = "http://localhost:" + port + "/api/vendor-a/products";
		ResponseEntity<List<VendorProductRow>> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<>() {}
		);

		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		List<VendorProductRow> body = response.getBody();
		assertThat(body).isNotNull().hasSize(expected.size());
		if (!expected.isEmpty()) {
			assertThat(body.get(0)).isEqualTo(expected.get(0));
			assertThat(body.get(expected.size() - 1)).isEqualTo(expected.get(expected.size() - 1));
		}
	}
}
