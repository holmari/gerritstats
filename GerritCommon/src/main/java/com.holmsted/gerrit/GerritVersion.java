package com.holmsted.gerrit;

import javax.annotation.Nullable;

public class GerritVersion {
    public final int major;
    public final int minor;
    public final int patch;

    public static GerritVersion fromString(String versionString) {
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

        return new GerritVersion(major, minor, patch);
    }

    public static GerritVersion makeInvalid() {
        return new GerritVersion(-1, -1, -1);
    }

    private GerritVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    private static int safeValueOf(@Nullable String numberAsString) {
        try {
            if (numberAsString != null) {
                return Integer.valueOf(numberAsString);
            } else {
                return -1;
            }
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public boolean isAtLeast(int expectedMajor, int expectedMinor) {
        return this.major >= expectedMajor && this.minor >= expectedMinor;
    }

    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }
}
