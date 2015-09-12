package com.holmsted.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class FileWriter {

    public static void writeFile(String filename, String contents) {
        File dataFile = new File(filename);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(dataFile);
            Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(contents);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeFile(String filename, InputStream inputStream) {
        File dataFile = new File(filename);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(dataFile);
            byte[] buffer = new byte[4096];
            int readBytes;
            while ((readBytes = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, readBytes);
            }
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
