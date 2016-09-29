package com.holmsted.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public final class ResourceReader {

    public static List<String> readResourceFile(@Nonnull String resourceFilename) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream resourceStream = classLoader.getResourceAsStream(resourceFilename);
        if (resourceStream == null) {
            throw new IllegalArgumentException("No such resource: " + resourceFilename);
        }
        List<String> lines = new ArrayList<>();

        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream, "UTF-8"))) {
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private ResourceReader() {
    }
}
