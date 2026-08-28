-- ============================================================================
-- database/data.sql
-- JVM Query Statistics Lab — synthetic dataset generator
--
-- Purpose:
--   Populate the `orders` table with 1,000,000 rows of DETERMINISTIC,
--   INTENTIONALLY SKEWED data. This dataset is later used to reproduce
--   query-planner mistakes caused by missing/inaccurate/insufficient
--   statistics (bad cardinality estimates, poor plan choices, etc).
--
-- Design notes:
--   * No procedural loops, no 1,000,000 individual INSERT statements.
--     A single set-based INSERT ... SELECT ... FROM generate_series(...)
--     is used.
--   * "Randomness" is NOT produced with random(). Instead every value is
--     derived deterministically from the row number `id` via hashtext().
--     hashtext(text) is a pure function of its input, so the same id
--     always produces the same hash, and therefore the same row, on
--     every execution of this script (unlike random(), whose output
--     depends on the session's random seed / call order).
--   * hashtext() returns a signed int4. We mask off the sign bit with
--     `& 2147483647` (0x7FFFFFFF) and divide by 2147483647 to obtain a
--     deterministic pseudo-uniform value in [0, 1) for each "channel".
--     Each channel uses a distinct salt string (e.g. '|country',
--     '|city', ...) so the channels are decorrelated from one another.
--   * This script assumes the `orders` table already exists (see
--     schema.sql). No indexes are created here — indexes are introduced
--     deliberately later in the experiment.
-- ============================================================================

BEGIN;

WITH

-- ----------------------------------------------------------------------
-- 1. Base row generator + deterministic pseudo-random channels
-- ----------------------------------------------------------------------
raw AS (
    SELECT
        g AS id,
        ((hashtext(g::text || '|country')       & 2147483647)::numeric / 2147483647.0) AS r_country,
        ((hashtext(g::text || '|country_other')  & 2147483647)::numeric / 2147483647.0) AS r_country_other,
        ((hashtext(g::text || '|city')           & 2147483647)::numeric / 2147483647.0) AS r_city,
        ((hashtext(g::text || '|category')       & 2147483647)::numeric / 2147483647.0) AS r_category,
        ((hashtext(g::text || '|status')         & 2147483647)::numeric / 2147483647.0) AS r_status,
        ((hashtext(g::text || '|amount_tier')    & 2147483647)::numeric / 2147483647.0) AS r_amount_tier,
        ((hashtext(g::text || '|amount_val')     & 2147483647)::numeric / 2147483647.0) AS r_amount_val,
        ((hashtext(g::text || '|date_month')     & 2147483647)::numeric / 2147483647.0) AS r_date_month,
        ((hashtext(g::text || '|date_day')       & 2147483647)::numeric / 2147483647.0) AS r_date_day,
        ((hashtext(g::text || '|date_time')      & 2147483647)::numeric / 2147483647.0) AS r_date_time,
        ((hashtext(g::text || '|customer')       & 2147483647)::numeric / 2147483647.0) AS r_customer
    FROM generate_series(1, 1000000) AS g
),

-- ----------------------------------------------------------------------
-- 2. Country distribution
--    India ~70%, USA ~10%, Germany ~8%, Japan ~5%, UK ~4%,
--    Iceland ~0.012% (~120 rows), remainder split across a handful
--    of other countries.
-- ----------------------------------------------------------------------
with_country AS (
    SELECT
        raw.*,
        CASE
            WHEN r_country < 0.70000 THEN 'India'
            WHEN r_country < 0.80000 THEN 'USA'
            WHEN r_country < 0.88000 THEN 'Germany'
            WHEN r_country < 0.93000 THEN 'Japan'
            WHEN r_country < 0.97000 THEN 'UK'
            WHEN r_country < 0.97012 THEN 'Iceland'
            ELSE
                CASE
                    WHEN r_country_other < 0.20 THEN 'France'
                    WHEN r_country_other < 0.40 THEN 'Brazil'
                    WHEN r_country_other < 0.60 THEN 'Canada'
                    WHEN r_country_other < 0.80 THEN 'Australia'
                    ELSE 'South Africa'
                END
        END AS country
    FROM raw
),

-- ----------------------------------------------------------------------
-- 3. City distribution — depends logically on country (no impossible
--    country/city combinations).
-- ----------------------------------------------------------------------
with_city AS (
    SELECT
        with_country.*,
        CASE country
            WHEN 'India' THEN
                CASE
                    WHEN r_city < 0.30 THEN 'Bengaluru'
                    WHEN r_city < 0.55 THEN 'Mumbai'
                    WHEN r_city < 0.75 THEN 'Delhi'
                    WHEN r_city < 0.90 THEN 'Chennai'
                    ELSE 'Hyderabad'
                END
            WHEN 'USA' THEN
                CASE
                    WHEN r_city < 0.25 THEN 'New York'
                    WHEN r_city < 0.50 THEN 'Los Angeles'
                    WHEN r_city < 0.70 THEN 'Chicago'
                    WHEN r_city < 0.87 THEN 'Seattle'
                    ELSE 'Austin'
                END
            WHEN 'Germany' THEN
                CASE
                    WHEN r_city < 0.35 THEN 'Berlin'
                    WHEN r_city < 0.62 THEN 'Munich'
                    WHEN r_city < 0.83 THEN 'Hamburg'
                    ELSE 'Frankfurt'
                END
            WHEN 'Japan' THEN
                CASE
                    WHEN r_city < 0.45 THEN 'Tokyo'
                    WHEN r_city < 0.78 THEN 'Osaka'
                    ELSE 'Kyoto'
                END
            WHEN 'UK' THEN
                CASE
                    WHEN r_city < 0.55 THEN 'London'
                    WHEN r_city < 0.80 THEN 'Manchester'
                    ELSE 'Birmingham'
                END
            WHEN 'Iceland' THEN 'Reykjavik'
            WHEN 'France' THEN
                CASE WHEN r_city < 0.6 THEN 'Paris' ELSE 'Lyon' END
            WHEN 'Brazil' THEN
                CASE WHEN r_city < 0.6 THEN 'Sao Paulo' ELSE 'Rio de Janeiro' END
            WHEN 'Canada' THEN
                CASE WHEN r_city < 0.6 THEN 'Toronto' ELSE 'Vancouver' END
            WHEN 'Australia' THEN
                CASE WHEN r_city < 0.6 THEN 'Sydney' ELSE 'Melbourne' END
            WHEN 'South Africa' THEN
                CASE WHEN r_city < 0.6 THEN 'Johannesburg' ELSE 'Cape Town' END
        END AS city
    FROM with_country
),

-- ----------------------------------------------------------------------
-- 4. Product category — 10 categories, skewed (not uniform).
--    Electronics 20%, Clothing 15%, Grocery 15%, Furniture 10%,
--    Automotive 8%, Books 8%, Software 8%, Industrial Equipment 6%,
--    Sports 6%, Healthcare 4%.
-- ----------------------------------------------------------------------
with_category AS (
    SELECT
        with_city.*,
        CASE
            WHEN r_category < 0.20 THEN 'Electronics'
            WHEN r_category < 0.35 THEN 'Clothing'
            WHEN r_category < 0.50 THEN 'Grocery'
            WHEN r_category < 0.60 THEN 'Furniture'
            WHEN r_category < 0.68 THEN 'Automotive'
            WHEN r_category < 0.76 THEN 'Books'
            WHEN r_category < 0.84 THEN 'Software'
            WHEN r_category < 0.90 THEN 'Industrial Equipment'
            WHEN r_category < 0.96 THEN 'Sports'
            ELSE 'Healthcare'
        END AS product_category
    FROM with_city
),

-- ----------------------------------------------------------------------
-- 5. Status — COMPLETED 65%, PENDING 15%, CANCELLED 10%, FAILED 6%,
--    REFUNDED 4%.
-- ----------------------------------------------------------------------
with_status AS (
    SELECT
        with_category.*,
        CASE
            WHEN r_status < 0.65 THEN 'COMPLETED'
            WHEN r_status < 0.80 THEN 'PENDING'
            WHEN r_status < 0.90 THEN 'CANCELLED'
            WHEN r_status < 0.96 THEN 'FAILED'
            ELSE 'REFUNDED'
        END AS status
    FROM with_category
),

-- ----------------------------------------------------------------------
-- 6. Amount — range ~10 to ~100,000, concentrated in the low/mid range
--    with a smaller long tail of high-value orders (useful later for
--    `WHERE amount > 90000` / histogram estimation experiments).
--    95% of orders fall in a "normal" 10..5,000 band (squared falloff
--    keeps most of them near the low end); 5% fall in a "high value"
--    5,000..100,000 band (cubed falloff keeps most of THOSE near the
--    low end of that band too, so truly huge orders stay rare).
-- ----------------------------------------------------------------------
with_amount AS (
    SELECT
        with_status.*,
        CASE
            WHEN r_amount_tier < 0.95 THEN
                round((10    + (5000   - 10)   * power(r_amount_val, 2))::numeric, 2)
            ELSE
                round((5000  + (100000 - 5000) * power(r_amount_val, 3))::numeric, 2)
        END AS amount
    FROM with_status
),

-- ----------------------------------------------------------------------
-- 7. Customer id — ~100,000 distinct customers, skewed so that a
--    minority of customers account for a disproportionate number of
--    orders (realistic cardinality characteristics, repeated ids).
-- ----------------------------------------------------------------------
with_customer AS (
    SELECT
        with_amount.*,
        (1 + floor(power(r_customer, 3) * 99999))::bigint AS customer_id
    FROM with_amount
),

-- ----------------------------------------------------------------------
-- 8. created_at — spread over the previous ~2 years (24 months), with
--    a non-uniform month weighting (some months busier than others,
--    via a repeating seasonal sine-based weight) rather than a flat
--    uniform spread. Day-of-month and time-of-day are then chosen
--    deterministically within the selected month.
-- ----------------------------------------------------------------------
months AS (
    SELECT
        m AS month_offset,                                   -- 0 = current month, 23 = 23 months ago
        (1 + 0.5 * sin(m * pi() / 6.0))::numeric AS weight    -- seasonal weight, repeats every 12 months
    FROM generate_series(0, 23) AS m
),
months_cum AS (
    SELECT
        month_offset,
        (SUM(weight) OVER (ORDER BY month_offset) - weight) / SUM(weight) OVER () AS lo,
        (SUM(weight) OVER (ORDER BY month_offset))          / SUM(weight) OVER () AS hi
    FROM months
),
with_month AS (
    SELECT
        with_customer.*,
        mc.month_offset
    FROM with_customer
    JOIN months_cum mc
      ON with_customer.r_date_month >= mc.lo
     AND (with_customer.r_date_month < mc.hi OR mc.month_offset = 23)
),
final AS (
    SELECT
        customer_id,
        country,
        city,
        product_category,
        status,
        amount,
        (
            date_trunc('month', CURRENT_DATE)::date
            - (month_offset || ' months')::interval
            + (floor(r_date_day * 27))::int * interval '1 day'
            + (floor(r_date_time * 86400))::int * interval '1 second'
        )::timestamp AS created_at
    FROM with_month
)

INSERT INTO orders (customer_id, country, city, product_category, status, amount, created_at)
SELECT customer_id, country, city, product_category, status, amount, created_at
FROM final;

COMMIT;

-- ============================================================================
-- VERIFICATION QUERIES
-- Run these after loading to confirm the dataset has the intended shape
-- and skew. These are read-only and safe to re-run at any time.
-- ============================================================================

-- 1. Total row count (expect 1,000,000)
SELECT COUNT(*) AS total_rows FROM orders;

-- 2. Country distribution (expect India ~70%, Iceland ~120 rows / ~0.012%)
SELECT
    country,
    COUNT(*) AS row_count,
    round(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 4) AS pct
FROM orders
GROUP BY country
ORDER BY COUNT(*) DESC;

-- 3. Status distribution (expect COMPLETED ~65%, REFUNDED ~4%, etc.)
SELECT
    status,
    COUNT(*) AS row_count,
    round(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 4) AS pct
FROM orders
GROUP BY status
ORDER BY COUNT(*) DESC;

-- 4. Product category distribution (expect Electronics highest, Healthcare lowest)
SELECT
    product_category,
    COUNT(*) AS row_count,
    round(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 4) AS pct
FROM orders
GROUP BY product_category
ORDER BY COUNT(*) DESC;

-- 5. Distinct customer count (expect up to ~100,000, likely fewer distinct
--    values actually used due to skew, with many repeats per customer)
SELECT COUNT(DISTINCT customer_id) AS distinct_customers FROM orders;

-- 5b. Customer order-count skew — top 10 busiest customers
SELECT customer_id, COUNT(*) AS order_count
FROM orders
GROUP BY customer_id
ORDER BY order_count DESC
LIMIT 10;

-- 6. Amount range and average (expect min ~10, max close to 100000, mean well
--    below the midpoint due to the low-value concentration)
SELECT
    MIN(amount)          AS min_amount,
    MAX(amount)           AS max_amount,
    round(AVG(amount), 2)  AS avg_amount,
    COUNT(*) FILTER (WHERE amount > 90000) AS rows_above_90000
FROM orders;

-- 7. created_at range (expect roughly a 2-year span ending near CURRENT_DATE)
SELECT
    MIN(created_at) AS earliest_order,
    MAX(created_at) AS latest_order
FROM orders;

-- 7b. Orders per month — confirms months are NOT uniformly populated
SELECT
    date_trunc('month', created_at)::date AS order_month,
    COUNT(*) AS row_count
FROM orders
GROUP BY 1
ORDER BY 1;
