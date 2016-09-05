package com.holmsted.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class ResourceReader {

    public static List<String> readResourceFile(@Nonnull String resourceFilename) {
        ClassLoader classLoader = ResourceReader.class.getClassLoader();
        InputStream resourceStream = classLoader.getResourceAsStream(resourceFilename);
        if (resourceStream == null) {
            throw new NullPointerException("No such resource: " + resourceFilename);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream));
        List<String> lines = new ArrayList<>();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }
}
