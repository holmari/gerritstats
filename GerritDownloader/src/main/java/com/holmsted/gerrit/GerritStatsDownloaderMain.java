package com.holmsted.gerrit;

import com.holmsted.file.FileWriter;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class GerritStatsDownloaderMain {

    public static void main(String[] args) {
        CommandLineParser commandLine = new CommandLineParser();
        if (!commandLine.parse(args)) {
            System.out.println("Reads Gerrit statistics from a server and writes them to a file.");
            commandLine.printUsage();
            System.exit(1);
            return;
        }

        String serverName = checkNotNull(commandLine.getServerName());
        GerritStatReader reader = GerritStatReader.fromCommandLine(serverName, commandLine.getServerPort());
        reader.setCommitLimit(commandLine.getCommitLimit());
        reader.setProjectNames(commandLine.getProjectNames());

        String data = reader.readData();
        if (data.isEmpty()) {
            System.out.println("No output was generated.");
        } else {
            String outputFile = checkNotNull(commandLine.getOutputFile());
            FileWriter.writeFile(outputFile, data);
            System.out.println("Wrote output to " + outputFile);
        }
    }
}
