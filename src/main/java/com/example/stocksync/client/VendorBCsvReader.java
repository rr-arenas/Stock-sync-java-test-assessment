package com.example.stocksync.client;

import com.example.stocksync.dto.VendorProductRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


@Component
public class VendorBCsvReader {

	private static final Logger log = LoggerFactory.getLogger(VendorBCsvReader.class);
	private static final String HEADER = "sku,name,stockQuantity";

	private final String csvPath;

	public VendorBCsvReader(@Value("${vendor.b.csv-path:/tmp/vendor-b/stock.csv}") String csvPath) {
		this.csvPath = csvPath;
	}
	public List<VendorProductRow> read() throws IOException {
		Path path = Path.of(csvPath);
		if (!Files.exists(path)) {
			throw new IOException("Vendor B CSV file not found: " + csvPath);
		}

		List<VendorProductRow> rows = new ArrayList<>();
		List<String> lines = Files.readAllLines(path);

		if (lines.isEmpty()) {
			return rows;
		}

		int start = 0;
		if (lines.get(0).strip().equalsIgnoreCase(HEADER)) {
			start = 1;
		}

		for (int i = start; i < lines.size(); i++) {
			String line = lines.get(i).strip();
			if (line.isEmpty()) {
				continue;
			}
			VendorProductRow row = parseLine(line, i + 1);
			if (row != null) {
				rows.add(row);
			}
		}

		return rows;
	}

	private VendorProductRow parseLine(String line, int lineNumber) {
		String[] parts = line.split(",", -1);
		if (parts.length < 3) {
			log.warn("Vendor B CSV line {}: expected 3 columns, got {} - skipping: {}", lineNumber, parts.length, line);
			return null;
		}
		String sku = parts[0].strip();
		String name = parts[1].strip();
		String qtyStr = parts[2].strip();
		if (sku.isEmpty()) {
			log.warn("Vendor B CSV line {}: empty sku - skipping", lineNumber);
			return null;
		}
		int stockQuantity;
		try {
			stockQuantity = Integer.parseInt(qtyStr);
		} catch (NumberFormatException e) {
			log.warn("Vendor B CSV line {}: invalid stockQuantity '{}' - skipping", lineNumber, qtyStr);
			return null;
		}
		return new VendorProductRow(sku, name, stockQuantity);
	}
}
