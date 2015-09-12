package com.holmsted.gerrit;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class YearlyItemList<T> extends ArrayList<T> {

    private final Hashtable<Integer, List<T>> itemsPerMonth = new Hashtable<>();

    @Nonnull
    private final DateTimeProvider<T> dateTimeProvider;

    YearlyItemList(@Nonnull DateTimeProvider<T> dateTimeProvider) {
        this.dateTimeProvider = dateTimeProvider;
        for (int i = 1; i <= 12; ++i) {
            itemsPerMonth.put(i, new ArrayList<T>());
        }
    }

    @Override
    public boolean add(T item) {
        DateTime createdDate = new DateTime(dateTimeProvider.getDate(checkNotNull(item)));
        itemsPerMonth.get(createdDate.getMonthOfYear()).add(item);
        return super.add(item);
    }

    /**
     * Returns a hashtable that maps against a month index,
     * and a list of items within that month.
     * Assumes that the given item range only contains items from within a single year.
     * Call filterForYear() first if you need to filter for both year and month.
     */
    public Hashtable<Integer, List<T>> getMonthlyItems() {
        return itemsPerMonth;
    }

    public float getMonthOnMonthChange(int month) {
        if (month > 1) {
            int itemsInThisMonth = itemsPerMonth.get(month).size();
            int itemsInPrevMonth = itemsPerMonth.get(month - 1).size();
            return MonthlyTimeFormat.getSafeRateOfChange(itemsInPrevMonth, itemsInThisMonth);
        } else {
            return Float.NaN;
        }
    }

    public float getQuarterOnQuarterChange(int month) {
        int quarter = MonthlyTimeFormat.monthToQuarter(month);
        int quarterStartMonth = 1 + (quarter * 3);
        if (quarterStartMonth > 1) {
            int quarterItemCount = getQuarterlyItemCount(quarter);
            int prevQuarterItemCount = 0;
            int prevQuarterStartMonth = quarterStartMonth - 3;

            for (int i = prevQuarterStartMonth; i <= prevQuarterStartMonth + 2; ++i) {
                prevQuarterItemCount += itemsPerMonth.get(i).size();
            }
            return MonthlyTimeFormat.getSafeRateOfChange(prevQuarterItemCount, quarterItemCount);
        } else {
            return Float.NaN;
        }
    }

    public String getDisplayableQuarterOnQuarterChange(int month) {
        return MonthlyTimeFormat.formatFloat(getQuarterOnQuarterChange(month));
    }

    public String getDisplayableMonthOnMonthChange(int month) {
        return MonthlyTimeFormat.formatFloat(getMonthOnMonthChange(month));
    }

    public int getMonthlyItemCount(int month) {
        return itemsPerMonth.get(month).size();
    }

    public String getPrintableMonthlyItemCount(int month) {
        return String.valueOf(itemsPerMonth.get(month).size());
    }

    /**
     * Returns the quarterly item count for zero-based quarter index in [0..3] range.
     */
    public int getQuarterlyItemCount(int quarter) {
        checkArgument(quarter >= 0 && quarter < 4);

        int quarterStartMonth = 1 + (quarter * 3);
        int quarterItemCount = 0;
        for (int i = quarterStartMonth; i <= quarterStartMonth + 2; ++i) {
            quarterItemCount += itemsPerMonth.get(i).size();
        }
        return quarterItemCount;
    }
}
