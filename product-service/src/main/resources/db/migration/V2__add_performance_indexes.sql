-- Speed up product images collection loading
CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images(product_id);

-- Optimize product catalog filtering and search by category and seller
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_seller_id ON products(seller_id);

-- Optimize review lookups by product, hierarchical parent, and author
CREATE INDEX IF NOT EXISTS idx_product_reviews_product_id ON product_reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_product_reviews_parent_id ON product_reviews(parent_id);
CREATE INDEX IF NOT EXISTS idx_product_reviews_user_id ON product_reviews(user_id);
