package com.holmsted.gerrit.downloaders;

import com.google.common.base.Preconditions;
import com.holmsted.gerrit.GerritVersion;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GerritCommitData {
    private final List<JSONObject> commits = new ArrayList<>();
    private final GerritMetaData metadata;

    @Nonnull
    private final GerritVersion gerritVersion;


    public GerritCommitData(@Nonnull String commits, @Nonnull GerritVersion gerritVersion) {
        this.gerritVersion = gerritVersion;

        List<String> strings = Arrays.asList(commits.split("\n"));
        String lastLine = strings.get(strings.size() - 1);
        this.metadata = Preconditions.checkNotNull(GerritMetaData.fromJSONString(lastLine, gerritVersion));

        for (int i = 0; i < strings.size() - 1; ++i) {
            this.commits.add(new JSONObject(strings.get(i)));
        }
    }

    public GerritMetaData getMetaData() {
        return metadata;
    }

    public List<JSONObject> getCommits() {
        return commits;
    }
}