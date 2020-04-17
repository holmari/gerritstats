package com.holmsted;

import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;

public final class RandomLists {

    private static final Random RANDOM_GENERATOR = new Random();

    public static int randomInt(int maxValue) {
        return RANDOM_GENERATOR.nextInt(maxValue);
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
