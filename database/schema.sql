-- =====================================================================
--  GroceryIQ - Smart Grocery Inventory System
--  Schema for MySQL 8.0+
--
--  Design notes
--  * Products are soft-deleted (archived_at) so sales history is never lost.
--  * Sales store a snapshot of the unit price at the time of sale, and
--    total_amount is a generated column so it can never disagree with
--    quantity * price.
--  * Sales are voided rather than deleted, which keeps an audit trail and
--    lets the app put the stock back.
--  * CHECK constraints back up the validation done in the service layer.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS grocery_inventory
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE grocery_inventory;

-- ---------------------------------------------------------------------
-- Users (staff accounts)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    role           ENUM('ADMIN','STAFF') NOT NULL DEFAULT 'STAFF',
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  TIMESTAMP    NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Categories
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(60)  NOT NULL UNIQUE,
    description  VARCHAR(255) NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Products
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    category_id  INT           NOT NULL,
    quantity     INT           NOT NULL DEFAULT 0,
    price        DECIMAL(10,2) NOT NULL,
    min_stock    INT           NOT NULL DEFAULT 10,
    expiry_date  DATE          NULL,
    archived_at  TIMESTAMP     NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT chk_products_quantity  CHECK (quantity  >= 0),
    CONSTRAINT chk_products_price     CHECK (price     >= 0),
    CONSTRAINT chk_products_min_stock CHECK (min_stock >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_expiry   ON products(expiry_date);
CREATE INDEX idx_products_name     ON products(name);

-- ---------------------------------------------------------------------
-- Sales
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sales (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    product_id     INT           NOT NULL,
    quantity_sold  INT           NOT NULL,
    unit_price     DECIMAL(10,2) NOT NULL,
    total_amount   DECIMAL(12,2) AS (quantity_sold * unit_price) STORED,
    sale_date      DATE          NOT NULL,
    recorded_by    INT           NULL,
    voided_at      TIMESTAMP     NULL,
    voided_by      INT           NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sales_product     FOREIGN KEY (product_id)  REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sales_recorded_by FOREIGN KEY (recorded_by) REFERENCES users(id)    ON DELETE SET NULL,
    CONSTRAINT fk_sales_voided_by   FOREIGN KEY (voided_by)   REFERENCES users(id)    ON DELETE SET NULL,
    CONSTRAINT chk_sales_quantity   CHECK (quantity_sold > 0),
    CONSTRAINT chk_sales_price      CHECK (unit_price   >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_sales_date    ON sales(sale_date);
CREATE INDEX idx_sales_product ON sales(product_id);
