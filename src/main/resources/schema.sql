-- 1. Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
                                          log_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                          user_id INT NOT NULL,
                                          action VARCHAR(255) NOT NULL,
    table_affected VARCHAR(100) NOT NULL,
    record_id INT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT log_id_unique UNIQUE (log_id)
    );

-- 2. Items
CREATE TABLE IF NOT EXISTS items (
                                     item_id INT AUTO_INCREMENT PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255) NULL,
    brand_name VARCHAR(255) NULL,
    barcode VARCHAR(100) NULL,
    category ENUM ('antibiotics', 'painkiller', 'vaccine', 'medical supply', 'non medical supply', 'equipment') NULL,
    dosage_form VARCHAR(255) NOT NULL,
    strength VARCHAR(50) NULL,
    retail_price DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    reorder_level INT DEFAULT 10 NOT NULL,
    prescription_required TINYINT(1) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT barcode_unique UNIQUE (barcode)
    );
CREATE INDEX idx_items_category_type ON items (category);

-- 3. Batches
CREATE TABLE IF NOT EXISTS batches (
                                       batch_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                       batch_number VARCHAR(100) NOT NULL,
    item_id INT NOT NULL,
    quantity_received INT NOT NULL,
    quantity_remaining INT NOT NULL,
    manufactured_date DATE NULL,
    expiration_date DATE NULL,
    purchase_price DECIMAL(12, 2) NOT NULL,
    selling_price DECIMAL(12, 2) NOT NULL,
    storage_location VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL,
    received_date DATE NOT NULL,
    CONSTRAINT batch_id_unique UNIQUE (batch_id),
    CONSTRAINT item_batch_unique UNIQUE (item_id, batch_number),
    CHECK (`quantity_received` >= 0),
    CHECK (`quantity_remaining` >= 0),
    CHECK (`purchase_price` >= 0),
    CHECK (`selling_price` >= 0),
    CHECK (`status` IN ('active', 'expired', 'damaged'))
    );
CREATE INDEX idx_batches_item_expiration ON batches (item_id, expiration_date);
CREATE INDEX idx_batches_status ON batches (status);

-- 4. Customers
CREATE TABLE IF NOT EXISTS customers (
                                         customer_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                         first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(50) NULL,
    email VARCHAR(50) NULL,
    address TEXT NULL,
    date_of_birth DATE NULL,
    gender VARCHAR(20) NULL,
    medical_record_number VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT cust_id_unique UNIQUE (customer_id),
    CONSTRAINT mrn_unique UNIQUE (medical_record_number)
    );

-- 5. Suppliers
CREATE TABLE IF NOT EXISTS suppliers (
                                         supplier_id INT AUTO_INCREMENT PRIMARY KEY,
                                         name VARCHAR(255) NOT NULL,
    contact VARCHAR(255) NOT NULL,
    phone_number VARCHAR(13) NULL,
    email VARCHAR(255) NULL,
    address VARCHAR(255) NULL,
    license_number VARCHAR(100) NULL,
    payment_terms VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL
    );
CREATE INDEX index_supplier_name ON suppliers (name);

-- 6. Purchases
CREATE TABLE IF NOT EXISTS purchases (
                                         purchase_id BIGINT NOT NULL PRIMARY KEY,
                                         order_date DATE NOT NULL,
                                         expected_delivery_date DATE NULL,
                                         status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(14, 2) NULL,
    supplier_id INT NOT NULL,
    is_approved TINYINT(1) DEFAULT 0 NULL,
    user_id INT NULL,
    actual_amount DECIMAL(15, 2) DEFAULT 0.00 NULL,
    received_at TIMESTAMP NULL,
    CHECK (`status` IN ('pending', 'delivered', 'cancelled')),
    CHECK (`total_amount` >= 0)
    );
CREATE INDEX idx_purchase_orders_supplier ON purchases (supplier_id);

-- 7. Purchase Items (UPDATED WITH IS_RECEIVED)
CREATE TABLE IF NOT EXISTS purchase_items (
                                              purchase_item_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                              purchase_id BIGINT NOT NULL,
                                              item_id INT NOT NULL,
                                              batch_number VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    expiry_date DATE NULL,
    is_received TINYINT(1) DEFAULT 0, -- Track if item is received in StockEntry
    CONSTRAINT po_item_unique UNIQUE (purchase_id, item_id, batch_number),
    CHECK (`quantity` >= 0),
    CHECK (`unit_price` >= 0)
    );

-- 8. Sales
CREATE TABLE IF NOT EXISTS sales (
                                     sale_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                     customer_id INT NULL,
                                     total_amount DECIMAL(14, 2) DEFAULT 0.00 NULL,
    discount DECIMAL(14, 2) DEFAULT 0.00 NULL,
    payment_method VARCHAR(20) NOT NULL,
    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT sale_id_unique UNIQUE (sale_id),
    CHECK (`total_amount` >= 0),
    CHECK (`discount` >= 0),
    CHECK (`payment_method` IN ('cash', 'card', 'insurance'))
    );
CREATE INDEX idx_sales_customer ON sales (customer_id);

-- 9. Sale Items
CREATE TABLE IF NOT EXISTS sale_items (
                                          sale_item_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                          sale_id BIGINT UNSIGNED NOT NULL,
                                          batch_id INT NOT NULL,
                                          item_id INT NOT NULL,
                                          quantity INT NOT NULL,
                                          unit_price DECIMAL(12, 2) NOT NULL,
    subtotal DECIMAL(14, 2) DEFAULT 0.00 NULL,
    CHECK (`quantity` >= 0),
    CHECK (`unit_price` > 0),
    CHECK (`subtotal` >= 0)
    );
CREATE INDEX idx_sale_items_batch ON sale_items (batch_id);
CREATE INDEX idx_sale_items_item ON sale_items (item_id);
CREATE INDEX idx_sale_items_sale ON sale_items (sale_id);

-- 10. Stock Adjustments
CREATE TABLE IF NOT EXISTS stock_adjustments (
                                                 adjustment_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                                 batch_id INT NOT NULL,
                                                 item_id INT NOT NULL,
                                                 old_quantity INT NOT NULL,
                                                 new_quantity INT NOT NULL,
                                                 difference INT NOT NULL,
                                                 reason TEXT NOT NULL,
                                                 adjusted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                 adjusted_by INT NOT NULL,
                                                 CHECK (`old_quantity` >= 0),
    CHECK (`new_quantity` >= 0)
    );
CREATE INDEX idx_stock_adj_item_batch ON stock_adjustments (item_id, batch_id);

-- 11. Item Suppliers
CREATE TABLE IF NOT EXISTS item_suppliers (
                                              item_supplier_id INT AUTO_INCREMENT PRIMARY KEY,
                                              item_id INT NOT NULL,
                                              supplied_id INT NOT NULL,
                                              FOREIGN KEY (supplied_id) REFERENCES suppliers (supplier_id),
    FOREIGN KEY (item_id) REFERENCES items (item_id)
    );

-- 12. Users
CREATE TABLE IF NOT EXISTS users (
                                     user_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    rfid_tag VARCHAR(100) NULL,
    in_warehouse TINYINT(1) DEFAULT 0 NULL,
    CONSTRAINT email_unique UNIQUE (email),
    CONSTRAINT rfid_tag_unique UNIQUE (rfid_tag),
    CHECK (`role` IN ('admin', 'pharmacist', 'cashier', 'manager'))
    );

-- 13. Waste
CREATE TABLE IF NOT EXISTS waste (
                                     waste_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                     batch_id INT NOT NULL,
                                     item_id INT NOT NULL,
                                     quantity_removed INT NOT NULL,
                                     reason VARCHAR(20) NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    processed_by INT NOT NULL,
    CHECK (`quantity_removed` >= 0),
    CHECK (`reason` IN ('expired', 'damaged', 'recalled'))
    );
CREATE INDEX idx_waste_batch ON waste (batch_id);