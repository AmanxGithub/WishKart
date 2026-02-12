-- WishKart Database Initialization Script
-- This script runs when PostgreSQL container is first created

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Grant privileges to wishkart user
GRANT ALL PRIVILEGES ON DATABASE wishkart TO wishkart;

-- Create indexes for better search performance (if tables exist)
-- These will be created by Hibernate on first run, but we add them here for clarity

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'WishKart database initialized successfully!';
END $$;
