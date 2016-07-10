package com.holmsted.gerrit;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.holmsted.json.JsonUtils;

public class GerritStatParser {

    public List<Commit> parseCommits(String jsonCommitData) {
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
