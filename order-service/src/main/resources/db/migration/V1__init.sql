CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    total_price NUMERIC NOT NULL,
    status VARCHAR(255) NOT NULL,
    payment_method VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    delivery_info JSONB,
    return_reason VARCHAR(1000)
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    product_id UUID NOT NULL,
    name VARCHAR(255),
    quantity INTEGER NOT NULL,
    price_per_unit NUMERIC NOT NULL,
    order_id UUID REFERENCES orders(id)
);

CREATE TABLE order_returns (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    order_item_id BIGINT NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    item_price_snapshot NUMERIC NOT NULL,
    return_shipping_cost NUMERIC NOT NULL,
    final_refund_amount NUMERIC NOT NULL,
    is_shipping_deducted BOOLEAN NOT NULL DEFAULT FALSE,
    return_tracking_number VARCHAR(255),
    return_shipment_id UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
