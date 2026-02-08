package com.example.stocksync.client;

import com.example.stocksync.dto.VendorProductRow;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class VendorARestClientTest {

	private static final String PRODUCTS_URL = "http://localhost/api/vendor-a/products";
	private static final String VENDOR_A_JSON = "/vendor-a/products.json";

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private RestTemplate restTemplate;
	private MockRestServiceServer server;
	private VendorARestClient client;

	@BeforeEach
	void setUp() {
		restTemplate = new RestTemplate();
		server = MockRestServiceServer.createServer(restTemplate);
		client = new VendorARestClient(restTemplate, PRODUCTS_URL);
	}

	private String loadVendorAJson() throws IOException {
		try (InputStream in = getClass().getResourceAsStream(VENDOR_A_JSON)) {
			if (in == null) {
				throw new IllegalStateException("Test resource not found: " + VENDOR_A_JSON);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Loads and parses Vendor A products from the JSON file. Count and items are dynamic from the file.
	 */
	private List<VendorProductRow> loadVendorAProducts() throws IOException {
		String json = loadVendorAJson();
		return objectMapper.readValue(json, new TypeReference<>() {});
	}

	@Test
	void fetchProducts_returnsListOnSuccess() throws IOException {
		List<VendorProductRow> expected = loadVendorAProducts();
		String json = loadVendorAJson();
		server.expect(requestTo(PRODUCTS_URL))
				.andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

		List<VendorProductRow> result = client.fetchProducts();

		assertThat(result).hasSize(expected.size());
		if (!expected.isEmpty()) {
			assertThat(result.get(0)).isEqualTo(expected.get(0));
			assertThat(result.get(expected.size() - 1)).isEqualTo(expected.get(expected.size() - 1));
		}
		server.verify();
	}

	@Test
	void fetchProducts_retriesOnFailureThenSucceeds() throws IOException {
		List<VendorProductRow> expected = loadVendorAProducts();
		String json = loadVendorAJson();
		server.expect(requestTo(PRODUCTS_URL)).andRespond(withServerError());
		server.expect(requestTo(PRODUCTS_URL)).andRespond(withServerError());
		server.expect(requestTo(PRODUCTS_URL))
				.andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

		List<VendorProductRow> result = client.fetchProducts();

		assertThat(result).hasSize(expected.size());
		if (!expected.isEmpty()) {
			assertThat(result.get(0)).isEqualTo(expected.get(0));
		}
		server.verify();
	}

	@Test
	void fetchProducts_throwsAfterMaxRetries() {
		server.expect(requestTo(PRODUCTS_URL)).andRespond(withServerError());
		server.expect(requestTo(PRODUCTS_URL)).andRespond(withServerError());
		server.expect(requestTo(PRODUCTS_URL)).andRespond(withServerError());

		assertThatThrownBy(() -> client.fetchProducts())
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("failed after 3 attempts");
		server.verify();
	}

	@Test
	void fetchProducts_returnsEmptyListWhenResponseIsEmptyArray() {
		server.expect(requestTo(PRODUCTS_URL))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		List<VendorProductRow> result = client.fetchProducts();

		assertThat(result).isEmpty();
		server.verify();
	}
}
