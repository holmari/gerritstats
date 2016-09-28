package com.holmsted.gerrit;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class YearlyItemList<T> extends ArrayList<T> {

    private final Map<Integer, List<T>> itemsPerMonth = new Hashtable<>();

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
}
