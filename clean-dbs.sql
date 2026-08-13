-- =============================================================
-- CraftHub / MilHub Complete Database Reset Script
-- Run this in each database (user_service_db, product_service_db, etc.)
-- in Cloud SQL Studio to completely wipe and trigger full Flyway re-creation!
-- =============================================================

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
