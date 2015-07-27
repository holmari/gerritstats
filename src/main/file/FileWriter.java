package file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
