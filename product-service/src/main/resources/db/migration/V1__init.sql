CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    parent_id BIGINT REFERENCES categories(id)
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    price NUMERIC NOT NULL,
    old_price NUMERIC,
    quantity INTEGER NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    length DOUBLE PRECISION NOT NULL,
    width DOUBLE PRECISION NOT NULL,
    height DOUBLE PRECISION NOT NULL,
    preview_image_url VARCHAR(255) NOT NULL,
    access_level VARCHAR(255) NOT NULL,
    seller_id UUID NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    seller_name VARCHAR(255),
    seller_logo_url VARCHAR(255),
    average_rating DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    review_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE product_images (
    product_id UUID NOT NULL REFERENCES products(id),
    image_url VARCHAR(255)
);

CREATE TABLE product_reviews (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    user_id UUID NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    user_avatar_url VARCHAR(255),
    rating INTEGER,
    comment TEXT,
    is_verified_purchase BOOLEAN NOT NULL DEFAULT FALSE,
    parent_id UUID REFERENCES product_reviews(id),
    created_at TIMESTAMP
);
