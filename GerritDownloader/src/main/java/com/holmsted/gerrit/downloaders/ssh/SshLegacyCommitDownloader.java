package com.holmsted.gerrit.downloaders.ssh;

import com.google.common.base.Strings;
import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.GerritVersion;
import com.holmsted.gerrit.downloaders.GerritCommitData;
import com.holmsted.gerrit.downloaders.GerritCommitDownloader;

import javax.annotation.Nonnull;

/**
 * Data reader with Gerrit pre-2.9 support. The following problems are being worked around:
 * <p>
 * 1) if no status query is passed, only open commits are listed. So this reader manually
 *    queries for open, merged and abandoned commits.
 * 2) the resume/limit behavior is not implemented in pre-2.9, so resume_sortKey is used instead.
 */
public class SshLegacyCommitDownloader extends GerritCommitDownloader {
    private static final String[] STATUS_QUERIES = {"status:open", "status:merged", "status:abandoned"};

    private int statusQueryIndex = 0;
    private boolean hasMoreChanges = true;
    private String resumeSortKey;
    private int rowCount;


    public SshLegacyCommitDownloader(@Nonnull GerritServer gerritServer, GerritVersion gerritVersion) {
        super(gerritServer, gerritVersion);
    }


    @Nonnull
    @Override
    public boolean hasMoreData() {
        if (!hasMoreChanges && statusQueryIndex < STATUS_QUERIES.length) {
            statusQueryIndex++;
            hasMoreChanges = true;
        }

        return hasMoreChanges
                && (rowCount < getOverallCommitLimit() || getOverallCommitLimit() == NO_COMMIT_LIMIT);
    }

    @Nonnull
    @Override
    public GerritCommitData readData() {
        return readStatusData(STATUS_QUERIES[statusQueryIndex]);
    }

    private GerritCommitData readStatusData(String statusQuery) {
        String gerritQuery = getGerritQuery();
        SshCommand sshCommand = new SshCommand(getGerritServer());
        String resumeSortkeyArg = !Strings.nullToEmpty(resumeSortKey).isEmpty()
                ?  "resume_sortkey:" + resumeSortKey : "";

        String output = sshCommand.exec(String.format("query %s %s "
                + "--format=JSON "
                + "--all-approvals "
                + "--comments "
                + "%s ",
                gerritQuery,
                statusQuery,
                resumeSortkeyArg
        ));

        GerritCommitData commitData = new GerritCommitData(Strings.nullToEmpty(output), getGerritVersion());
        resumeSortKey = commitData.getMetaData().getResumeSortKey();
        hasMoreChanges = commitData.getMetaData().hasMoreChanges();
        rowCount += commitData.getMetaData().getRowCount();
        return commitData;
    }

    public String getGerritQuery() {
        String query = String.format("project:^%s", getProjectName());

        if (getAfterDate() != null) {
            System.out.println("WARNING: --after-date parameter not supported with Gerrit prior to v2.9.");
        }

        return query;
    }
}