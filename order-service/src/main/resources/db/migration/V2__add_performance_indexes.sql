-- Speed up child items lookup for batch fetch (WHERE order_id IN (...))
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);

-- Optimize order filtering by customer, seller, and status
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_seller_id ON orders(seller_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);

-- Optimize return requests lookup by order
CREATE INDEX IF NOT EXISTS idx_order_returns_order_id ON order_returns(order_id);

-- Prevent full table scan on scheduled polling every 5 seconds
CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox_events(status, created_at);
