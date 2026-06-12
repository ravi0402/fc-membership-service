package com.firstclub.membership.catalog.domain;

import java.math.BigDecimal;

/** Numeric comparison used by threshold-based criteria. */
public enum ComparisonOperator {
    GTE, GT, LTE, LT, EQ;

    /** @return true if {@code actual <op> threshold}. */
    public boolean test(BigDecimal actual, BigDecimal threshold) {
        int cmp = actual.compareTo(threshold);
        return switch (this) {
            case GTE -> cmp >= 0;
            case GT -> cmp > 0;
            case LTE -> cmp <= 0;
            case LT -> cmp < 0;
            case EQ -> cmp == 0;
        };
    }
}
