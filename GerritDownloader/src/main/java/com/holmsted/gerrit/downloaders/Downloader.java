package com.holmsted.gerrit.downloaders;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.List;

import javax.annotation.Nonnull;

import org.json.JSONObject;

import com.holmsted.file.FileWriter;
import com.holmsted.gerrit.CommandLineParser;
import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.GerritServer.Cli;
import com.holmsted.gerrit.GerritVersion;
import com.holmsted.gerrit.downloaders.rest.GerritRest;
import com.holmsted.gerrit.downloaders.rest.GerritRestServer;
import com.holmsted.gerrit.downloaders.rest.GerritRestStatsDownloader;
import com.holmsted.gerrit.downloaders.ssh.GerritSsh;
import com.holmsted.gerrit.downloaders.ssh.GerritSshServer;
import com.holmsted.gerrit.downloaders.ssh.GerritSshStatsDownloader;

public class Downloader {

    @Nonnull
    private final CommandLineParser commandLine;
    @Nonnull
    private final GerritServer gerritServer;
    @Nonnull
    private final AbstractGerritMetadataFetcher fetcher;

    public Downloader(@Nonnull CommandLineParser commandLine) {
        this.commandLine = commandLine;
        if ("ssh".equals(this.commandLine.getCliTool())) {
            gerritServer = new GerritSshServer( //
                    commandLine.getServerName(), //
                    commandLine.getServerPort(), //
                    commandLine.getPrivateKey());
            fetcher = new GerritSsh((GerritSshServer) gerritServer);
        } else {
            gerritServer = new GerritRestServer( //
                    commandLine.getServerName(), //
                    commandLine.getServerPort(), //
                    Cli.valueOf(commandLine.getCliTool().toUpperCase()));
            fetcher = new GerritRest((GerritRestServer) gerritServer);
        }
    }

    public void download() {
        List<String> projectNames = commandLine.getProjectNames();
        if (projectNames == null || projectNames.isEmpty())
            projectNames = fetcher.listProjects();

        GerritVersion gerritVersion = fetcher.getVersion();
        if (gerritVersion == null) {
            System.out.println("Could not query for Gerrit version, aborting.");
            System.out.println("Are you sure the server name is correct, and that you are connected to it?");
            return;
        }
        System.out.println("Gerrit version is " + gerritVersion);

        AbstractGerritStatsDownloader downloader = null;
        if ("ssh".equals(this.commandLine.getCliTool()))
            downloader = new GerritSshStatsDownloader((GerritSshServer) gerritServer, gerritVersion);
        else
            downloader = new GerritRestStatsDownloader((GerritRestServer) gerritServer, gerritVersion);

        for (String projectName : projectNames) {
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
                writeJsonFile(outputFilename, downloader.getGerritStatsVersion(), gerritVersion, data);
                System.out.println("Wrote output to " + outputFilename);
            }
        }
    }

    private void writeJsonFile(@Nonnull String outputFilename, int gerritStatsVersion, @Nonnull GerritVersion gerritVersion,
            @Nonnull List<JSONObject> data) {
        JSONObject root = new JSONObject();
        root.put("gerritStatsVersion", gerritStatsVersion);
        root.put("gerritVersion", gerritVersion.toString());
        root.put("commits", data);
        FileWriter.writeFile(outputFilename, root.toString());
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
