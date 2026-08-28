package com.gopinath_statslab.service;

import com.gopinath_statslab.model.ColumnStats;
import com.gopinath_statslab.model.HistogramBucket;
import com.gopinath_statslab.model.MostCommonValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StatisticsStore — in-memory cache of computed column statistics.
 *
 * Acts as a simple key-value store: columnName → ColumnStats.
 * Has two modes per column: GOOD (real stats) and CORRUPTED (fake stats).
 *
 * The "corrupt" operation deliberately replaces real statistics with
 * wrong ones — this is the core of Phase 4 (What Happens When Stats Lie).
 */
@Slf4j
@Component
public class StatisticsStore {

    /** Holds the currently active statistics (either GOOD or CORRUPTED) */
    private final Map<String, ColumnStats> store = new HashMap<>();

    /** Backup of the original GOOD statistics before corruption */
    private final Map<String, ColumnStats> goodBackup = new HashMap<>();

    private boolean isCorrupted = false;

    // ── Storage ─────────────────────────────────────────────────

    public void store(Map<String, ColumnStats> stats) {
        store.clear();
        store.putAll(stats);
        goodBackup.clear();
        goodBackup.putAll(stats);
        isCorrupted = false;
        log.info("Statistics stored for columns: {}", stats.keySet());
    }

    public ColumnStats get(String columnName) {
        return store.get(columnName);
    }

    public Map<String, ColumnStats> getAll() {
        return Map.copyOf(store);
    }

    public boolean hasStats() {
        return !store.isEmpty();
    }

    public boolean isCorrupted() {
        return isCorrupted;
    }

    // ── Corrupt: replace real stats with deliberately wrong ones ─

    /**
     * Corrupt the statistics — simulate what happens when:
     *   - ANALYZE was never run
     *   - Statistics are stale (data changed, stats didn't)
     *   - The planner has wrong assumptions
     *
     * Corruption strategy:
     *   - country: replace real MCV with uniform distribution (every country = 10%)
     *   - status:  replace real MCV with uniform distribution (every status = 20%)
     *   - amount:  replace histogram with flat buckets (all equally likely)
     */
    public void corrupt() {
        if (store.isEmpty()) {
            log.warn("Cannot corrupt — no statistics collected yet.");
            return;
        }

        // Corrupt country stats
        if (store.containsKey("country")) {
            ColumnStats real = goodBackup.get("country");
            long totalRows = real.getRowCount();

            // Fake MCV: every country gets 10% — reality is India=70%, Iceland=0.012%
            List<MostCommonValue> fakeMCV = List.of(
                MostCommonValue.builder().value("India").frequency(100000L).pct(0.10).build(),
                MostCommonValue.builder().value("USA").frequency(100000L).pct(0.10).build(),
                MostCommonValue.builder().value("Germany").frequency(100000L).pct(0.10).build(),
                MostCommonValue.builder().value("Japan").frequency(100000L).pct(0.10).build(),
                MostCommonValue.builder().value("UK").frequency(100000L).pct(0.10).build(),
                MostCommonValue.builder().value("Iceland").frequency(100000L).pct(0.10).build(),
                MostCommonValue.builder().value("France").frequency(100000L).pct(0.10).build(),
                MostCommonValue.builder().value("Brazil").frequency(100000L).pct(0.10).build(),
                MostCommonValue.builder().value("Canada").frequency(100000L).pct(0.10).build(),
                MostCommonValue.builder().value("Australia").frequency(100000L).pct(0.10).build()
            );

            store.put("country", ColumnStats.builder()
                    .columnName("country")
                    .rowCount(totalRows)
                    .distinctCount(real.getDistinctCount())
                    .nullCount(0L)
                    .minValue(real.getMinValue())
                    .maxValue(real.getMaxValue())
                    .mostCommonValues(fakeMCV)
                    .computedAt(real.getComputedAt())
                    .mode("CORRUPTED")
                    .build());
        }

        // Corrupt status stats
        if (store.containsKey("status")) {
            ColumnStats real = goodBackup.get("status");
            List<MostCommonValue> fakeStatus = List.of(
                MostCommonValue.builder().value("COMPLETED").frequency(200000L).pct(0.20).build(),
                MostCommonValue.builder().value("PENDING").frequency(200000L).pct(0.20).build(),
                MostCommonValue.builder().value("CANCELLED").frequency(200000L).pct(0.20).build(),
                MostCommonValue.builder().value("FAILED").frequency(200000L).pct(0.20).build(),
                MostCommonValue.builder().value("REFUNDED").frequency(200000L).pct(0.20).build()
            );
            store.put("status", ColumnStats.builder()
                    .columnName("status")
                    .rowCount(real.getRowCount())
                    .distinctCount(real.getDistinctCount())
                    .nullCount(0L)
                    .mostCommonValues(fakeStatus)
                    .computedAt(real.getComputedAt())
                    .mode("CORRUPTED")
                    .build());
        }

        // Corrupt amount stats — flat histogram
        if (store.containsKey("amount")) {
            ColumnStats real = goodBackup.get("amount");
            long totalRows = real.getRowCount();
            List<HistogramBucket> flatBuckets = new ArrayList<>();
            double bucketWidth = 5000;
            for (int i = 0; i < 20; i++) {
                flatBuckets.add(HistogramBucket.builder()
                        .bucketIndex(i)
                        .bucketLo(i * bucketWidth)
                        .bucketHi((i + 1) * bucketWidth)
                        .rowCount(totalRows / 20)
                        .pct(0.05)
                        .build());
            }
            store.put("amount", ColumnStats.builder()
                    .columnName("amount")
                    .rowCount(totalRows)
                    .distinctCount(real.getDistinctCount())
                    .nullCount(0L)
                    .minValue(real.getMinValue())
                    .maxValue(real.getMaxValue())
                    .avgValue(real.getAvgValue())
                    .histogramBuckets(flatBuckets)
                    .computedAt(real.getComputedAt())
                    .mode("CORRUPTED")
                    .build());
        }

        isCorrupted = true;
        log.warn("Statistics have been CORRUPTED for demo purposes.");
    }

    /** Restore the original good statistics */
    public void restore() {
        store.clear();
        store.putAll(goodBackup);
        isCorrupted = false;
        log.info("Statistics restored to GOOD state.");
    }
}
