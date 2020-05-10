package com.holmsted.gerrit.downloaders.ssh;

import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.GerritVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

public class SshCommand {
    @Nonnull
    private final GerritServer gerritServer;


    public static List<String> getProjectList(@Nonnull GerritServer gerritServer) {
        SshCommand sshCommand = new SshCommand(gerritServer);
        String output = sshCommand.exec("ls-projects");

        List<String> projectList = new ArrayList<>();
        Collections.addAll(projectList, output.split("\n"));
        return projectList;
    }

    public static GerritVersion getServerVersion(@Nonnull GerritServer gerritServer) {
        SshCommand sshCommand = new SshCommand(gerritServer);
        String output = sshCommand.exec("version");
        if (output != null) {
            return GerritVersion.fromString(output.substring(output.lastIndexOf(' ') + 1));
        } else {
            return null;
        }
    }


    public SshCommand(@Nonnull GerritServer gerritServer) {
        this.gerritServer = gerritServer;
    }

    public String exec(@Nonnull String gerritCommand) {
        Runtime runtime = Runtime.getRuntime();
        try {
            String privateKey = gerritServer.getPrivateKey();
            String command = String.format("ssh -p %s %s %s gerrit %s",
                    String.valueOf(gerritServer.getPort()),
                            privateKey != null ? "-i " + privateKey : "",
                            gerritServer.getServerName(),
                    gerritCommand);
            System.out.println(command);

            Process exec = runtime.exec(command, null);

            char[] buffer = new char[1024];
            int readChars;

            BufferedReader readerOut = new BufferedReader(new InputStreamReader(exec.getInputStream(), "UTF-8"));
            StringBuilder output = new StringBuilder();
            while ((readChars = readerOut.read(buffer)) != -1) {
                output.append(String.copyValueOf(buffer, 0, readChars));
            }
            readerOut.close();

            BufferedReader readerErr = new BufferedReader(new InputStreamReader(exec.getErrorStream(), "UTF-8"));
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

            return output.toString();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
