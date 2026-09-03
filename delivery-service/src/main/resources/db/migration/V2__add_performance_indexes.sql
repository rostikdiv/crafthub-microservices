-- Speed up branch retrieval by city/location
CREATE INDEX IF NOT EXISTS idx_branches_location_id ON branches(location_id);

-- Speed up shipment tracking by order ID and tracking number
CREATE INDEX IF NOT EXISTS idx_shipments_order_id ON shipments(order_id);
CREATE INDEX IF NOT EXISTS idx_shipments_tracking_number ON shipments(tracking_number);
