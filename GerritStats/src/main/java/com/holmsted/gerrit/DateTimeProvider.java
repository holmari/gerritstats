package com.holmsted.gerrit;

import javax.annotation.Nonnull;

public interface DateTimeProvider<T> {
    long getDate(@Nonnull T object);
}
