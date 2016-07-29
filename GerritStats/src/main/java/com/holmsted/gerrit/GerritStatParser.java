package com.holmsted.gerrit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.holmsted.json.JsonUtils;

public class GerritStatParser {

    public List<Commit> parseCommits(String jsonCommitData) {
        List<Commit> commits;
        try {
            JSONObject object = JsonUtils.readJsonString(jsonCommitData);
            int gerritStatsVersion = object.optInt("gerritStatsVersion");
            if (gerritStatsVersion == 0) {
                commits = parseLegacyFormatData(jsonCommitData);
            } else {
                commits = parseJsonObject(object);
            }
        } catch(JSONException e) {
            // the earlier versions of GerritDownloader output were not valid json, but
            // instead files with line-by-line json.
            commits = parseLegacyFormatData(jsonCommitData);
        }

        return commits;
    }

    private List<Commit> parseJsonObject(JSONObject rootObject) {
        List<Commit> commits = new ArrayList<>();

        JSONArray jsonCommits = rootObject.getJSONArray("commits");
        for (int i = 0; i < jsonCommits.length(); ++i) {
            JSONObject jsonCommit = jsonCommits.getJSONObject(i);
            if (Commit.isCommit(jsonCommit)) {
                commits.add(Commit.fromJson(jsonCommit));
            }
        }

        return commits;
    }

    List<Commit> parseLegacyFormatData(String jsonCommitData) {
        System.out.println("Using legacy file format parser for GerritStats .json file(s).");
        System.out.println("Rerun GerritDownloader to start using the new format.");

        List<Commit> commits = new ArrayList<>();
        String[] lines = jsonCommitData.split("\n");
        for (String line : lines) {
            try {
                JSONObject lineJson = JsonUtils.readJsonString(line);
                if (Commit.isCommit(lineJson)) {
                    commits.add(Commit.fromJson(lineJson));
                    // ignore the stats, log the rest in case the format changes
                } else if (!lineJson.get("type").equals("stats")) {
                    System.err.println("Ignored line " + line);
                }
            } catch (JSONException ex) {
                System.err.println(String.format("Not JsonObject: '%s'", line));
            }
        }
        return commits;
    }
}
