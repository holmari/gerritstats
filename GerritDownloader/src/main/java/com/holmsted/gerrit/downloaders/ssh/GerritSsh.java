package com.holmsted.gerrit.downloaders.ssh;

import com.holmsted.gerrit.GerritServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

public class GerritSsh {

    public static List<String> listProjects(@Nonnull GerritServer gerritServer) {
        GerritSshCommand sshCommand = new GerritSshCommand(gerritServer);
        String output = sshCommand.exec("ls-projects");

        List<String> projectList = new ArrayList<>();
        Collections.addAll(projectList, output.split("\n"));
        return projectList;
    }

    public static Version version(@Nonnull GerritServer gerritServer) {
        GerritSshCommand sshCommand = new GerritSshCommand(gerritServer);
        String output = sshCommand.exec("version");

        return Version.fromString(output.substring(output.lastIndexOf(' ') + 1));
    }

    public static class Version {
        public final int major;
        public final int minor;
        public final int patch;

        public static Version fromString(String versionString) {
            // output format for new Gerrit versions: "gerrit version X.YY.ZZ"
            String[] versionParts = versionString.trim().split("\\.");
            int major = Integer.valueOf(versionParts[0]);
            int minor;
            int patch;

            if (versionParts.length >= 3) {
                minor = safeValueOf(versionParts[1]);
                patch = safeValueOf(versionParts[2]);
            } else if (versionParts.length == 2) {
                // Output format for older Gerrit versions: "gerrit version X.YY-ZZ-gitsha"
                String[] dotVersionParts = versionParts[1].split("-");
                minor = safeValueOf(dotVersionParts[0]);
                patch = safeValueOf(dotVersionParts[1]);
            } else {
                minor = -1;
                patch = -1;
                System.err.println(String.format("Unknown version string '%s'", versionString));
            }

            return new Version(major, minor, patch);
        }

        private Version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        private static int safeValueOf(String numberAsString) {
            try {
                return Integer.valueOf(numberAsString);
            } catch (NumberFormatException ex) {
                return -1;
            }
        }

        public boolean isAtLeast(int expectedMajor, int expectedMinor) {
            return this.major >= expectedMajor && this.minor >= expectedMinor;
        }
    }
}
