ALTER TABLE products ADD CONSTRAINT chk_products_stock_nonnegative CHECK (stock_quantity >= 0);
ALTER TABLE order_items ADD CONSTRAINT uq_order_item_product UNIQUE (order_id, product_id);
CREATE INDEX idx_orders_customer_created ON orders(customer_id, created_at DESC, id DESC);
