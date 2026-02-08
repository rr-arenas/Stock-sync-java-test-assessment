package com.example.stocksync.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.stocksync.client.VendorARestClient;
import com.example.stocksync.client.VendorBCsvReader;
import com.example.stocksync.domain.Product;
import com.example.stocksync.dto.VendorProductRow;
import com.example.stocksync.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockSyncServiceTest {

	@Mock
	private VendorARestClient vendorARestClient;

	@Mock
	private VendorBCsvReader vendorBCsvReader;

	@Mock
	private ProductRepository productRepository;

	private StockSyncService stockSyncService;

	private ListAppender<ILoggingEvent> logAppender;

	@BeforeEach
	void setUp() {
		stockSyncService = new StockSyncService(vendorARestClient, vendorBCsvReader, productRepository);
		attachLogCapture();
	}

	private void attachLogCapture() {
		Logger logger = (Logger) LoggerFactory.getLogger(StockSyncService.class);
		logAppender = new ListAppender<>();
		logAppender.start();
		logger.addAppender(logAppender);
		logger.setLevel(Level.INFO);
	}

	@Test
	void sync_createsNewProductsWhenNoneExist() throws Exception {
		when(vendorARestClient.fetchProducts()).thenReturn(List.of(
				new VendorProductRow("SKU1", "Product 1", 10)
		));
		when(vendorBCsvReader.read()).thenReturn(List.of(
				new VendorProductRow("SKU2", "Product 2", 5)
		));
		when(productRepository.findBySkuAndVendor("SKU1", "VendorA")).thenReturn(Optional.empty());
		when(productRepository.findBySkuAndVendor("SKU2", "VendorB")).thenReturn(Optional.empty());

		stockSyncService.sync();

		ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
		verify(productRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());
		List<Product> saved = captor.getAllValues();
		assertThat(saved).anyMatch(p -> "SKU1".equals(p.getSku()) && "VendorA".equals(p.getVendor()) && p.getStockQuantity() == 10);
		assertThat(saved).anyMatch(p -> "SKU2".equals(p.getSku()) && "VendorB".equals(p.getVendor()) && p.getStockQuantity() == 5);
	}

	@Test
	void sync_logsWhenProductGoesFromStockToOutOfStock() throws Exception {
		Product existing = new Product("SKU1", "Product 1", 5, "VendorA");
		existing.setId(UUID.randomUUID());
		when(vendorARestClient.fetchProducts()).thenReturn(List.of(
				new VendorProductRow("SKU1", "Product 1", 0)
		));
		when(vendorBCsvReader.read()).thenReturn(List.of());
		when(productRepository.findBySkuAndVendor("SKU1", "VendorA")).thenReturn(Optional.of(existing));

		stockSyncService.sync();

		assertThat(logAppender.list)
				.anyMatch(e -> e.getFormattedMessage().contains("out of stock") && e.getFormattedMessage().contains("SKU1"));
		verify(productRepository).save(existing);
		assertThat(existing.getStockQuantity()).isEqualTo(0);
	}

	@Test
	void sync_doesNotLogWhenProductWasAlreadyOutOfStock() throws Exception {
		Product existing = new Product("SKU1", "Product 1", 0, "VendorA");
		existing.setId(UUID.randomUUID());
		when(vendorARestClient.fetchProducts()).thenReturn(List.of(
				new VendorProductRow("SKU1", "Product 1", 0)
		));
		when(vendorBCsvReader.read()).thenReturn(List.of());
		when(productRepository.findBySkuAndVendor("SKU1", "VendorA")).thenReturn(Optional.of(existing));

		stockSyncService.sync();

		assertThat(logAppender.list).noneMatch(e -> e.getFormattedMessage().contains("out of stock"));
		verify(productRepository).save(existing);
	}

	@Test
	void sync_doesNotLogWhenProductGetsStock() throws Exception {
		Product existing = new Product("SKU1", "Product 1", 0, "VendorA");
		existing.setId(UUID.randomUUID());
		when(vendorARestClient.fetchProducts()).thenReturn(List.of(
				new VendorProductRow("SKU1", "Product 1", 5)
		));
		when(vendorBCsvReader.read()).thenReturn(List.of());
		when(productRepository.findBySkuAndVendor("SKU1", "VendorA")).thenReturn(Optional.of(existing));

		stockSyncService.sync();

		assertThat(logAppender.list).noneMatch(e -> e.getFormattedMessage().contains("out of stock"));
		verify(productRepository).save(existing);
		assertThat(existing.getStockQuantity()).isEqualTo(5);
	}

	@Test
	void sync_continuesWithVendorBWhenVendorAFails() throws Exception {
		when(vendorARestClient.fetchProducts()).thenThrow(new RuntimeException("Network error"));
		when(vendorBCsvReader.read()).thenReturn(List.of(new VendorProductRow("SKU2", "Product 2", 3)));
		when(productRepository.findBySkuAndVendor("SKU2", "VendorB")).thenReturn(Optional.empty());

		stockSyncService.sync();

		ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
		verify(productRepository).save(captor.capture());
		assertThat(captor.getValue().getSku()).isEqualTo("SKU2");
		assertThat(captor.getValue().getVendor()).isEqualTo("VendorB");
	}
}
