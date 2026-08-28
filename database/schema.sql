-- ============================================================================
-- database/schema.sql
-- JVM Query Statistics Lab — table definition
--
-- Purpose:
--   Define the `orders` table. This file is mounted into PostgreSQL's
--   docker-entrypoint-initdb.d/ directory and runs automatically on
--   first container start (before data.sql is loaded manually).
--
-- Key design decisions:
--   * NO indexes are created here. Indexes are added deliberately
--     later in the experiment so we can demonstrate the difference
--     between full-scan (no index) and index-scan (with index).
--   * id is BIGSERIAL so PostgreSQL manages the sequence; the seed
--     script (data.sql) inserts without specifying id and lets the
--     sequence assign it.
--   * amount is NUMERIC(12,2) — exact decimal arithmetic, suitable
--     for monetary values.
--   * created_at defaults to NOW() but data.sql provides explicit
--     timestamps spread over a 2-year window.
-- ============================================================================

CREATE TABLE IF NOT EXISTS orders (
    id               BIGSERIAL       PRIMARY KEY,
    customer_id      BIGINT          NOT NULL,
    country          VARCHAR(100)    NOT NULL,
    city             VARCHAR(100)    NOT NULL,
    product_category VARCHAR(100)    NOT NULL,
    status           VARCHAR(20)     NOT NULL,
    amount           NUMERIC(12, 2)  NOT NULL,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- Statistics metadata table
-- Used by the Spring Boot app to store computed statistics so the
-- frontend can display them without re-running expensive aggregations
-- on every request.
-- ============================================================================

CREATE TABLE IF NOT EXISTS column_statistics (
    id              SERIAL          PRIMARY KEY,
    column_name     VARCHAR(100)    NOT NULL UNIQUE,
    row_count       BIGINT,
    distinct_count  BIGINT,
    null_count      BIGINT,
    min_value       TEXT,
    max_value       TEXT,
    avg_value       NUMERIC(20, 4),
    computed_at     TIMESTAMP       DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS histogram_buckets (
    id              SERIAL          PRIMARY KEY,
    column_name     VARCHAR(100)    NOT NULL,
    bucket_index    INT             NOT NULL,
    bucket_lo       NUMERIC(20, 4)  NOT NULL,
    bucket_hi       NUMERIC(20, 4)  NOT NULL,
    row_count       BIGINT          NOT NULL,
    computed_at     TIMESTAMP       DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS most_common_values (
    id              SERIAL          PRIMARY KEY,
    column_name     VARCHAR(100)    NOT NULL,
    value           TEXT            NOT NULL,
    frequency       BIGINT          NOT NULL,
    pct             NUMERIC(6, 4)   NOT NULL,
    computed_at     TIMESTAMP       DEFAULT NOW()
);
