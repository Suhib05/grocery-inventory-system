# Architecture notes

## Database

```mermaid
erDiagram
    CATEGORIES ||--o{ PRODUCTS : contains
    PRODUCTS ||--o{ SALES : "sold in"
    USERS ||--o{ SALES : records

    USERS {
        int id PK
        varchar username UK
        varchar password_hash
        varchar full_name
        enum role "ADMIN | STAFF"
        boolean active
    }
    CATEGORIES {
        int id PK
        varchar name UK
        varchar description
    }
    PRODUCTS {
        int id PK
        varchar name
        int category_id FK
        int quantity "CHECK >= 0"
        decimal price "CHECK >= 0"
        int min_stock
        date expiry_date "NULL = non-perishable"
        timestamp archived_at "soft delete"
    }
    SALES {
        int id PK
        int product_id FK
        int quantity_sold "CHECK > 0"
        decimal unit_price "price at time of sale"
        decimal total_amount "GENERATED"
        date sale_date
        int recorded_by FK
        timestamp voided_at
        int voided_by FK
    }
```

## Recording a sale

```mermaid
sequenceDiagram
    actor Cashier
    participant UI as sales.js
    participant F as AuthFilter
    participant S as SaleServlet
    participant SS as SaleService
    participant D as JdbcSaleDao
    participant DB as MySQL

    Cashier->>UI: choose product + quantity
    UI->>F: POST /api/sales (X-CSRF-Token)
    F->>F: session valid? CSRF token matches?
    F->>S: forward
    S->>SS: record(input, user)
    SS->>SS: validate, product exists, not expired, enough stock
    SS->>D: recordSale(productId, qty, shelfPrice, date, userId)
    D->>DB: BEGIN
    D->>DB: UPDATE products SET quantity = quantity - ? WHERE id = ? AND quantity >= ?
    alt 0 rows updated (stock ran out)
        D->>DB: ROLLBACK
        D-->>SS: InsufficientStockException
        SS-->>S: ConflictException
        S-->>UI: 409 {"error": "..."}
    else stock lowered
        D->>DB: INSERT INTO sales ...
        D->>DB: COMMIT
        D-->>SS: sale id
        SS-->>S: Sale
        S-->>UI: 201 Sale JSON
    end
```

The service checks stock first so it can return a friendly message. The conditional `UPDATE` is what makes the
operation safe: if another cashier sells the last units between the check and the update, no rows are updated
and the whole transaction is rolled back.

## Product status rules

Status is worked out every time a product is read, in this order (the first rule that matches wins):

| # | Rule | Status |
|---|---|---|
| 1 | quantity is 0 | Out of Stock |
| 2 | expiry date is before today | Expired |
| 3 | expiry date is today to 7 days from now | Expiring Soon |
| 4 | quantity ≤ minimum stock | Low Stock |
| 5 | otherwise | In Stock |

Out of Stock comes first because when there's nothing on the shelf, the expiry date no longer matters.

## Error responses

Every API error has the same shape, so the frontend handles them all the same way:

```json
{ "error": "Please fix the highlighted fields.",
  "fieldErrors": { "price": "Price can have at most 2 decimal places." } }
```

| Status | When |
|---|---|
| 400 | Invalid input (`ValidationException`) or malformed JSON |
| 401 | Not logged in / session expired |
| 403 | Missing CSRF token, or a STAFF user trying an ADMIN action |
| 404 | Unknown product or sale |
| 409 | Conflicts with current data (not enough stock, expired product, sale already voided) |
| 429 | Too many failed logins |
| 500 | Unexpected error (logged on the server; details are not sent to the browser) |
