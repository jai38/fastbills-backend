package com.fastbills.api.receipt;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum AgeingBucket {
    CURRENT,
    DAYS_1_TO_30,
    DAYS_31_TO_45,
    DAYS_46_TO_60,
    DAYS_61_TO_90,
    OVER_90;

    /**
     * Calculates ageing bucket based on days past due date.
     * Counted in days past due_date: current, 1 to 30, 31 to 45, 46 to 60, 61 to 90, over 90.
     */
    public static AgeingBucket calculate(LocalDate dueDate) {
        return calculate(dueDate, LocalDate.now());
    }

    public static AgeingBucket calculate(LocalDate dueDate, LocalDate asOfDate) {
        if (dueDate == null) {
            return CURRENT;
        }
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        long daysPastDue = ChronoUnit.DAYS.between(dueDate, date);

        if (daysPastDue <= 0) {
            return CURRENT;
        } else if (daysPastDue <= 30) {
            return DAYS_1_TO_30;
        } else if (daysPastDue <= 45) {
            return DAYS_31_TO_45;
        } else if (daysPastDue <= 60) {
            return DAYS_46_TO_60;
        } else if (daysPastDue <= 90) {
            return DAYS_61_TO_90;
        } else {
            return OVER_90;
        }
    }
}
