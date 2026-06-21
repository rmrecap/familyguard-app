require('dotenv').config();
const { Pool } = require('pg');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false,
});

const migrations = `
-- FamilyGuard Database Schema

-- Devices table
CREATE TABLE IF NOT EXISTS devices (
  device_id VARCHAR(36) PRIMARY KEY,
  device_name VARCHAR(255) NOT NULL,
  device_model VARCHAR(100),
  os_version VARCHAR(50),
  fcm_token TEXT,
  role VARCHAR(20) NOT NULL CHECK (role IN ('PARENT', 'CHILD')),
  group_id VARCHAR(36),
  registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
  last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Family groups table
CREATE TABLE IF NOT EXISTS family_groups (
  group_id VARCHAR(36) PRIMARY KEY,
  invite_code VARCHAR(10) UNIQUE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Add foreign key for group_id in devices
ALTER TABLE devices ADD CONSTRAINT fk_device_group
  FOREIGN KEY (group_id) REFERENCES family_groups(group_id)
  ON DELETE SET NULL;

-- Location updates table
CREATE TABLE IF NOT EXISTS location_updates (
  id SERIAL PRIMARY KEY,
  device_id VARCHAR(36) NOT NULL,
  payload TEXT NOT NULL,
  timestamp BIGINT NOT NULL,
  signature TEXT,
  synced_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_location_device_id ON location_updates(device_id);
CREATE INDEX idx_location_synced_at ON location_updates(synced_at);

-- SOS alerts table
CREATE TABLE IF NOT EXISTS sos_alerts (
  alert_id VARCHAR(36) PRIMARY KEY,
  device_id VARCHAR(36) NOT NULL,
  payload TEXT NOT NULL,
  timestamp BIGINT NOT NULL,
  signature TEXT,
  status VARCHAR(20) NOT NULL CHECK (status IN ('TRIGGERED', 'DELIVERED', 'ACKNOWLEDGED', 'CANCELLED')),
  acknowledged_by VARCHAR(36),
  acknowledged_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_sos_device_id ON sos_alerts(device_id);
CREATE INDEX idx_sos_status ON sos_alerts(status);

-- Geofence events table
CREATE TABLE IF NOT EXISTS geofence_events (
  id SERIAL PRIMARY KEY,
  device_id VARCHAR(36) NOT NULL,
  payload TEXT NOT NULL,
  timestamp BIGINT NOT NULL,
  signature TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_geofence_device_id ON geofence_events(device_id);

-- Safe zones table
CREATE TABLE IF NOT EXISTS safe_zones (
  zone_id VARCHAR(36) PRIMARY KEY,
  group_id VARCHAR(36) NOT NULL,
  name VARCHAR(255) NOT NULL,
  center_latitude DOUBLE PRECISION NOT NULL,
  center_longitude DOUBLE PRECISION NOT NULL,
  radius_meters FLOAT NOT NULL,
  is_active BOOLEAN DEFAULT true,
  notify_on_entry BOOLEAN DEFAULT true,
  notify_on_exit BOOLEAN DEFAULT true,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_safe_zones_group_id ON safe_zones(group_id);

-- Consent records table
CREATE TABLE IF NOT EXISTS consent_records (
  id SERIAL PRIMARY KEY,
  feature_id VARCHAR(50) NOT NULL,
  device_id VARCHAR(36) NOT NULL,
  granted BOOLEAN NOT NULL,
  granted_at TIMESTAMP WITH TIME ZONE NOT NULL,
  revoked_at TIMESTAMP WITH TIME ZONE,
  consent_version VARCHAR(20) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_consent_device_id ON consent_records(device_id);

-- Audit log table
CREATE TABLE IF NOT EXISTS audit_logs (
  id SERIAL PRIMARY KEY,
  device_id VARCHAR(36) NOT NULL,
  action VARCHAR(100) NOT NULL,
  actor VARCHAR(50) NOT NULL,
  details TEXT,
  ip_address VARCHAR(45),
  user_agent TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_audit_device_id ON audit_logs(device_id);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);

-- Sync queue table
CREATE TABLE IF NOT EXISTS sync_queue (
  id SERIAL PRIMARY KEY,
  device_id VARCHAR(36) NOT NULL,
  payload TEXT NOT NULL,
  sync_type VARCHAR(50) NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SYNCED', 'FAILED')),
  retry_count INT DEFAULT 0,
  max_retries INT DEFAULT 3,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  synced_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_sync_queue_status ON sync_queue(status);
CREATE INDEX idx_sync_queue_device_id ON sync_queue(device_id);
`;

async function migrate() {
  try {
    console.log('Starting database migration...');
    await pool.query(migrations);
    console.log('Migration completed successfully!');
    process.exit(0);
  } catch (error) {
    console.error('Migration failed:', error);
    process.exit(1);
  }
}

migrate();
