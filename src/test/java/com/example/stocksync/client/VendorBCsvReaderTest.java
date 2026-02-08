package com.example.stocksync.client;

import com.example.stocksync.dto.VendorProductRow;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VendorBCsvReaderTest {

	@Test
	void read_parsesCsvWithHeader() throws IOException {
		Path tempFile = Files.createTempFile("vendor-b", ".csv");
		Files.writeString(tempFile, """
				sku,name,stockQuantity
				ABC123,Product A,10
				XYZ456,Product B,0
				LMN789,Product C,5
				""");
		VendorBCsvReader reader = new VendorBCsvReader(tempFile.toString());

		List<VendorProductRow> rows = reader.read();

		assertThat(rows).hasSize(3);
		assertThat(rows.get(0)).isEqualTo(new VendorProductRow("ABC123", "Product A", 10));
		assertThat(rows.get(1)).isEqualTo(new VendorProductRow("XYZ456", "Product B", 0));
		assertThat(rows.get(2)).isEqualTo(new VendorProductRow("LMN789", "Product C", 5));
		Files.deleteIfExists(tempFile);
	}

	@Test
	void read_parsesAnyNumberOfRows() throws IOException {
		int rowCount = 25;
		StringBuilder csv = new StringBuilder("sku,name,stockQuantity\n");
		for (int i = 0; i < rowCount; i++) {
			csv.append("SKU").append(i).append(",Product ").append(i).append(",").append(i * 2).append("\n");
		}
		Path tempFile = Files.createTempFile("vendor-b", ".csv");
		Files.writeString(tempFile, csv.toString());
		VendorBCsvReader reader = new VendorBCsvReader(tempFile.toString());

		List<VendorProductRow> rows = reader.read();

		assertThat(rows).hasSize(rowCount);
		assertThat(rows.get(0)).isEqualTo(new VendorProductRow("SKU0", "Product 0", 0));
		assertThat(rows.get(24)).isEqualTo(new VendorProductRow("SKU24", "Product 24", 48));
		Files.deleteIfExists(tempFile);
	}

	@Test
	void read_usesClasspathFallbackWhenFileNotFound() throws IOException {
		VendorBCsvReader reader = new VendorBCsvReader("/nonexistent/vendor-b/stock.csv");

		List<VendorProductRow> rows = reader.read();

		// Fallback to classpath vendor-b/stock.csv
		assertThat(rows).isNotEmpty();
		assertThat(rows.get(0).sku()).isNotBlank();
		assertThat(rows.get(0).stockQuantity()).isNotNull();
	}

	@Test
	void read_returnsEmptyWhenFileEmpty() throws IOException {
		Path tempFile = Files.createTempFile("vendor-b", ".csv");
		VendorBCsvReader reader = new VendorBCsvReader(tempFile.toString());

		List<VendorProductRow> rows = reader.read();

		assertThat(rows).isEmpty();
		Files.deleteIfExists(tempFile);
	}

	@Test
	void read_skipsInvalidRows() throws IOException {
		Path tempFile = Files.createTempFile("vendor-b", ".csv");
		Files.writeString(tempFile, """
				sku,name,stockQuantity
				OK1,Valid,1
				bad
				OK2,Also Valid,2
				""");
		VendorBCsvReader reader = new VendorBCsvReader(tempFile.toString());

		List<VendorProductRow> rows = reader.read();

		assertThat(rows).hasSize(2);
		assertThat(rows.get(0)).isEqualTo(new VendorProductRow("OK1", "Valid", 1));
		assertThat(rows.get(1)).isEqualTo(new VendorProductRow("OK2", "Also Valid", 2));
		Files.deleteIfExists(tempFile);
	}
}
