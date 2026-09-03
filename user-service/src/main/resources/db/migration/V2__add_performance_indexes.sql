-- Enforce 1:1 integrity and fast profile lookups
CREATE UNIQUE INDEX IF NOT EXISTS idx_seller_profiles_user_id ON seller_profiles(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_military_profiles_user_id ON military_profiles(user_id);

-- Optimize verification review queries and status filtering
CREATE INDEX IF NOT EXISTS idx_verification_docs_user_id ON verification_docs(user_id);
CREATE INDEX IF NOT EXISTS idx_verification_docs_status ON verification_docs(status);

-- Optimize saved delivery addresses and seller pickup points lookups
CREATE INDEX IF NOT EXISTS idx_saved_addresses_user_id ON saved_addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_seller_points_seller_profile_id ON seller_points(seller_profile_id);

-- Optimize seller review aggregations (rating, review count)
CREATE INDEX IF NOT EXISTS idx_seller_reviews_seller_id ON seller_reviews(seller_id);
