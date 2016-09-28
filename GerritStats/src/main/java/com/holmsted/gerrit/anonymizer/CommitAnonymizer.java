package com.holmsted.gerrit.anonymizer;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.Commit.Approval;
import com.holmsted.gerrit.Commit.ChangeComment;
import com.holmsted.gerrit.Commit.Identity;
import com.holmsted.gerrit.Commit.PatchSet;
import com.holmsted.gerrit.Commit.PatchSetComment;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CommitAnonymizer {
    private static final String ACME_CORP_URL = "https://acme.corp/gerrit";
    private static final String URL_ACME_HOST = "acme.corp";
    private static final int URL_ACME_PORT = 29418;

    private final IdentityGenerator generator = new IdentityGenerator();
    private final FakeFilenameGenerator filenameGenerator = new FakeFilenameGenerator();

    private final Map<Identity, Identity> identityMapping = new HashMap<>();
    private final Map<String, String> urlMapping = new HashMap<>();
    private final Map<String, String> filenameMapping = new HashMap<>();
    private final Map<String, String> projectNameMapping = new HashMap<>();
    private final Map<ProjectAndBranch, String> branchNameMapping = new HashMap<>();

    private int nextCommitNumber = 1;
    private int devBranchNameSuffix = 1;

    private static class ProjectAndBranch {
        @Nonnull
        final String projectName;
        @Nonnull
        final String branchName;

        public ProjectAndBranch(@Nonnull String projectName, @Nonnull String branchName) {
            this.projectName = projectName;
            this.branchName = branchName;
        }
    }

    public List<Commit> process(@Nonnull List<Commit> commits) {
        return commits.stream().map(this::anonymizeCommit).collect(Collectors.toList());
    }

    private Identity getOrCreateIdentity(@Nullable Commit.Identity identityToAnonymize) {
        if (identityToAnonymize == null) {
            return null;
        } else {
            return identityMapping.computeIfAbsent(identityToAnonymize,
                    identity -> generator.generateIdentity());
        }
    }

    @Nullable
    private String generateAcmeCorpUrl(@Nonnull Commit commitToAnonymize) {
        try {
            if (commitToAnonymize.url != null) {
                URL url = new URL(commitToAnonymize.url);
                URL newUrl = new URL(url.getProtocol(), URL_ACME_HOST, URL_ACME_PORT,
                        "/c/acme/" + commitToAnonymize.commitNumber);
                return newUrl.toString();
            } else {
                return null;
            }
        } catch (MalformedURLException e) {
            return ACME_CORP_URL;
        }
    }

    private String getOrCreateFilename(@Nonnull Commit commit, @Nullable String filename) {
        return filenameMapping.computeIfAbsent(filename,
                name -> filenameGenerator.generateFilenameFromProjectName(commit.project));
    }

    @Nullable
    private String getBranchForProject(@Nullable String projectName, @Nullable String branchName) {
        if (projectName != null && branchName != null) {
            if ("master".equals(branchName)) {
                return "master";
            } else {
                ProjectAndBranch key = new ProjectAndBranch(projectName, branchName);
                return branchNameMapping.computeIfAbsent(key,
                        projectAndBranch -> "dev-" + (devBranchNameSuffix++));
            }
        } else {
            return null;
        }
    }

    @Nullable
    private String getOrCreateUrlForCommit(@Nonnull Commit commit) {
        if (commit.url == null) {
            return null;
        } else {
            return urlMapping.computeIfAbsent(commit.url,
                    url -> generateAcmeCorpUrl(commit));
        }
    }

    private String getOrCreateProject(@Nullable String projectName) {
        if (projectName == null) {
            return null;
        } else {
            return projectNameMapping.computeIfAbsent(projectName,
                    name -> filenameGenerator.generateUniqueProjectName());
        }
    }

    private String loremIpsumize(@Nullable String message) {
        return LoremIpsumGenerator.makeLoremIpsum(message);
    }

    private String generateId() {
        return "I" + UUID.randomUUID();
    }

    private Commit anonymizeCommit(@Nonnull Commit commitToAnonymize) {
        List<Identity> reviewers = commitToAnonymize.reviewers.stream().map(
                this::getOrCreateIdentity).collect(Collectors.toList());

        List<ChangeComment> changeComments = commitToAnonymize.comments.stream().map(
                comment -> new ChangeComment(comment.timestamp,
                        getOrCreateIdentity(comment.reviewer),
                        loremIpsumize(comment.message)))
                .collect(Collectors.toList());

        List<PatchSet> patchSets = commitToAnonymize.patchSets.stream().map(
                patchSet -> anonymizePatchSet(commitToAnonymize, patchSet)
        ).collect(Collectors.toList());

        return new Commit(
                getOrCreateProject(commitToAnonymize.project),
                getBranchForProject(commitToAnonymize.project, commitToAnonymize.branch),
                generateId(),
                nextCommitNumber++,
                FakeCommitTitleGenerator.generate(),
                getOrCreateIdentity(commitToAnonymize.owner),
                getOrCreateUrlForCommit(commitToAnonymize),
                loremIpsumize(commitToAnonymize.commitMessage),
                commitToAnonymize.createdOnDate,
                commitToAnonymize.lastUpdatedDate,
                commitToAnonymize.isOpen,
                commitToAnonymize.status,
                reviewers,
                changeComments,
                patchSets
        );
    }

    private PatchSet anonymizePatchSet(Commit commit, PatchSet toAnonymize) {
        List<Approval> approvals = toAnonymize.approvals.stream().map(
                approval ->
                        new Approval(approval.type,
                                approval.description,
                                approval.value,
                                approval.grantedOnDate,
                                getOrCreateIdentity(approval.grantedBy)))
                .collect(Collectors.toList());

        List<PatchSetComment> comments = toAnonymize.comments.stream().map(
                comment ->
                        new PatchSetComment(getOrCreateFilename(commit, comment.file),
                                comment.line,
                                getOrCreateIdentity(comment.reviewer),
                                loremIpsumize(comment.message),
                                comment.patchSetTimestamp
                        )).collect(Collectors.toList());

        return new PatchSet(
                toAnonymize.number,
                toAnonymize.revision,
                toAnonymize.parents,
                toAnonymize.ref,
                getOrCreateIdentity(toAnonymize.uploader),
                toAnonymize.createdOnDate,
                getOrCreateIdentity(toAnonymize.author),
                toAnonymize.isDraft,
                toAnonymize.kind,
                approvals,
                comments,
                toAnonymize.sizeInsertions,
                toAnonymize.sizeDeletions
        );
    }
}
