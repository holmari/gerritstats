package com.holmsted.gerrit;

import org.joda.time.DateTime;

import java.text.NumberFormat;

public class MonthlyTimeFormat {
    public static boolean isPastCurrentDate(int year, int month) {
        DateTime currentDate = new DateTime();
        return year >= currentDate.getYear() && month > currentDate.getMonthOfYear();
    }

    public static NumberFormat getNumberFormatter() {
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(1);
        return percentFormat;
    }

    public static String formatRateOfChange(float prevValue, float nextValue) {
        return formatFloat(getSafeRateOfChange(prevValue, nextValue));
    }

    public static String formatFloat(float rateOfChange) {
        if (!Float.isNaN(rateOfChange)) {
            return getNumberFormatter().format(rateOfChange);
        } else {
            return "N/A";
        }
    }

    public static float getSafeRateOfChange(float prevValue, float currentValue) {
        if (prevValue == currentValue) {
            return 0;
        } else if (prevValue != 0) {
            float delta = (currentValue / prevValue);
            return delta < 1 ? -(1 - delta) : delta - 1;
        } else {
            return currentValue > 0 ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        }
    }

    /**
     * Converts a 1-based month [1..12] to 0-based quarter [0..3].
     */
    public static int monthToQuarter(int month) {
        return ((month - 1) / 3);
    }
}
