package com.example.stocksync.service;

import com.example.stocksync.client.VendorARestClient;
import com.example.stocksync.client.VendorBCsvReader;
import com.example.stocksync.domain.Product;
import com.example.stocksync.dto.VendorProductRow;
import com.example.stocksync.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Synchronizes product stock from Vendor A (REST) and Vendor B (CSV).
 * Upserts by (sku, vendor) and logs when a product transitions to out-of-stock.
 */
@Service
public class StockSyncService {

	private static final Logger log = LoggerFactory.getLogger(StockSyncService.class);
	private static final String VENDOR_A = "VendorA";
	private static final String VENDOR_B = "VendorB";

	private final VendorARestClient vendorARestClient;
	private final VendorBCsvReader vendorBCsvReader;
	private final ProductRepository productRepository;

	public StockSyncService(VendorARestClient vendorARestClient,
							VendorBCsvReader vendorBCsvReader,
							ProductRepository productRepository) {
		this.vendorARestClient = vendorARestClient;
		this.vendorBCsvReader = vendorBCsvReader;
		this.productRepository = productRepository;
	}

	@Transactional
	public void sync() {
		syncVendor(VENDOR_A, fetchVendorA());
		syncVendor(VENDOR_B, fetchVendorB());
	}

	private List<VendorProductRow> fetchVendorA() {
		try {
			return vendorARestClient.fetchProducts();
		} catch (Exception e) {
			log.warn("Vendor A fetch failed, skipping: {}", e.getMessage());
			return List.of();
		}
	}

	private List<VendorProductRow> fetchVendorB() {
		try {
			return vendorBCsvReader.read();
		} catch (IOException e) {
			log.warn("Vendor B read failed, skipping: {}", e.getMessage());
			return List.of();
		}
	}

	private void syncVendor(String vendor, List<VendorProductRow> rows) {
		for (VendorProductRow row : rows) {
			upsert(vendor, row);
		}
	}

	private void upsert(String vendor, VendorProductRow row) {
		Optional<Product> existing = productRepository.findBySkuAndVendor(row.sku(), vendor);

		if (existing.isPresent()) {
			Product product = existing.get();
			int previousQty = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
			int newQty = row.stockQuantity() != null ? row.stockQuantity() : 0;

			if (previousQty > 0 && newQty == 0) {
				log.info("Product out of stock: sku={}, vendor={}, name={}", product.getSku(), vendor, product.getName());
			}

			product.setName(row.name());
			product.setStockQuantity(newQty);
			productRepository.save(product);
		} else {
			Product product = new Product(
					row.sku(),
					row.name(),
					row.stockQuantity() != null ? row.stockQuantity() : 0,
					vendor
			);
			productRepository.save(product);
		}
	}
}
