package com.holmsted.gerrit;

import com.holmsted.file.FileWriter;

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
        String projectName = commandLine.getProjectName();
        if (projectName != null) {
            reader.setProjectName(projectName);
        }

        String data = reader.readData();
        String outputFile = checkNotNull(commandLine.getOutputFile());
        FileWriter.writeFile(outputFile, data);
        System.out.println("Wrote output to " + outputFile);
    }
}
