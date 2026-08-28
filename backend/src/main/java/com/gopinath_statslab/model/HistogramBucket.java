package com.gopinath_statslab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One bucket in an equal-width histogram for a numeric column */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistogramBucket {
    private int bucketIndex;
    private double bucketLo;
    private double bucketHi;
    private long rowCount;
    private double pct;
}
