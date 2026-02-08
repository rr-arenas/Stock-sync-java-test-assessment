# Stock Sync Service

This is a Spring Boot microservice that synchronizes product stock levels from two vendors into a database, detects when a product goes out of stock, and exposes the current stock via a REST API. It was built as a coding assessment for an e-commerce backend role.

---

## What the service does

- Runs on a schedule and performs a **full sync** from both vendors each time
- Fetches from **Vendor A** (REST API) and **Vendor B** (CSV file), normalizes data, and stores it in an **H2 in-memory database**
- Identifies each product by **sku + vendor** (same SKU can exist for different vendors)
- When stock goes from **greater than zero to zero**, logs an out-of-stock message
- If one vendor fails, the other is still processed
- **GET /products** returns all products in the database with latest stock levels

---

## Prerequisites

- **Java 17+** for local runs (Maven wrapper `mvnw` is included, so Maven install is optional)
- **Docker** to build and run the container

---

## Setup

Clone or unpack the project and open a terminal in the **project root** (folder containing `pom.xml` and `Dockerfile`). No extra setup is required; the app uses an in-memory H2 database and bundled data for both vendors.

---

## How to run with Maven

From the project root:

```bash
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd`. The app starts on **port 8080**. Wait a few seconds for the initial sync, then open:

- **Products:** http://localhost:8080/products
- **Swagger UI:** http://localhost:8080/swagger-ui.html

The scheduler runs a sync every 10 seconds by default (see Configuration).

---

## How to run with Docker

From the project root, build the image:

```bash
docker build -t stock-sync-service .
```

Run the container:

```bash
docker run -p 8080:8080 stock-sync-service
```

Then open http://localhost:8080/products (wait a few seconds for the initial sync). To stop: `Ctrl+C` or `docker stop <container-id>`.

---

## How Vendor A is simulated

- A **mock REST controller** in the same app: **GET /api/vendor-a/products**
- It reads from a JSON file: `src/main/resources/vendor-a/products.json`
- Format: array of objects with `sku`, `name`, `stockQuantity`
- The sync job calls `http://localhost:8080/api/vendor-a/products` and gets this data
- Edit `vendor-a/products.json` to change products or quantities; the next sync will use it

---

## How Vendor B is simulated

- The sync reads a **CSV file** from the path in `vendor.b.csv-path` (default: `/tmp/vendor-b/stock.csv`)
- **Format:** UTF-8, comma-delimited, header: `sku,name,stockQuantity`
- If that file **does not exist**, the app uses a bundled CSV: `src/main/resources/vendor-b/stock.csv`
- To use your own file, create e.g. `/tmp/vendor-b/stock.csv` with the same header and row format

---

## Main endpoints

| Endpoint | Description |
|----------|-------------|
| **GET /products** | All products in the DB. JSON array: `id`, `sku`, `name`, `stockQuantity`, `vendor` |
| **GET /api/vendor-a/products** | Mock Vendor A API (used by sync; callable for testing) |
| **GET /swagger-ui.html** | Swagger UI. OpenAPI JSON: `/v3/api-docs` |
| **GET /h2-console** | H2 console. URL: `jdbc:h2:mem:stocksync`, user: `sa`, password: empty |

---

## Configuration

Key properties in `src/main/resources/application.properties`:

| Property | Description |
|----------|-------------|
| `vendor.b.csv-path` | Vendor B CSV path. Default: `/tmp/vendor-b/stock.csv`. Missing file triggers classpath fallback |
| `vendor.a.products-url` | Vendor A API URL. Default: `http://localhost:8080/api/vendor-a/products` |
| `stock.sync.cron` | Sync schedule. Default: every 10 seconds (`*/10 * * * * ?`). Example for every 5 min: `0 */5 * * * ?` |
| `spring.datasource.url` | H2 JDBC URL. Default: in-memory `stocksync` |

---

## Assumptions and trade-offs

- **Product identity:** Unique key is `(sku, vendor)`. Same SKU for different vendors = separate products.
- **Full sync:** Each run fetches the full list from both vendors and upserts. Products not in the feed are not deleted.
- **Out-of-stock:** Logged only (no separate event table). Can be extended to persist later.
- **Vendor A:** Called over HTTP (in-app mock). Retry: 3 attempts, 1 second delay. In production the URL would point to an external service.
- **Resilience:** If Vendor A fails, sync continues with Vendor B (and vice versa).
- **Database:** H2 in-memory; data is lost on restart. For production, switch to a persistent DB (e.g. PostgreSQL).

---

## Testing

This section describes how to test the application in two ways: automated tests (unit and integration) and manual testing of the running application.

---

### Running automated tests

All tests run with Maven from the project root. No external services or files are required; test data is under `src/test/resources`.

**Run the full test suite:**

```bash
./mvnw test
```

Expect all tests to pass. The suite includes unit tests and integration tests that start the application with an in-memory database.

**Run tests by area (optional):**

To run only the tests for a specific part of the app, use `-Dtest` with the test class name:

- **Sync and out-of-stock logic:**  
  `./mvnw test -Dtest=StockSyncServiceTest`

- **Vendor B CSV reader (parsing, fallback, invalid rows):**  
  `./mvnw test -Dtest=VendorBCsvReaderTest`

- **Vendor A REST client (fetch, retry, empty response):**  
  `./mvnw test -Dtest=VendorARestClientTest`

- **Scheduler (invokes sync, handles failure):**  
  `./mvnw test -Dtest=StockSyncSchedulerTest`

- **GET /products and product API:**  
  `./mvnw test -Dtest=ProductControllerIntegrationTest`

- **Vendor A mock endpoint:**  
  `./mvnw test -Dtest=VendorAMockControllerTest`

**What the tests cover:**

- **StockSyncServiceTest:** Creates products from both vendors; updates existing products by sku and vendor; logs when stock goes from greater than zero to zero; does not log when already zero or when stock increases; continues with Vendor B when Vendor A fails.
- **VendorBCsvReaderTest:** Parses CSV with header; handles any number of rows; uses classpath fallback when the file path does not exist; skips invalid rows; returns empty list for an empty file.
- **VendorARestClientTest:** Fetches and maps JSON to products; retries on failure and throws after max retries; returns empty list for an empty array response.
- **ProductControllerIntegrationTest:** GET /products returns 200 and a list; when products exist in the database, they appear in the response.
- **VendorAMockControllerTest:** GET /api/vendor-a/products returns 200 and the product list from the bundled JSON.

---

### Manual testing (running application)

Manual testing checks that the application works end-to-end when started with Maven or Docker.

**Step 1: Start the application**

From the project root run:

```bash
./mvnw spring-boot:run
```

Wait until the log shows that the application has started (e.g. "Started StockSyncServiceApplication") and that the initial sync has run (e.g. "Initial stock sync completed"). This usually takes a few seconds.

**Step 2: Test GET /products**

Open in a browser or call with curl:

```bash
curl -s http://localhost:8080/products
```

You should get a JSON array of products (for example 24 products: 12 from Vendor A and 12 from Vendor B). Each object has `id`, `sku`, `name`, `stockQuantity`, and `vendor`. If you see an empty array `[]`, wait a few more seconds for the initial sync and try again.

**Step 3: Test Swagger UI**

Open in a browser:

- http://localhost:8080/swagger-ui.html

The Swagger UI page should load. You can expand the Products API and try GET /products from the browser. The OpenAPI JSON is available at http://localhost:8080/v3/api-docs.

**Step 4: Test Vendor A mock endpoint**

To confirm the Vendor A mock returns data:

```bash
curl -s http://localhost:8080/api/vendor-a/products
```

You should see a JSON array of products (the same data as in `src/main/resources/vendor-a/products.json`).

**Step 5: Test H2 console (optional)**

Open http://localhost:8080/h2-console in a browser. Use:

- **JDBC URL:** `jdbc:h2:mem:stocksync`
- **User name:** `sa`
- **Password:** leave empty

Click Connect. Run the SQL query:

```sql
SELECT * FROM product;
```

You should see the same products as returned by GET /products. This confirms that the sync has written data to the database.

**Step 6: Test out-of-stock logging (optional)**

To verify that the service logs when a product goes from in-stock to out-of-stock:

1. Stop the application (Ctrl+C).
2. Edit `src/main/resources/vendor-a/products.json` and set one product’s `stockQuantity` to `0` (for example the first product).
3. Start the application again with `./mvnw spring-boot:run`.
4. Watch the console log. After the initial sync runs, you should see a line similar to:  
   `Product out of stock: sku=..., vendor=VendorA, name=...`
5. Call GET /products again and confirm that the product you changed now has `stockQuantity` 0.

---

### Testing with Docker

To test the application as a Docker container:

1. Build the image from the project root:

```bash
docker build -t stock-sync-service .
```

(Use `sudo docker build` if your user is not in the docker group.)

2. Run the container:

```bash
docker run -p 8080:8080 stock-sync-service
```

3. Wait a few seconds for the application to start and the initial sync to complete.

4. From the host machine, run the same checks as in manual testing: GET http://localhost:8080/products, Swagger UI at http://localhost:8080/swagger-ui.html, and optionally the Vendor A mock and H2 console. Results should match the Maven run.

5. Stop the container with Ctrl+C or `docker stop <container-id>`.

---

## Project structure

- **src/main/java** – Controllers, services, clients (Vendor A REST, Vendor B CSV), repository, domain, DTOs, scheduler, config
- **src/main/resources** – `application.properties`, `vendor-a/products.json`, `vendor-b/stock.csv`
- **src/test/java** – Unit and integration tests
- **src/test/resources** – Test data (vendor-a, vendor-b)
- **Dockerfile** – Multi-stage: Maven build then JRE runtime
