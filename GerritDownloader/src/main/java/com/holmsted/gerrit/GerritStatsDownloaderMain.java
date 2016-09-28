package com.holmsted.gerrit;

import com.holmsted.gerrit.downloaders.Downloader;

public final class GerritStatsDownloaderMain {

    public static void main(String[] args) {
        CommandLineParser commandLine = new CommandLineParser();
        if (!commandLine.parse(args)) {
            System.out.println("Reads Gerrit statistics from a server and writes them to a file.");
            commandLine.printUsage();
            System.exit(1);
            return;
        }

        Downloader downloader = new Downloader(commandLine);
        downloader.download();
    }

    private GerritStatsDownloaderMain() {
    }
}
