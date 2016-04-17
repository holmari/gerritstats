package com.holmsted.gerrit;

import com.google.common.base.Strings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Creates a listing of all Gerrit projects on the given server.
 */
public class GerritProjectLister {

    @Nonnull
    private final GerritServer gerritServer;

    public GerritProjectLister(@Nonnull GerritServer gerritServer) {
        this.gerritServer = gerritServer;
    }

    @Nonnull
    public List<String> getProjectListing() {
        List<String> projectList = new ArrayList<>();

        Runtime runtime = Runtime.getRuntime();
        try {
            String command = String.format("ssh -p %s %s gerrit ls-projects",
                    String.valueOf(gerritServer.getPort()), gerritServer.getServerName());
            System.out.println(command);

            Process exec = runtime.exec(command, null);

            char[] buffer = new char[1024];
            int readChars;

            BufferedReader readerOut = new BufferedReader(new InputStreamReader(exec.getInputStream()));
            StringBuilder output = new StringBuilder();
            while ((readChars = readerOut.read(buffer)) != -1) {
                output.append(String.copyValueOf(buffer, 0, readChars));
            }
            readerOut.close();

            BufferedReader readerErr = new BufferedReader(new InputStreamReader(exec.getErrorStream()));
            StringBuilder error = new StringBuilder();
            while ((readChars = readerErr.read(buffer)) != -1) {
                error.append(String.copyValueOf(buffer, 0, readChars));
            }
            readerErr.close();

            int errorCode = exec.waitFor();
            if (errorCode != 0) {
                System.err.println("Process exited with return code " + errorCode + " and output:");
                System.err.println(error);
                return null;
            }

            Collections.addAll(projectList, output.toString().split("\n"));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return projectList;
    }
}
