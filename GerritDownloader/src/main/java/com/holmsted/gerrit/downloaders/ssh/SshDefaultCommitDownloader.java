package com.holmsted.gerrit.downloaders.ssh;

import com.google.common.base.Strings;
import com.holmsted.file.FileReader;
import com.holmsted.file.FileWriter;
import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.GerritVersion;
import com.holmsted.gerrit.downloaders.GerritCommitData;
import com.holmsted.gerrit.downloaders.GerritCommitDownloader;

import javax.annotation.Nonnull;
import java.io.File;


/**
 * Reads data from Gerrit versions 2.9 and higher.
 */
public class SshDefaultCommitDownloader extends GerritCommitDownloader {
    private int startOffset;
    private boolean hasMoreChanges = true;
    private int rowCount = 0;


    public SshDefaultCommitDownloader(@Nonnull GerritServer gerritServer, GerritVersion gerritVersion) {
        super(gerritServer, gerritVersion);
    }


    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    @Nonnull
    @Override
    public boolean hasMoreData() {
        return hasMoreChanges
                && (rowCount < getOverallCommitLimit() || getOverallCommitLimit() == NO_COMMIT_LIMIT);
    }

    public GerritCommitData readData() {
        String gerritQuery = getGerritQuery();
        String output = "";
        String file = "/home/ezivkoc/repo/gerritstats/data/ci-config/" + startOffset + ".txt";

        SshCommand sshCommand = new SshCommand(getGerritServer());

        output = sshCommand.exec(String.format("query %s "
                        + "--format=JSON "
                        + "--all-approvals "
                        + "--comments "
                        + "--all-reviewers "
                        + createStartOffsetArg(),
                gerritQuery
        ));

        FileWriter.writeFile(file, output);

        GerritCommitData commitData = new GerritCommitData(Strings.nullToEmpty(output), getGerritVersion());
        setStartOffset(startOffset + commitData.getMetaData().getRowCount());
        hasMoreChanges = commitData.getMetaData().hasMoreChanges();
        rowCount += commitData.getMetaData().getRowCount();
        return commitData;
    }

    public String getGerritQuery() {
        String query = String.format("project:{^%s}", getProjectName());

        if (getAfterDate() != null) {
            query += String.format(" after:{%s}", getAfterDate());
        }
        if (getBeforeDate() != null) {
            query += String.format(" before:{%s}", getBeforeDate());
        }

        return query;
    }

    private String createStartOffsetArg() {
        return startOffset != 0 ? "--start " + startOffset + " " : "";
    }
}