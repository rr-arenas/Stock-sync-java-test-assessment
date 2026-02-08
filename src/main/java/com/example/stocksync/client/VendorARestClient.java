package com.example.stocksync.client;

import com.example.stocksync.dto.VendorProductRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Fetches product stock from Vendor A's REST API.
 * Retries on failure (transient errors) with backoff.
 */
@Component
public class VendorARestClient {

	private static final Logger log = LoggerFactory.getLogger(VendorARestClient.class);
	private static final int MAX_ATTEMPTS = 3;
	private static final long BACKOFF_MS = 1000;

	private final RestTemplate restTemplate;
	private final String productsUrl;

	public VendorARestClient(
			RestTemplate restTemplate,
			@Value("${vendor.a.products-url:http://localhost:8080/api/vendor-a/products}") String productsUrl) {
		this.restTemplate = restTemplate;
		this.productsUrl = productsUrl;
	}

	/**
	 * Fetches all products from Vendor A. Retries up to {@value #MAX_ATTEMPTS} times on failure.
	 */
	public List<VendorProductRow> fetchProducts() {
		Exception lastException = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				List<VendorProductRow> result = restTemplate.exchange(
						productsUrl,
						HttpMethod.GET,
						null,
						new ParameterizedTypeReference<List<VendorProductRow>>() {}
				).getBody();
				return result != null ? result : Collections.emptyList();
			} catch (Exception e) {
				lastException = e;
				log.warn("Vendor A fetch attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
				if (attempt < MAX_ATTEMPTS) {
					try {
						Thread.sleep(BACKOFF_MS);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("Vendor A fetch interrupted", ie);
					}
				}
			}
		}
		throw new RuntimeException("Vendor A fetch failed after " + MAX_ATTEMPTS + " attempts", lastException);
	}
}
