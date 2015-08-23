package com.holmsted.gerrit;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import jsonutils.JsonUtils;

public class GerritStatParser {

    public List<Commit> parseCommits(String jsonCommitData) {
        List<Commit> commits = new ArrayList<>();
        String[] lines = jsonCommitData.split("\n");
        for (String line : lines) {
            try {
                JSONObject lineJson = JsonUtils.readJsonString(line);
                if (Commit.isCommit(lineJson)) {
                    commits.add(Commit.fromJson(lineJson));
                } else {
                    // TODO ignore for now
                    System.err.println("Ignored line " + line);
                }
            } catch (JSONException ex) {
                System.err.println(String.format("Not JsonObject: '%s'", line));
            }
        }

        return commits;
    }
}
