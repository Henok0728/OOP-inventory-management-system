create table if not exists audit_logs
(
    log_id         bigint unsigned auto_increment
    primary key,
    user_id        int                                 not null,
    action         varchar(255)                        not null,
    table_affected varchar(100)                        not null,
    record_id      int                                 null,
    timestamp      timestamp default CURRENT_TIMESTAMP not null,
    constraint log_id
    unique (log_id)
    );

create table if not exists batches
(
    batch_id           bigint unsigned auto_increment
    primary key,
    batch_number       varchar(100)   not null,
    item_id            int            not null,
    quantity_received  int            not null,
    quantity_remaining int            not null,
    manufactured_date  date           null,
    expiration_date    date           null,
    purchase_price     decimal(12, 2) not null,
    selling_price      decimal(12, 2) not null,
    storage_location   varchar(100)   null,
    status             varchar(20)    not null,
    received_date      date           not null,
    constraint batch_id
    unique (batch_id),
    constraint item_id
    unique (item_id, batch_number),
    check (`quantity_received` >= 0),
    check (`quantity_remaining` >= 0),
    check (`purchase_price` >= 0),
    check (`selling_price` >= 0),
    check (`status` in (_utf8mb4\'active\',_utf8mb4\'expired\',_utf8mb4\'damaged\'))
    );

create index idx_batches_item_expiration
    on batches (item_id, expiration_date);

create index idx_batches_status
    on batches (status);

create table if not exists customers
(
    customer_id           bigint unsigned auto_increment
    primary key,
    first_name            varchar(100)                        not null,
    last_name             varchar(100)                        not null,
    phone                 varchar(50)                         null,
    email                 varchar(50)                         null,
    address               text                                null,
    date_of_birth         date                                null,
    gender                varchar(20)                         null,
    medical_record_number varchar(100)                        null,
    created_at            timestamp default CURRENT_TIMESTAMP not null,
    constraint customer_id
    unique (customer_id),
    constraint medical_record_number
    unique (medical_record_number)
    );

create table if not exists items
(
    item_id               int auto_increment
    primary key,
    name                  varchar(255)                                                                                       not null,
    generic_name          varchar(255)                                                                                       null,
    brand_name            varchar(255)                                                                                       null,
    barcode               varchar(100)                                                                                       null,
    category              enum ('antibiotics', 'painkiller', 'vaccine', 'medical supply', 'non medical supply', 'equipment') null,
    dosage_form           varchar(255)                                                                                       not null,
    strength              varchar(50)                                                                                        null,
    retail_price          decimal(12, 2) default 0.00                                                                        not null,
    reorder_level         int            default 10                                                                          not null,
    prescription_required tinyint(1)     default 0                                                                           not null,
    created_at            timestamp      default CURRENT_TIMESTAMP                                                           null,
    updated_at            timestamp      default CURRENT_TIMESTAMP                                                           null on update CURRENT_TIMESTAMP,
    constraint barcode
    unique (barcode)
    );

create index idx_items_category_type
    on items (category);

create table if not exists products
(
    id                  int auto_increment
    primary key,
    name                text          not null,
    category            text          null,
    quantity            int default 0 not null,
    price               double        null,
    low_stock_threshold int           null
);

create table if not exists purchase_items
(
    item_id          int            not null,
    purchase_item_id bigint unsigned auto_increment
    primary key,
    batch_number     varchar(100)   not null,
    quantity         int            not null,
    unit_price       decimal(12, 2) not null,
    expiry_date      date           null,
    purchase_id      int            not null,
    constraint purchase_id
    unique (purchase_id, batch_number),
    constraint purchase_item_id
    unique (purchase_item_id),
    check (`quantity` >= 0),
    check (`unit_price` >= 0)
    );

create table if not exists purchases
(
    purchase_id            decimal        not null
    primary key,
    order_date             date           not null,
    expected_delivery_date date           null,
    status                 varchar(20)    not null,
    total_amount           decimal(14, 2) null,
    supplier_id            int            not null,
    check (`status` in (_utf8mb4\'pending\',_utf8mb4\'delivered\',_utf8mb4\'cancelled\')),
    check (`total_amount` >= 0)
    );

create index idx_purchase_orders_supplier
    on purchases (supplier_id);

create table if not exists sale_items
(
    sale_id      int            not null,
    sale_item_id bigint unsigned auto_increment
    primary key,
    quantity     int            not null,
    unit_price   decimal(12, 2) not null,
    subtotal     decimal(14, 2) not null,
    batch_id     int            not null,
    item_id      int            not null,
    constraint sale_item_id
    unique (sale_item_id),
    check (`quantity` >= 0),
    check (`unit_price` > 0),
    check (`subtotal` >= 0)
    );

create index idx_sale_items_batch
    on sale_items (batch_id);

create index idx_sale_items_item
    on sale_items (item_id);

create index idx_sale_items_sale
    on sale_items (sale_id);

create table if not exists sales
(
    sale_id        bigint unsigned auto_increment
    primary key,
    customer_id    int                                      null,
    total_amount   decimal(14, 2) default 0.00              null,
    discount       decimal(14, 2) default 0.00              null,
    payment_method varchar(20)                              not null,
    sale_date      timestamp      default CURRENT_TIMESTAMP not null,
    constraint sale_id
    unique (sale_id),
    check (`total_amount` >= 0),
    check (`discount` >= 0),
    check (`payment_method` in (_utf8mb4\'cash\',_utf8mb4\'card\',_utf8mb4\'insurance\'))
    );

create index idx_sales_customer
    on sales (customer_id);

create table if not exists stock_adjustments
(
    batch_id      int                                 not null,
    adjustment_id bigint unsigned auto_increment
    primary key,
    item_id       int                                 not null,
    old_quantity  int                                 not null,
    new_quantity  int                                 not null,
    difference    int                                 not null,
    reason        text                                not null,
    adjusted_at   timestamp default CURRENT_TIMESTAMP not null,
    adjusted_by   int                                 not null,
    constraint adjustment_id
    unique (adjustment_id),
    check (`old_quantity` >= 0),
    check (`new_quantity` >= 0)
    );

create index idx_stock_adj_item_batch
    on stock_adjustments (item_id, batch_id);

create table if not exists suppliers
(
    supplier_id    int auto_increment
    primary key,
    name           varchar(255)                        not null,
    contact        varchar(255)                        not null,
    phone_number   varchar(13)                         null,
    email          varchar(255)                        null,
    address        varchar(255)                        null,
    license_number varchar(100)                        null,
    payment_terms  varchar(100)                        null,
    created_at     timestamp default CURRENT_TIMESTAMP null
    );

create table if not exists item_suppliers
(
    item_supplier_id int auto_increment
    primary key,
    item_id          int not null,
    supplied_id      int not null,
    constraint fk_supplier_id
    foreign key (item_supplier_id) references suppliers (supplier_id),
    constraint item_suppliers_ibfk_1
    foreign key (item_id) references items (item_id)
    );

create index item_id
    on item_suppliers (item_id);

create index index_supplier_name
    on suppliers (name);

create table if not exists users
(
    user_id       bigint unsigned auto_increment
    primary key,
    name          varchar(255)                        not null,
    email         varchar(255)                        not null,
    password_hash varchar(255)                        not null,
    role          varchar(50)                         not null,
    created_at    timestamp default CURRENT_TIMESTAMP not null,
    constraint email
    unique (email),
    constraint user_id
    unique (user_id),
    check (`role` in (_utf8mb4\'admin\',_utf8mb4\'pharmacist\',_utf8mb4\'cashier\',_utf8mb4\'manager\'))
    );

create table if not exists waste
(
    waste_id         bigint unsigned auto_increment
    primary key,
    batch_id         int                                 not null,
    item_id          int                                 not null,
    quantity_removed int                                 not null,
    reason           varchar(20)                         not null,
    recorded_at      timestamp default CURRENT_TIMESTAMP not null,
    processed_by     int                                 not null,
    constraint waste_id
    unique (waste_id),
    check (`quantity_removed` >= 0),
    check (`reason` in (_utf8mb4\'expired\',_utf8mb4\'damaged\',_utf8mb4\'recalled\'))
    );

create index idx_waste_batch
    on waste (batch_id);

