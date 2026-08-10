CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE military_profiles (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    unit_number VARCHAR(255) NOT NULL,
    edrpou VARCHAR(255) NOT NULL,
    commander_name VARCHAR(255) NOT NULL,
    official_address VARCHAR(255)
);

CREATE TABLE seller_profiles (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    company_name VARCHAR(255) NOT NULL,
    description TEXT,
    logo_url TEXT,
    tax_id VARCHAR(255) NOT NULL UNIQUE,
    rating DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    review_count INTEGER NOT NULL DEFAULT 0,
    total_sales INTEGER NOT NULL DEFAULT 0,
    auto_confirm_orders BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE seller_points (
    id UUID PRIMARY KEY,
    seller_profile_id UUID NOT NULL REFERENCES seller_profiles(id),
    name VARCHAR(255) NOT NULL,
    city_ref VARCHAR(255) NOT NULL,
    city_name VARCHAR(255),
    region VARCHAR(255),
    street_name VARCHAR(255),
    building VARCHAR(255),
    apartment VARCHAR(255),
    zip_code VARCHAR(255),
    phone VARCHAR(255),
    instructions TEXT
);

CREATE TABLE saved_addresses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255),
    provider VARCHAR(255) NOT NULL,
    delivery_type VARCHAR(255) NOT NULL,
    city_ref VARCHAR(255) NOT NULL,
    city_name VARCHAR(255),
    region VARCHAR(255),
    branch_ref VARCHAR(255),
    branch_name VARCHAR(255),
    street_name VARCHAR(255),
    building VARCHAR(255),
    apartment VARCHAR(255),
    zip_code VARCHAR(255)
);

CREATE TABLE seller_reviews (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    user_id UUID NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    rating INTEGER,
    comment TEXT,
    seller_reply TEXT,
    created_at TIMESTAMP
);

CREATE TABLE verification_docs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    document_type VARCHAR(255),
    doc_url VARCHAR(255),
    status VARCHAR(255),
    created_at TIMESTAMP
);
