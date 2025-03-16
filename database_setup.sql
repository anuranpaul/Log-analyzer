-- Create the database if it doesn't exist
CREATE DATABASE loganalyzer;

-- Connect to the database
\c loganalyzer;

-- Create the log severity enum type
CREATE TYPE severity_enum AS ENUM ('INFO', 'DEBUG', 'WARNING', 'ERROR', 'CRITICAL');

-- Create the logs table (optional, as Hibernate will create it)
CREATE TABLE IF NOT EXISTS logs (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    application VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    severity severity_enum NOT NULL,
    source VARCHAR(255),
    host VARCHAR(255)
);

-- Create the metadata fields table (optional, as Hibernate will create it)
CREATE TABLE IF NOT EXISTS metadata_fields (
    id SERIAL PRIMARY KEY,
    log_id BIGINT REFERENCES logs(id) ON DELETE CASCADE,
    key VARCHAR(255) NOT NULL,
    value VARCHAR(255) NOT NULL
);

-- Index for faster querying by timestamp
CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON logs(timestamp);

-- Index for faster querying by application
CREATE INDEX IF NOT EXISTS idx_logs_application ON logs(application);

-- Index for faster querying by severity
CREATE INDEX IF NOT EXISTS idx_logs_severity ON logs(severity);

-- Grant permissions to postgres user
GRANT ALL PRIVILEGES ON DATABASE loganalyzer TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;

-- Note: You can run this script with: 
-- psql -U postgres -f database_setup.sql 