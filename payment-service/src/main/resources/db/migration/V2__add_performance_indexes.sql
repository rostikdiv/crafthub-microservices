-- Speed up transaction lookup by order ID (frequently queried by order status workflows)
CREATE INDEX IF NOT EXISTS idx_transactions_order_id ON transactions(order_id);

-- Optimize transaction queries by user ID and status
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
