package com.holmsted.gerrit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.holmsted.json.JsonUtils;

import javax.annotation.Nonnull;

public class GerritStatParser {

    public static class ParserContext {
        final GerritVersion version;

        ParserContext(@Nonnull GerritVersion version) {
            this.version = version;
        }
    }

    public static class GerritData {
        @Nonnull
        final GerritVersion version;
        @Nonnull
        final List<Commit> commits = new ArrayList<>();

        GerritData(@Nonnull GerritVersion version) {
            this.version = version;
        }
    }


    @Nonnull
    public GerritData parseJsonData(@Nonnull String jsonFileData) {
        GerritData data;
        try {
            JSONObject object = JsonUtils.readJsonString(jsonFileData);
            int gerritStatsVersion = object.optInt("gerritStatsVersion");
            if (gerritStatsVersion == 0) {
                data = parseLegacyFormatData(jsonFileData);
            } else {
                data = parseJsonObject(object);
            }
        } catch (JSONException e) {
            // the earlier versions of GerritDownloader output were not valid json, but
            // instead files with line-by-line json.
            data = parseLegacyFormatData(jsonFileData);
        }

        return data;
    }

    @Nonnull
    private GerritData parseJsonObject(@Nonnull JSONObject rootObject) {
        JSONArray jsonCommits = rootObject.getJSONArray("commits");
        GerritVersion gerritVersion = GerritVersion.fromString(rootObject.getString("gerritVersion"));
        ParserContext context = new ParserContext(gerritVersion);
        GerritData data = new GerritData(gerritVersion);

        for (int i = 0; i < jsonCommits.length(); ++i) {
            JSONObject jsonCommit = jsonCommits.getJSONObject(i);
            if (Commit.isCommit(jsonCommit)) {
                data.commits.add(Commit.fromJson(jsonCommit, context));
            }
        }

        return data;
    }

    @Nonnull
    private GerritData parseLegacyFormatData(@Nonnull String jsonCommitData) {
        System.out.println("Using legacy file format parser for GerritStats .json file(s).");
        System.out.println("This file format has some limitations.");
        System.out.println("Please rerun GerritDownloader to start using the new format.");

        GerritVersion gerritVersion = GerritVersion.makeInvalid();

        GerritData data = new GerritData(gerritVersion);
        ParserContext context = new ParserContext(gerritVersion);

        String[] lines = jsonCommitData.split("\n");
        for (String line : lines) {
            try {
                JSONObject lineJson = JsonUtils.readJsonString(line);
                if (Commit.isCommit(lineJson)) {
                    data.commits.add(Commit.fromJson(lineJson, context));
                    // ignore the stats, log the rest in case the format changes
                } else if (!lineJson.get("type").equals("stats")) {
                    System.err.println("Ignored line " + line);
                }
            } catch (JSONException ex) {
                System.err.println(String.format("Not JsonObject: '%s'", line));
            }
        }
        return data;
    }
}
