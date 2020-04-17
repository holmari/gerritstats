package com.holmsted.gerrit.downloaders.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.holmsted.gerrit.downloaders.CommandRunner;

public class GerritRestCommand extends CommandRunner {

    // See https://gerrit-review.googlesource.com/Documentation/rest-api.html
    public static String GERRIT_XSSI_BLOCKER = ")]}'\n";

    @Nonnull
    private final GerritRestServer gerritServer;

    public GerritRestCommand(@Nonnull GerritRestServer gerritServer) {
        this.gerritServer = gerritServer;
    }

    /**
     * Executes a command to query the Gerrit server via a command-line tool.
     * 
     * @param urlSuffix Path starting with slash; e.g., /r/projects?d
     * @return Output from command but without the XSSI prefix; null on error.
     */
    public String exec(@Nonnull String urlSuffix) {
        URL url = null;
        try {
            url = new URL("https", gerritServer.getServerName(), gerritServer.getPort(), urlSuffix);
        } catch (MalformedURLException ex) {
            System.err.println("Failed to construct URL using suffix " + urlSuffix);
            return null;
        }
        List<String> l = new ArrayList<>();
        for (String c : gerritServer.getCli().getCommands())
            l.add(c);
        l.add(url.toExternalForm());
        String[] commandArray = l.toArray(new String[] {});
        System.out.println(commandArrayToString(commandArray));
        String output = runCommand(commandArray);
        if (output != null && output.startsWith(GERRIT_XSSI_BLOCKER))
            return output.substring(GERRIT_XSSI_BLOCKER.length());
        return output;
    }
}
