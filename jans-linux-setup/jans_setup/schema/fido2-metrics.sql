-- FIDO2 Metrics SQL Schema
-- This schema defines the SQL tables for FIDO2 metrics storage

-- Table for FIDO2 Metrics Entry
CREATE TABLE IF NOT EXISTS jans_fido2_metrics_entry (
    id VARCHAR(36) PRIMARY KEY,
    metric_type VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    user_id VARCHAR(255),
    username VARCHAR(255),
    operation_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    duration_ms BIGINT,
    authenticator_type VARCHAR(50),
    device_info JSON,
    error_reason TEXT,
    error_category VARCHAR(50),
    fallback_method VARCHAR(50),
    fallback_reason TEXT,
    user_agent TEXT,
    ip_address VARCHAR(45),
    session_id VARCHAR(255),
    additional_data JSON,
    node_id VARCHAR(255),
    application_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_metric_type (metric_type),
    INDEX idx_timestamp (timestamp),
    INDEX idx_user_id (user_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_status (status),
    INDEX idx_authenticator_type (authenticator_type),
    INDEX idx_node_id (node_id),
    INDEX idx_created_at (created_at)
);

-- Table for FIDO2 Metrics Aggregation
CREATE TABLE IF NOT EXISTS jans_fido2_metrics_aggregation (
    id VARCHAR(36) PRIMARY KEY,
    aggregation_type VARCHAR(20) NOT NULL,
    time_period VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    node_id VARCHAR(255),
    application_type VARCHAR(50),
    
    -- Registration Metrics
    registration_attempts BIGINT DEFAULT 0,
    registration_successes BIGINT DEFAULT 0,
    registration_failures BIGINT DEFAULT 0,
    registration_success_rate DECIMAL(5,4),
    registration_avg_duration DECIMAL(10,2),
    registration_min_duration BIGINT,
    registration_max_duration BIGINT,
    
    -- Authentication Metrics
    authentication_attempts BIGINT DEFAULT 0,
    authentication_successes BIGINT DEFAULT 0,
    authentication_failures BIGINT DEFAULT 0,
    authentication_success_rate DECIMAL(5,4),
    authentication_avg_duration DECIMAL(10,2),
    authentication_min_duration BIGINT,
    authentication_max_duration BIGINT,
    
    -- Fallback Metrics
    fallback_events BIGINT DEFAULT 0,
    fallback_rate DECIMAL(5,4),
    fallback_methods JSON,
    
    -- Device/Platform Metrics
    device_types JSON,
    authenticator_types JSON,
    browsers JSON,
    operating_systems JSON,
    
    -- Error Analysis
    error_categories JSON,
    top_errors JSON,
    
    -- User Metrics
    unique_users BIGINT DEFAULT 0,
    new_users BIGINT DEFAULT 0,
    returning_users BIGINT DEFAULT 0,
    user_adoption_rate DECIMAL(5,4),
    
    -- Performance Metrics
    peak_concurrent_operations INT,
    avg_concurrent_operations DECIMAL(10,2),
    peak_memory_usage BIGINT,
    avg_memory_usage DECIMAL(10,2),
    peak_cpu_usage DECIMAL(5,2),
    avg_cpu_usage DECIMAL(5,2),
    
    -- Geographic/Network Metrics
    top_ip_addresses JSON,
    geographic_distribution JSON,
    
    -- Additional Analytics
    session_metrics JSON,
    custom_metrics JSON,
    
    -- Metadata
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_quality VARCHAR(20),
    completeness DECIMAL(3,2),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_aggregation (aggregation_type, time_period, node_id),
    INDEX idx_aggregation_type (aggregation_type),
    INDEX idx_time_period (time_period),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time),
    INDEX idx_node_id (node_id),
    INDEX idx_created_at (created_at)
);

-- Table for FIDO2 User Metrics (for user-level tracking)
CREATE TABLE IF NOT EXISTS jans_fido2_user_metrics (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    first_registration_date TIMESTAMP,
    last_activity_date TIMESTAMP,
    total_registrations INT DEFAULT 0,
    total_authentications INT DEFAULT 0,
    successful_registrations INT DEFAULT 0,
    successful_authentications INT DEFAULT 0,
    failed_registrations INT DEFAULT 0,
    failed_authentications INT DEFAULT 0,
    fallback_events INT DEFAULT 0,
    preferred_authenticator_type VARCHAR(50),
    preferred_device_type VARCHAR(50),
    preferred_browser VARCHAR(100),
    preferred_os VARCHAR(100),
    avg_registration_duration DECIMAL(10,2),
    avg_authentication_duration DECIMAL(10,2),
    last_ip_address VARCHAR(45),
    last_user_agent TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_user (user_id),
    INDEX idx_username (username),
    INDEX idx_first_registration (first_registration_date),
    INDEX idx_last_activity (last_activity_date),
    INDEX idx_is_active (is_active),
    INDEX idx_created_at (created_at)
);

-- Table for FIDO2 Device Metrics (for device-level tracking)
CREATE TABLE IF NOT EXISTS jans_fido2_device_metrics (
    id VARCHAR(36) PRIMARY KEY,
    device_fingerprint VARCHAR(255) NOT NULL,
    device_type VARCHAR(50),
    browser VARCHAR(100),
    browser_version VARCHAR(50),
    operating_system VARCHAR(100),
    os_version VARCHAR(50),
    platform VARCHAR(50),
    user_agent TEXT,
    total_operations INT DEFAULT 0,
    successful_operations INT DEFAULT 0,
    failed_operations INT DEFAULT 0,
    avg_operation_duration DECIMAL(10,2),
    first_seen TIMESTAMP,
    last_seen TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_device (device_fingerprint),
    INDEX idx_device_type (device_type),
    INDEX idx_browser (browser),
    INDEX idx_os (operating_system),
    INDEX idx_platform (platform),
    INDEX idx_first_seen (first_seen),
    INDEX idx_last_seen (last_seen),
    INDEX idx_is_active (is_active),
    INDEX idx_created_at (created_at)
);

-- Table for FIDO2 Error Metrics (for error tracking and analysis)
CREATE TABLE IF NOT EXISTS jans_fido2_error_metrics (
    id VARCHAR(36) PRIMARY KEY,
    error_code VARCHAR(100),
    error_category VARCHAR(50),
    error_message TEXT,
    operation_type VARCHAR(50),
    user_id VARCHAR(255),
    device_fingerprint VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    stack_trace TEXT,
    context_data JSON,
    occurrence_count INT DEFAULT 1,
    first_occurrence TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_occurrence TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_resolved BOOLEAN DEFAULT FALSE,
    resolution_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_error_code (error_code),
    INDEX idx_error_category (error_category),
    INDEX idx_operation_type (operation_type),
    INDEX idx_user_id (user_id),
    INDEX idx_device_fingerprint (device_fingerprint),
    INDEX idx_occurrence_count (occurrence_count),
    INDEX idx_first_occurrence (first_occurrence),
    INDEX idx_last_occurrence (last_occurrence),
    INDEX idx_is_resolved (is_resolved),
    INDEX idx_created_at (created_at)
);

-- Table for FIDO2 Performance Metrics (for performance monitoring)
CREATE TABLE IF NOT EXISTS jans_fido2_performance_metrics (
    id VARCHAR(36) PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    value DECIMAL(15,4) NOT NULL,
    unit VARCHAR(20),
    node_id VARCHAR(255),
    timestamp TIMESTAMP NOT NULL,
    tags JSON,
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_metric_name (metric_name),
    INDEX idx_metric_type (metric_type),
    INDEX idx_node_id (node_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_created_at (created_at)
);

-- Views for common queries

-- View for daily metrics summary
CREATE OR REPLACE VIEW v_fido2_daily_metrics AS
SELECT 
    DATE(timestamp) as date,
    COUNT(*) as total_operations,
    SUM(CASE WHEN operation_type = 'REGISTRATION' THEN 1 ELSE 0 END) as registrations,
    SUM(CASE WHEN operation_type = 'AUTHENTICATION' THEN 1 ELSE 0 END) as authentications,
    SUM(CASE WHEN operation_type = 'FALLBACK' THEN 1 ELSE 0 END) as fallbacks,
    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as successes,
    SUM(CASE WHEN status = 'FAILURE' THEN 1 ELSE 0 END) as failures,
    AVG(duration_ms) as avg_duration,
    MIN(duration_ms) as min_duration,
    MAX(duration_ms) as max_duration,
    COUNT(DISTINCT user_id) as unique_users,
    COUNT(DISTINCT ip_address) as unique_ips
FROM jans_fido2_metrics_entry
GROUP BY DATE(timestamp)
ORDER BY date DESC;

-- View for user adoption metrics
CREATE OR REPLACE VIEW v_fido2_user_adoption AS
SELECT 
    DATE(first_registration_date) as registration_date,
    COUNT(*) as new_users,
    SUM(total_registrations) as total_registrations,
    SUM(successful_registrations) as successful_registrations,
    AVG(avg_registration_duration) as avg_registration_duration
FROM jans_fido2_user_metrics
WHERE first_registration_date IS NOT NULL
GROUP BY DATE(first_registration_date)
ORDER BY registration_date DESC;

-- View for device analytics
CREATE OR REPLACE VIEW v_fido2_device_analytics AS
SELECT 
    device_type,
    browser,
    operating_system,
    platform,
    COUNT(*) as device_count,
    SUM(total_operations) as total_operations,
    SUM(successful_operations) as successful_operations,
    AVG(avg_operation_duration) as avg_duration,
    COUNT(DISTINCT DATE(first_seen)) as active_days
FROM jans_fido2_device_metrics
WHERE is_active = TRUE
GROUP BY device_type, browser, operating_system, platform
ORDER BY total_operations DESC;

-- View for error analysis
CREATE OR REPLACE VIEW v_fido2_error_analysis AS
SELECT 
    error_category,
    error_code,
    COUNT(*) as error_count,
    SUM(occurrence_count) as total_occurrences,
    COUNT(DISTINCT user_id) as affected_users,
    COUNT(DISTINCT device_fingerprint) as affected_devices,
    MIN(first_occurrence) as first_seen,
    MAX(last_occurrence) as last_seen,
    AVG(occurrence_count) as avg_occurrences_per_error
FROM jans_fido2_error_metrics
WHERE is_resolved = FALSE
GROUP BY error_category, error_code
ORDER BY total_occurrences DESC;

-- Stored procedures for common operations

-- Procedure to clean up old metrics data
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS CleanupOldFido2Metrics(IN retention_days INT)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Delete old metrics entries
    DELETE FROM jans_fido2_metrics_entry 
    WHERE created_at < DATE_SUB(NOW(), INTERVAL retention_days DAY);
    
    -- Delete old aggregations (keep longer for historical analysis)
    DELETE FROM jans_fido2_metrics_aggregation 
    WHERE created_at < DATE_SUB(NOW(), INTERVAL (retention_days * 2) DAY);
    
    -- Delete old performance metrics
    DELETE FROM jans_fido2_performance_metrics 
    WHERE created_at < DATE_SUB(NOW(), INTERVAL retention_days DAY);
    
    COMMIT;
END //
DELIMITER ;

-- Procedure to generate daily aggregations
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS GenerateDailyFido2Aggregation(IN target_date DATE)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    INSERT INTO jans_fido2_metrics_aggregation (
        id, aggregation_type, time_period, start_time, end_time,
        registration_attempts, registration_successes, registration_failures,
        authentication_attempts, authentication_successes, authentication_failures,
        fallback_events, unique_users, last_updated
    )
    SELECT 
        UUID() as id,
        'DAILY' as aggregation_type,
        DATE_FORMAT(target_date, '%Y-%m-%d') as time_period,
        target_date as start_time,
        DATE_ADD(target_date, INTERVAL 1 DAY) as end_time,
        SUM(CASE WHEN operation_type = 'REGISTRATION' THEN 1 ELSE 0 END) as registration_attempts,
        SUM(CASE WHEN operation_type = 'REGISTRATION' AND status = 'SUCCESS' THEN 1 ELSE 0 END) as registration_successes,
        SUM(CASE WHEN operation_type = 'REGISTRATION' AND status = 'FAILURE' THEN 1 ELSE 0 END) as registration_failures,
        SUM(CASE WHEN operation_type = 'AUTHENTICATION' THEN 1 ELSE 0 END) as authentication_attempts,
        SUM(CASE WHEN operation_type = 'AUTHENTICATION' AND status = 'SUCCESS' THEN 1 ELSE 0 END) as authentication_successes,
        SUM(CASE WHEN operation_type = 'AUTHENTICATION' AND status = 'FAILURE' THEN 1 ELSE 0 END) as authentication_failures,
        SUM(CASE WHEN operation_type = 'FALLBACK' THEN 1 ELSE 0 END) as fallback_events,
        COUNT(DISTINCT user_id) as unique_users,
        NOW() as last_updated
    FROM jans_fido2_metrics_entry
    WHERE DATE(timestamp) = target_date
    ON DUPLICATE KEY UPDATE
        registration_attempts = VALUES(registration_attempts),
        registration_successes = VALUES(registration_successes),
        registration_failures = VALUES(registration_failures),
        authentication_attempts = VALUES(authentication_attempts),
        authentication_successes = VALUES(authentication_successes),
        authentication_failures = VALUES(authentication_failures),
        fallback_events = VALUES(fallback_events),
        unique_users = VALUES(unique_users),
        last_updated = VALUES(last_updated);
    
    COMMIT;
END //
DELIMITER ;

