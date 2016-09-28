package com.holmsted.gerrit;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class DatedList<T> extends ArrayList<T> {
    private long minDate = Long.MAX_VALUE;
    private long maxDate = Long.MIN_VALUE;

    @Nonnull
    private final DateTimeProvider<T> dateTimeProvider;

    private final Map<Integer, YearlyItemList<T>> itemsPerYear = new Hashtable<>();

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
}
