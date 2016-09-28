package com.holmsted;

import java.util.List;

import javax.annotation.Nonnull;

public final class RandomLists {

    public static int randomInt(int maxValue) {
        return (int) (Math.random() * maxValue);
    }

    public static <T> T randomItemFrom(@Nonnull List<T> items) {
        return items.get(randomInt(items.size()));
    }

    public static <T> T randomItemFrom(@Nonnull T[] list) {
        return list[randomInt(list.length)];
    }

    private RandomLists() {
    }
}
