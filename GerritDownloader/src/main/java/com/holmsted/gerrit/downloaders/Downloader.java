package com.holmsted.gerrit.downloaders;

import com.holmsted.file.FileReader;
import com.holmsted.file.FileWriter;
import com.holmsted.gerrit.CommandLineParser;
import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.GerritVersion;
import com.holmsted.gerrit.downloaders.ssh.SshDownloaderFactory;
import com.holmsted.gerrit.downloaders.ssh.SshCommand;

import com.holmsted.json.JsonUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.holmsted.gerrit.downloaders.GerritCommitDownloader.NO_COMMIT_LIMIT;


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

        gerritVersion = SshCommand.getServerVersion(gerritServer);
        if (gerritVersion == null) {
            System.out.println("Could not query for Gerrit version, aborting.");
            System.out.println("Are you sure the server name is correct, and that you are connected to it?");
            return;
        }

        for (String projectName : projectNames) {
            GerritCommitDownloader downloader = createDownloader();
            downloader.setOverallCommitLimit(commandLine.getCommitLimit());
            downloader.setAfterDate(commandLine.getAfterDate());
            downloader.setBeforeDate(commandLine.getBeforeDate());
            downloader.setProjectName(projectName);

            if (downloader.getOverallCommitLimit() != NO_COMMIT_LIMIT) {
                System.out.println(String.format("Reading data from %s for last %d commits",
                        downloader.getGerritServer(), downloader.getOverallCommitLimit()));
            } else {
                System.out.println("Reading all commit data from " + downloader.getGerritServer());
            }

            String outputFile = getProjectDataFile(projectName);
            new File(outputFile).delete();

            // Download

            while (downloader.hasMoreData()) {
                GerritCommitData commits = downloader.readData();

                // Write to file

                if (commits.getCommits().isEmpty()) {
                    System.out.println(String.format("No output was generated for project '%s'", projectName));
                } else {
                    System.out.println("Writing output to: " + outputFile);
                    writeJsonFile(outputFile, commits.getCommits());
                }
            }
        }
    }

    private void writeJsonFile(@Nonnull String outputFilename, @Nonnull List<JSONObject> data) {
        JSONObject root = new JSONObject();
        File file = new File(outputFilename);

        if (file.exists()) {
            root = JsonUtils.readJsonString(FileReader.readFile(file.getAbsolutePath()));
        }

        root.put("gerritStatsVersion", FILE_FORMAT_VERSION);
        root.put("gerritVersion", gerritVersion.toString());

        if (!root.has("commits"))
            root.put("commits", Collections.emptyList());

        JSONArray commitsArray = root.getJSONArray("commits");
        for (JSONObject obj : data) {
            commitsArray.put(obj);
        }

        FileWriter.writeFile(outputFilename, root.toString());
    }

    private GerritProjectLister createProjectLister() {
        return SshDownloaderFactory.createProjectLister(gerritServer, checkNotNull(gerritVersion));
    }

    private GerritCommitDownloader createDownloader() {
        return SshDownloaderFactory.createCommitDownloader(gerritServer, checkNotNull(gerritVersion));
    }

    @Nonnull
    private String getProjectDataFile(@Nonnull String projectName) {
        String outputDir = checkNotNull(commandLine.getOutputDir());

        return outputDir + File.separator + sanitizeFilename(projectName) + ".json";
    }

    @Nonnull
    private static String sanitizeFilename(@Nonnull String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
