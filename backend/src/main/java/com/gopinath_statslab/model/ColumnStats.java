package com.gopinath_statslab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ColumnStats — statistics computed for ONE column of the orders table.
 * Stored in memory by StatisticsStore and served to the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnStats {

    /** e.g. "country", "amount", "status" */
    private String columnName;

    /** Total rows in the table (same for every column) */
    private long rowCount;

    /** Number of distinct values in this column */
    private long distinctCount;

    /** Number of NULL values */
    private long nullCount;

    /** Minimum value as string (works for text, numeric, date) */
    private String minValue;

    /** Maximum value as string */
    private String maxValue;

    /** Average value — only meaningful for numeric columns, null otherwise */
    private Double avgValue;

    /** When these stats were computed */
    private LocalDateTime computedAt;

    /**
     * Most Common Values — top-N (value, count, pct) tuples.
     * Populated only for low-cardinality columns (country, status, category).
     */
    private List<MostCommonValue> mostCommonValues;

    /**
     * Histogram buckets — populated only for numeric columns (amount).
     */
    private List<HistogramBucket> histogramBuckets;

    /**
     * STATISTICS MODE — whether these stats are real or corrupted.
     * "GOOD"      — accurate, freshly computed
     * "CORRUPTED" — deliberately wrong (for the demo)
     * "NONE"      — not yet computed
     */
    private String mode;

    // ── Convenience helpers ──────────────────────────────────────

    /** Selectivity estimate for a specific value using MCV or uniform fallback */
    public double estimateSelectivity(String value) {
        if (mostCommonValues == null || mostCommonValues.isEmpty()) {
            // No MCV → uniform distribution assumption
            return distinctCount > 0 ? 1.0 / distinctCount : 0.1;
        }
        return mostCommonValues.stream()
                .filter(mcv -> mcv.getValue().equalsIgnoreCase(value))
                .mapToDouble(MostCommonValue::getPct)
                .findFirst()
                .orElse(1.0 / distinctCount); // value not in MCV → rare
    }

    /** Selectivity estimate for amount > threshold using histogram */
    public double estimateSelectivityGreaterThan(double threshold) {
        if (histogramBuckets == null || histogramBuckets.isEmpty()) {
            // No histogram → uniform fallback
            double min = minValue != null ? Double.parseDouble(minValue) : 0;
            double max = maxValue != null ? Double.parseDouble(maxValue) : 100000;
            return Math.max(0, (max - threshold) / (max - min));
        }
        long totalInHistogram = histogramBuckets.stream().mapToLong(HistogramBucket::getRowCount).sum();
        long rowsAbove = histogramBuckets.stream()
                .filter(b -> b.getBucketHi() > threshold)
                .mapToLong(b -> {
                    if (b.getBucketLo() >= threshold) return b.getRowCount();
                    // Partial bucket — linear interpolation
                    double fraction = (b.getBucketHi() - threshold) / (b.getBucketHi() - b.getBucketLo());
                    return Math.round(b.getRowCount() * fraction);
                })
                .sum();
        return totalInHistogram > 0 ? (double) rowsAbove / totalInHistogram : 0;
    }
}
