package com.holmsted.gerrit.downloaders;

import com.holmsted.file.FileWriter;
import com.holmsted.gerrit.CommandLineParser;
import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.downloaders.ssh.GerritSsh;
import com.holmsted.gerrit.GerritVersion;
import com.holmsted.gerrit.downloaders.ssh.SshDownloader;
import com.holmsted.gerrit.downloaders.ssh.SshProjectLister;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class Downloader {

    private static final int FILE_FORMAT_VERSION = 1;

    @Nonnull
    private final CommandLineParser commandLine;
    @Nonnull
    private final GerritServer gerritServer;

    private GerritVersion gerritVersion;

    public Downloader(@Nonnull CommandLineParser commandLine) {
        this.commandLine = commandLine;
        gerritServer = new GerritServer(
                commandLine.getServerName(),
                commandLine.getServerPort(),
                commandLine.getPrivateKey());
    }

    public void download() {
        List<String> projectNames = commandLine.getProjectNames();
        if (projectNames == null || projectNames.isEmpty()) {
            projectNames = createProjectLister().getProjectListing();
        }

        gerritVersion = GerritSsh.version(gerritServer);
        if (gerritVersion == null) {
            System.out.println("Could not query for Gerrit version, aborting.");
            System.out.println("Are you sure the server name is correct, and that you are connected to it?");
            return;
        }

        for (String projectName : projectNames) {
            AbstractGerritStatsDownloader downloader = createDownloader();
            downloader.setOverallCommitLimit(commandLine.getCommitLimit());
            downloader.setAfterDate(commandLine.getAfterDate());
            downloader.setBeforeDate(commandLine.getBeforeDate());
            downloader.setProjectName(projectName);
            List<JSONObject> data = downloader.readData();
            if (data.isEmpty()) {
                System.out.println(String.format("No output was generated for project '%s'", projectName));
            } else {
                String outputDir = checkNotNull(commandLine.getOutputDir());
                String outputFilename = outputDir + File.separator + projectNameToFilename(projectName);
                writeJsonFile(outputFilename, data);
                System.out.println("Wrote output to " + outputFilename);
            }
        }
    }

    private void writeJsonFile(@Nonnull String outputFilename, @Nonnull List<JSONObject> data) {
        JSONObject root = new JSONObject();
        root.put("gerritStatsVersion", FILE_FORMAT_VERSION);
        root.put("gerritVersion", gerritVersion.toString());
        root.put("commits", data);

        FileWriter.writeFile(outputFilename, root.toString());
    }

    private ProjectLister createProjectLister() {
        return new SshProjectLister(gerritServer);
    }

    private AbstractGerritStatsDownloader createDownloader() {
        return new SshDownloader(gerritServer, checkNotNull(gerritVersion));
    }

    @Nonnull
    private static String projectNameToFilename(@Nonnull String projectName) {
        return sanitizeFilename(projectName) + ".json";
    }

    @Nonnull
    private static String sanitizeFilename(@Nonnull String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
