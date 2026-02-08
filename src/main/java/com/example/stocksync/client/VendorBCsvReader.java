package com.example.stocksync.client;

import com.example.stocksync.dto.VendorProductRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class VendorBCsvReader {

	private static final Logger log = LoggerFactory.getLogger(VendorBCsvReader.class);
	private static final String HEADER = "sku,name,stockQuantity";
	private static final String FALLBACK_RESOURCE = "vendor-b/stock.csv";

	private final String csvPath;

	public VendorBCsvReader(@Value("${vendor.b.csv-path:/tmp/vendor-b/stock.csv}") String csvPath) {
		this.csvPath = csvPath;
	}

	public List<VendorProductRow> read() throws IOException {
		List<String> lines;
		Path path = Path.of(csvPath);
		if (Files.exists(path)) {
			lines = Files.readAllLines(path);
		} else {
			log.info("Vendor B CSV not found at {}, using classpath fallback {}", csvPath, FALLBACK_RESOURCE);
			lines = readFromClasspath();
		}

		List<VendorProductRow> rows = new ArrayList<>();

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

	private List<String> readFromClasspath() throws IOException {
		ClassPathResource resource = new ClassPathResource(FALLBACK_RESOURCE);
		try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			return reader.lines().toList();
		}
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
