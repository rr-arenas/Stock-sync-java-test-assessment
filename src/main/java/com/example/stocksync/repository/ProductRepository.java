package com.example.stocksync.repository;

import com.example.stocksync.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	Optional<Product> findBySkuAndVendor(String sku, String vendor);
}
