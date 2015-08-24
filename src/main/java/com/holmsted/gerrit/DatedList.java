package com.holmsted.gerrit;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class DatedList<T> extends ArrayList<T> {
    private static final int[] MONTHS_IN_YEAR = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

    private long minDate = Long.MAX_VALUE;
    private long maxDate = Long.MIN_VALUE;

    @Nonnull
    private final DateTimeProvider<T> dateTimeProvider;

    private final Hashtable<Integer, YearlyItemList<T>> itemsPerYear = new Hashtable<>();

    public DatedList(@Nonnull DateTimeProvider<T> dateTimeProvider) {
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public boolean add(T item) {
        long date = dateTimeProvider.getDate(checkNotNull(item));
        DateTime itemDateTime = new DateTime(date);
        int year = itemDateTime.getYear();

        minDate = Math.min(minDate, date);
        maxDate = Math.max(maxDate, date);

        YearlyItemList<T> yearlyItems = itemsPerYear.get(year);
        if (yearlyItems == null) {
            yearlyItems = new YearlyItemList<>(dateTimeProvider);
        }
        yearlyItems.add(item);
        itemsPerYear.put(year, yearlyItems);
        return super.add(item);
    }

    public List<Integer> getYearRange() {
        List<Integer> years = new ArrayList<>();
        if (minDate == Long.MAX_VALUE || maxDate == Long.MIN_VALUE) {
            return years;
        }
        int minYear = new DateTime(minDate).getYear();
        int maxYear = new DateTime(maxDate).getYear();
        for (int year = minYear; year <= maxYear; ++year) {
            years.add(year);
        }
        return years;
    }

    public YearlyItemList<T> getItemsForYear(int year) {
        return itemsPerYear.get(year);
    }

    public String getDisplayableMonthOnMonthChange(int year, int month) {
        if (!isDateWithinRange(year, month)) {
            return "";
        }

        if (month > 1) {
            return itemsPerYear.get(year).getDisplayableMonthOnMonthChange(month);
        }

        YearlyItemList items = itemsPerYear.get(year);
        YearlyItemList prevYearItems = itemsPerYear.get(year - 1);
        if (prevYearItems == null || items == null) {
            return MonthlyTimeFormat.formatFloat(Float.NaN);
        } else {
            int itemsForLastMonthOfPrevYear = prevYearItems.getMonthlyItemCount(12);
            int itemsForFirstMonth = items.getMonthlyItemCount(1);
            return MonthlyTimeFormat.formatRateOfChange(itemsForLastMonthOfPrevYear, itemsForFirstMonth);
        }
    }

    public String getDisplayableQuarterOnQuarterChange(int year, int month) {
        if (!isDateWithinRange(year, month)) {
            return "";
        }

        int quarter = MonthlyTimeFormat.monthToQuarter(month);
        if (quarter > 0) {
            return itemsPerYear.get(year).getDisplayableQuarterOnQuarterChange(month);
        }

        YearlyItemList items = itemsPerYear.get(year);
        YearlyItemList prevYearItems = itemsPerYear.get(year - 1);
        if (prevYearItems == null || items == null) {
            return MonthlyTimeFormat.formatFloat(Float.NaN);
        } else {
            int itemsForLastQuarterOfPrevYear = prevYearItems.getQuarterlyItemCount(3);
            int itemsForFirstQuarter = items.getQuarterlyItemCount(0);
            return MonthlyTimeFormat.formatRateOfChange(itemsForLastQuarterOfPrevYear, itemsForFirstQuarter);
        }
    }

    public String getPrintableMonthlyItemCount(int year, int month) {
        if (!isDateWithinRange(year, month)) {
            return "";
        }
        YearlyItemList<T> ts = itemsPerYear.get(year);
        return itemsPerYear.get(year).getPrintableMonthlyItemCount(month);
    }

    public static int[] getMonthsInYear() {
        return MONTHS_IN_YEAR;
    }

    private boolean isDateWithinRange(int year, int month) {
        if (minDate == Long.MAX_VALUE || maxDate == Long.MIN_VALUE) {
            return false;
        }

        DateTime earliestDate = new DateTime(minDate);
        DateTime lastDate = new DateTime(maxDate);
        DateTime expectedLatestDate = new DateTime(year, month, 1, 23, 59);
        DateTime expectedEarliestDate = expectedLatestDate.dayOfMonth().setCopy(
                expectedLatestDate.dayOfMonth().getMaximumValue());

        return !MonthlyTimeFormat.isPastCurrentDate(year, month)
                && (earliestDate.isBefore(expectedEarliestDate)
                &&  lastDate.isAfter(expectedLatestDate));
    }
}
