package com.holmsted.gerrit;

import com.holmsted.file.FileWriter;

import java.io.File;
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

        GerritServer gerritServer = new GerritServer(commandLine.getServerName(),
                commandLine.getServerPort());

        List<String> projectNames = commandLine.getProjectNames();
        if (projectNames == null || projectNames.isEmpty()) {
            projectNames = new GerritProjectLister(gerritServer).getProjectListing();
        }

        for (String projectName : projectNames) {
            GerritStatReader reader = new GerritStatReader(gerritServer);
            reader.setCommitLimit(commandLine.getCommitLimit());
            reader.setProjectName(projectName);
            String data = reader.readData();
            if (data.isEmpty()) {
                System.out.println(String.format("No output was generated for project '%s'", projectName));
            } else {
                String outputDir = checkNotNull(commandLine.getOutputDir());
                String outputFilename = outputDir + File.separator + projectNameToFilename(projectName);
                FileWriter.writeFile(outputFilename, data);
                System.out.println("Wrote output to " + outputFilename);
            }
        }
    }

    private static String projectNameToFilename(String projectName) {
        return sanitizeFilename(projectName) + ".json";
    }

    private static String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
