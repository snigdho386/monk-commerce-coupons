# Coupons Management API
A RESTful service built with **Spring Boot** for managing and applying dynamic discount coupons in an e-commerce system. The project follows a clean layered structure and uses the **Strategy Pattern** to make coupon logic easy to extend.

---

## Tech Stack
- **Language:** Java 17+
- **Framework:** Spring Boot 3.x
- **Database:** H2 In-Memory Database
- **Architecture:** Controller → Service → Repository + Strategy Pattern

---

## Getting Started
### 1. Clone the Repository
```bash
git clone <your-repo-url>
cd <folder-name>
```

### 2. Build and Run
```bash
./mvnw spring-boot:run
```

### 3. Access the Application
- **API Base URL:** `http://localhost:8080`
- **H2 Console:** `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:monkdb`

---

## Use Cases & Design Considerations
The following scenarios were accounted for during the design phase to meet the problem statement thoroughly.

### Implemented
#### **1. Cart-Wise Coupons**
Discount is applied when total cart value exceeds a specified threshold.

#### **2. Product-Wise Coupons**
Discount applies only to specific product IDs present in the cart.

#### **3. BxGy (Buy X Get Y) Coupons**
- Supports multiple products in both the **buy** and **get** lists.
- Repetition allowed (e.g., Buy 2 Get 1 could apply multiple times).
- A repetition limit may be configured.

### **4. Coupon Expiration & Activation**
- Each `Coupon` now supports an optional `expirationDate` (`LocalDateTime`) and an `isActive` flag.
- Expired coupons (where `expirationDate` is before current time) and inactive coupons are ignored by the system:
  - `POST /applicable-coupons` will skip expired/inactive coupons when listing applicable coupons.
  - `POST /apply-coupon/{id}` will refuse to apply an expired or inactive coupon and return an error.

Example coupon with expiration:
```json
{
  "type": "cart-wise",
  "details": { "min_cart_value": 100.0, "discount_percent": 10.0 },
  "expirationDate": "2025-12-31T23:59:59",
  "isActive": true
}
```

Notes:
- `expirationDate` should be in ISO-8601 format (e.g. `yyyy-MM-dd'T'HH:mm:ss`).
- The service enforces these checks in the `CouponService` layer; controllers delegate to the service.
- Unit tests were added to verify that expired coupons are not applied.

- Usage-based expiry: By default coupons are NOT consumed on use and there is no
  usage-based expiry implemented in this service. Coupons are evaluated by
  `expirationDate` and `isActive` only. If you require per-use limits or
  single-use coupons, implement a usage counter (e.g. `usesRemaining`) or a
  `singleUse` flag and decrement/persist it when the coupon is successfully
  applied. See "Future Enhancements" for usage-limit ideas.

---

### Future Enhancements (Not Implemented Yet)
- Coupon stacking (applying multiple discounts in one order)
- Tiered cart discounts
- Maximum discount caps
- Exclusion rules for clearance items
- User-targeted coupons
- Global and per-user usage limits
- Optimized BxGy logic for multi-product cases

---

## Assumptions
- All prices are in a single currency.
- Product IDs are assumed valid (no catalog lookup).
- BxGy counts totals across all eligible products.
- Quantities are assumed to be integers.

---

## Limitations
- H2 database is in-memory, so data resets on restart.
- Validation is minimal to keep the MVP simple.
- Coupon applicability checks iterate the full list of coupons.

---

## API Reference
### **1. Create a Coupon**
`POST /coupons`
```json
{
  "type": "bxgy",
  "details": {
    "buy_products": [ { "product_id": 1, "quantity": 2 } ],
    "get_products": [ { "product_id": 2, "quantity": 1 } ],
    "repetition_limit": 3
  },
  "expirationDate": "2025-12-31T23:59:59",
  "isActive": true
}
```

Request body fields
- `type` (string): Coupon type. Supported values: `cart-wise`, `product-wise`, `bxgy`.
- `details` (object): Type-specific configuration. See sections below for `cart-wise`, `product-wise`, and `bxgy` detail shapes.
- `expirationDate` (ISO-8601 datetime, optional): If present and in the past, the server will reject the request. If present and in the future the coupon will become invalid after that moment.
- `isActive` (boolean, optional): Whether the coupon is active. Defaults to `true`.

Server-side validation
- The API validates incoming payloads and will return `400 Bad Request` when required fields are missing or malformed (for example, `details` missing required keys for the chosen `type`, or `expirationDate` is in the past).
- When applying a coupon via `POST /apply-coupon/{id}` the service will return:
  - `200 OK` with updated cart on success.
  - `404 Not Found` when the coupon id does not exist.
  - `400 Bad Request` when coupon criteria are not met (e.g., minimum cart value not reached).
  - `410 Gone` (or `400` depending on your preference) when attempting to apply an expired or inactive coupon. Currently the service returns a runtime error mapped to an HTTP error via the global exception handler.

Example error response (expired coupon)
```json
{
  "error": "Coupon expired or inactive",
  "couponId": 123
}
```

### **2. Check Applicable Coupons**
`POST /applicable-coupons`
- Input: Cart object
- Output: List of matching coupons + calculated discount

Behavior notes:
- Expired or inactive coupons are skipped when building the `applicable_coupons` list. The service performs a dry-run (cloning the cart) before returning the discount amount so the input cart is not mutated.

### **3. Apply Coupon**
`POST /apply-coupon/{id}`
- Input: Cart object
- Output: Updated cart with `total_discount` and `final_price`

Behavior notes:
- The endpoint performs the following checks in the service layer before applying:
  1. Coupon exists (`404` if not found).
  2. Coupon is active and not expired (`410` / error if expired/inactive).
  3. Coupon criteria are met (`400` if not applicable).

Implementation details
- The service enforces coupon lifecycle checks (expiration + `isActive`). Strategies implement the domain logic for discounts and are registered at service construction time.

Developer note:
- Strategy registration now uses an explicit type API: each `CouponStrategy` exposes `getType()` (e.g. `"bxgy"`). The `CouponService` builds a registry from `strategy.getType()` so adding a new strategy requires only creating a new strategy class that returns the appropriate type — no changes to the service are necessary.

---

## Running Tests
Unit tests are included and can be run with:
```bash
./mvnw test
```

---

## Notes
This README summarizes project behavior, constraints, and scope while remaining beginner-friendly and practical. Adjust descriptions as the project grows.

---

## Developer Narrative — assumptions, approach, design decisions

This section exists to explain the reasoning and trade-offs I (the developer) made while building this coupon microservice. The goal is to make it easy for another developer to pick up the project, understand why things are the way they are, and where to make safe changes.

- **High-level goal.** Build a small, maintainable service that can store coupons and calculate/apply discounts using a small set of strategy implementations. The design favors clarity and extensibility over cleverness.

- **Key assumptions**
  - Prices and discounts are expressed in a single currency for the entire system — no currency conversion layer is provided. This keeps the model and arithmetic simple for the MVP. If the product expands internationally, a currency-aware money type should replace raw doubles.
  - Product IDs are opaque long identifiers; the service does not call a product catalog to validate them. Strategies operate purely on IDs present in the `Cart` object.
  - `Coupon.details` is stored as a flexible JSON-backed `Map<String,Object>` to allow rapid iteration on coupon shapes. This trades type-safety for speed-of-development; when coupon shapes stabilize, migrating to typed DTOs is highly recommended.
  - The system treats expiry and active state as authoritative at apply-time (lazy evaluation). Coupons are not automatically deactivated by a background job; instead the service checks `expirationDate` and `isActive` at runtime.

- **Approach & architecture decisions**
  - Use the Strategy pattern for coupon rules so each coupon type encapsulates its own logic. This isolates domain logic and makes adding a new coupon type (e.g. `tiered`, `free-shipping`) a single-class change.
  - Keep a thin `CouponService` that coordinates persistence, lifecycle checks (existence, active, expiration), and strategy dispatch. The service does not implement discount logic — strategies do.
  - Persist `Coupon.details` using a `JsonConverter` so the DB stores the raw rules. Strategies are responsible for parsing and validating the contents of `details`.
  - Favor defensive programming in strategies: validate presence/types of expected fields and return the input cart unchanged when configuration is invalid. This reduces runtime exceptions caused by malformed data and keeps the API resilient.

- **Design principles applied**
  - **Separation of concerns (SRP):** Strategies only implement the coupon logic. `CouponService` handles persistence and lifecycle checks. Models are plain data holders. Helper behavior (e.g. cloning a cart for dry-runs) should be moved into the `Cart` model if you want to further tighten SRP.
  - **Open/Closed (OCP):** Strategies declare their canonical type via `getType()` and the service builds a registry at startup. Adding a new strategy does not require modifying `CouponService`.
  - **Dependency inversion:** `CouponService` depends on the `CouponStrategy` interface; Spring injects all implementations so the service works against abstractions.
  - **Defensive design:** Strategies log and return without mutating the cart when they encounter invalid configuration. This keeps the system robust against bad data.

- **Detailed decisions & trade-offs**
  - `Map<String,Object>` in `Coupon.details` vs typed DTOs:
    - Chosen: `Map` for speed of iteration and reduced upfront modeling work.
    - Trade-off: less compile-time safety and more runtime checks. Plan a migration to DTOs when shapes stabilize.
  - Expiration handling: lazy check on apply/list vs scheduled deactivation:
    - Chosen: lazy check. Simpler, fewer background components, immediate correctness on access.
    - Trade-off: the DB will still contain expired coupons; if you want to purge/deactivate them you can add a scheduler later.
  - Strategy registration: explicit `getType()` on the strategy vs probing or bean-name mapping:
    - Chosen: explicit `getType()` for clarity and to avoid coupling to bean naming conventions. It makes automatic registration straightforward and keeps the contract explicit.

- **Implemented strategies (how they work)**
  - `CartWiseStrategy` (`type="cart-wise")`: Applies a percent discount when `cart.totalPrice` exceeds a threshold. It expects `details` keys `threshold` and `discount` (percent). The strategy validates types and logs decisions.
  - `ProductWiseStrategy` (`type="product-wise")`: Applies a percent discount to items matching a `product_id` in the `details`. It expects `product_id` and `discount` (percent) in `details`. It loops cart items and adjusts `totalDiscount` accordingly.
  - `BxGyStrategy` (`type="bxgy")`: Implements buy-X-get-Y logic with support for multiple buy/get product entries and a `repetition_limit`. It validates numeric types and quantities, calculates repeat sets, and applies free-item discounts in FCFS order.

- **Validation & error handling**
  - The service throws domain exceptions for notable conditions and the `GlobalExceptionHandler` maps these to sensible HTTP codes and payloads (e.g. `CouponExpiredException` → 410 Gone / structured error body). Use the existing exceptions when adding behavior that should be communicated to API clients.
  - Strategies avoid throwing on configuration problems; they log the issue and return the input cart unchanged. This ensures API endpoints remain resilient.

- **Testing & observability**
  - Unit tests cover service behavior (happy path, invalid type, criteria not met, expired coupon). Add strategy-focused unit tests (parsing edge-cases, malformed `details`) whenever you change parsing logic.
  - Strategies log detailed debug-level messages on decision points. When debugging production-like issues set the strategy package to debug to see how rules were evaluated.

- **Extending the system**
  - Add a new strategy class that implements `CouponStrategy` and returns a unique `getType()` value. Implement `isApplicable` and `applyDiscount`. Add unit tests for the strategy. No changes are required in `CouponService`.
  - If you plan to migrate `Coupon.details` to typed DTOs, update the persistence converter and then change each strategy to accept the typed DTO (or create a parser helper).

