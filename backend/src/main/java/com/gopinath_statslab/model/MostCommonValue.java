package com.gopinath_statslab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One entry in the Most Common Values list for a categorical column */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MostCommonValue {
    private String value;
    private long frequency;
    /** fraction of total rows, e.g. 0.70 for India */
    private double pct;
}
