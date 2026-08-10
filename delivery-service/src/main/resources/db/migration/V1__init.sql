CREATE TABLE locations (
    id UUID PRIMARY KEY,
    provider VARCHAR(255) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    name_ukr VARCHAR(255) NOT NULL,
    region VARCHAR(255) NOT NULL
);

CREATE TABLE branches (
    id UUID PRIMARY KEY,
    location_id UUID NOT NULL REFERENCES locations(id),
    external_id VARCHAR(255) NOT NULL,
    branch_number VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE shipments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    status VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    tracking_number VARCHAR(255) NOT NULL,
    delivery_details JSONB,
    created_at TIMESTAMP,
    shipped_at TIMESTAMP
);
