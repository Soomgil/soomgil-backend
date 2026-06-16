CREATE SCHEMA IF NOT EXISTS geo;

CREATE TABLE geo.legal_regions (
  code varchar(10) PRIMARY KEY,
  name varchar(100) NOT NULL,
  full_name varchar(200) NOT NULL,
  level varchar(20) NOT NULL,
  parent_code varchar(10),
  sido_code varchar(2) NOT NULL,
  sigungu_code varchar(5),
  eupmyeondong_code varchar(8),
  raw_status varchar(10) NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  synced_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT legal_regions_code_length_check CHECK (char_length(code) = 10),
  CONSTRAINT legal_regions_level_check CHECK (level IN ('SIDO', 'SIGUNGU', 'EUPMYEONDONG')),
  CONSTRAINT legal_regions_parent_code_fk FOREIGN KEY (parent_code) REFERENCES geo.legal_regions (code)
);

CREATE INDEX legal_regions_level_is_active_idx ON geo.legal_regions (level, is_active);
CREATE INDEX legal_regions_parent_code_idx ON geo.legal_regions (parent_code);
CREATE INDEX legal_regions_sido_code_idx ON geo.legal_regions (sido_code);
CREATE INDEX legal_regions_sigungu_code_idx ON geo.legal_regions (sigungu_code);
CREATE INDEX legal_regions_is_active_idx ON geo.legal_regions (is_active);

CREATE TABLE geo.legal_region_sync_logs (
  id bigserial PRIMARY KEY,
  source varchar(50) NOT NULL,
  source_file_name varchar(255),
  total_count integer NOT NULL DEFAULT 0,
  inserted_count integer NOT NULL DEFAULT 0,
  updated_count integer NOT NULL DEFAULT 0,
  deactivated_count integer NOT NULL DEFAULT 0,
  started_at timestamptz NOT NULL,
  finished_at timestamptz,
  status varchar(20) NOT NULL,
  error_message text,
  CONSTRAINT legal_region_sync_logs_status_check CHECK (status IN ('SUCCESS', 'FAILED')),
  CONSTRAINT legal_region_sync_logs_total_count_check CHECK (total_count >= 0),
  CONSTRAINT legal_region_sync_logs_inserted_count_check CHECK (inserted_count >= 0),
  CONSTRAINT legal_region_sync_logs_updated_count_check CHECK (updated_count >= 0),
  CONSTRAINT legal_region_sync_logs_deactivated_count_check CHECK (deactivated_count >= 0)
);

CREATE INDEX legal_region_sync_logs_source_idx ON geo.legal_region_sync_logs (source);
CREATE INDEX legal_region_sync_logs_status_idx ON geo.legal_region_sync_logs (status);
CREATE INDEX legal_region_sync_logs_started_at_idx ON geo.legal_region_sync_logs (started_at);
