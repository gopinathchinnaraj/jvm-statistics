package com.gopinath_statslab.service;

import com.gopinath_statslab.model.ColumnStats;
import com.gopinath_statslab.model.HistogramBucket;
import com.gopinath_statslab.model.MostCommonValue;
import com.gopinath_statslab.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * StatisticsCollectorService — Phase 2: Investigate the Problem.
 *
 * This service computes real statistics from the orders table
 * and stores them in the StatisticsStore (in-memory cache).
 *
 * It answers the question: "What does the data actually look like?"
 *
 * Computes for each column:
 *   rowCount, distinctCount, nullCount, min, max, avg
 *   Histogram (numeric columns: amount)
 *   Most Common Values (categorical: country, status, product_category)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsCollectorService {

    private final OrderRepository orderRepository;
    private final JdbcTemplate jdbcTemplate;
    private final StatisticsStore statisticsStore;

    /**
     * Collect ALL statistics for all columns.
     * Called from the frontend "Collect Statistics" button.
     * Takes a few seconds on 1M rows.
     */
    public Map<String, ColumnStats> collectAll() {
        log.info("Starting full statistics collection on orders table...");
        long start = System.currentTimeMillis();

        long totalRows = orderRepository.count();

        Map<String, ColumnStats> stats = Map.of(
            "country",          collectForCountry(totalRows),
            "status",           collectForStatus(totalRows),
            "product_category", collectForProductCategory(totalRows),
            "amount",           collectForAmount(totalRows)
        );

        statisticsStore.store(stats);
        log.info("Statistics collection complete in {}ms", System.currentTimeMillis() - start);
        return stats;
    }

    // ── Per-column collectors ────────────────────────────────────

    private ColumnStats collectForCountry(long totalRows) {
        log.debug("Collecting stats for country...");

        Long distinct = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT country) FROM orders", Long.class);

        List<Object[]> mcvRaw = orderRepository.countByCountryGrouped();
        List<MostCommonValue> mcv = toMCV(mcvRaw, totalRows);

        String min = jdbcTemplate.queryForObject("SELECT MIN(country) FROM orders", String.class);
        String max = jdbcTemplate.queryForObject("SELECT MAX(country) FROM orders", String.class);

        return ColumnStats.builder()
                .columnName("country")
                .rowCount(totalRows)
                .distinctCount(distinct != null ? distinct : 0)
                .nullCount(0L)
                .minValue(min)
                .maxValue(max)
                .mostCommonValues(mcv)
                .computedAt(LocalDateTime.now())
                .mode("GOOD")
                .build();
    }

    private ColumnStats collectForStatus(long totalRows) {
        log.debug("Collecting stats for status...");

        Long distinct = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT status) FROM orders", Long.class);

        List<Object[]> mcvRaw = orderRepository.countByStatusGrouped();
        List<MostCommonValue> mcv = toMCV(mcvRaw, totalRows);

        return ColumnStats.builder()
                .columnName("status")
                .rowCount(totalRows)
                .distinctCount(distinct != null ? distinct : 0)
                .nullCount(0L)
                .mostCommonValues(mcv)
                .computedAt(LocalDateTime.now())
                .mode("GOOD")
                .build();
    }

    private ColumnStats collectForProductCategory(long totalRows) {
        log.debug("Collecting stats for product_category...");

        Long distinct = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT product_category) FROM orders", Long.class);

        List<Object[]> mcvRaw = orderRepository.countByProductCategoryGrouped();
        List<MostCommonValue> mcv = toMCV(mcvRaw, totalRows);

        return ColumnStats.builder()
                .columnName("product_category")
                .rowCount(totalRows)
                .distinctCount(distinct != null ? distinct : 0)
                .nullCount(0L)
                .mostCommonValues(mcv)
                .computedAt(LocalDateTime.now())
                .mode("GOOD")
                .build();
    }

    private ColumnStats collectForAmount(long totalRows) {
        log.debug("Collecting stats for amount (histogram)...");

        Double min = jdbcTemplate.queryForObject("SELECT MIN(amount) FROM orders", Double.class);
        Double max = jdbcTemplate.queryForObject("SELECT MAX(amount) FROM orders", Double.class);
        Double avg = jdbcTemplate.queryForObject("SELECT AVG(amount) FROM orders", Double.class);
        Long distinct = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT amount) FROM orders", Long.class);

        List<HistogramBucket> histogram = buildHistogram(
            min != null ? min : 0,
            max != null ? max : 100000,
            20, // 20 equal-width buckets
            totalRows
        );

        return ColumnStats.builder()
                .columnName("amount")
                .rowCount(totalRows)
                .distinctCount(distinct != null ? distinct : 0)
                .nullCount(0L)
                .minValue(min != null ? String.format("%.2f", min) : "0")
                .maxValue(max != null ? String.format("%.2f", max) : "100000")
                .avgValue(avg)
                .histogramBuckets(histogram)
                .computedAt(LocalDateTime.now())
                .mode("GOOD")
                .build();
    }

    /**
     * Build an equal-width histogram with the given number of buckets.
     * Queries the DB for each bucket's row count using BETWEEN.
     */
    private List<HistogramBucket> buildHistogram(double min, double max, int buckets, long totalRows) {
        double bucketWidth = (max - min) / buckets;
        List<HistogramBucket> result = new ArrayList<>();

        for (int i = 0; i < buckets; i++) {
            double lo = min + i * bucketWidth;
            double hi = (i == buckets - 1) ? max + 0.01 : min + (i + 1) * bucketWidth;

            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE amount >= ? AND amount < ?",
                Long.class, lo, hi
            );

            long rowCount = count != null ? count : 0;
            result.add(HistogramBucket.builder()
                    .bucketIndex(i)
                    .bucketLo(lo)
                    .bucketHi(hi)
                    .rowCount(rowCount)
                    .pct(totalRows > 0 ? (double) rowCount / totalRows : 0)
                    .build());
        }
        return result;
    }

    /** Convert [value, count] Object[] from JPQL to MostCommonValue list */
    private List<MostCommonValue> toMCV(List<Object[]> raw, long totalRows) {
        List<MostCommonValue> result = new ArrayList<>();
        for (Object[] row : raw) {
            String value = String.valueOf(row[0]);
            long freq    = ((Number) row[1]).longValue();
            double pct   = totalRows > 0 ? (double) freq / totalRows : 0;
            result.add(MostCommonValue.builder()
                    .value(value).frequency(freq).pct(pct).build());
        }
        return result;
    }
}
