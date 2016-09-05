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

    private final IdentityGenerator generator = new IdentityGenerator();
    private final FakeFilenameGenerator filenameGenerator = new FakeFilenameGenerator();

    private Map<Identity, Identity> identityMapping = new HashMap<>();
    private Map<String, String> urlMapping = new HashMap<>();
    private Map<String, String> filenameMapping = new HashMap<>();
    private Map<String, String> projectNameMapping = new HashMap<>();
    private Map<ProjectAndBranch, String> branchNameMapping = new HashMap<>();

    private int nextCommitNumber = 1;
    private int devBranchNameSuffix = 1;

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

    private String generateAcmeCorpUrl(@Nonnull Commit commitToAnonymize) {
        try {
            URL url = new URL(commitToAnonymize.url);
            URL newUrl = new URL(url.getProtocol(), URL_ACME_HOST, URL_ACME_PORT,
                    "/c/acme/" + String.valueOf(commitToAnonymize.commitNumber));
            return newUrl.toString();
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
            if (branchName == "master") {
                return "master";
            } else {
                ProjectAndBranch key = new ProjectAndBranch(projectName, branchName);
                return branchNameMapping.computeIfAbsent(key,
                        projectAndBranch -> "dev-" + String.valueOf(devBranchNameSuffix++));
            }
        } else {
            return null;
        }
    }

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
                    name -> filenameGenerator.generateProjectName());
        }
    }

    private String loremIpsumize(@Nullable String message) {
        return LoremIpsumGenerator.makeLoremIpsum(message);
    }

    private String generateId() {
        return "I" + UUID.randomUUID();
    }

    private Commit anonymizeCommit(@Nonnull Commit commit) {
        commit.commitNumber = nextCommitNumber++;
        commit.id = generateId();
        commit.url = getOrCreateUrlForCommit(commit);
        commit.owner = getOrCreateIdentity(commit.owner);
        commit.project = getOrCreateProject(commit.project);

        commit.subject = FakeCommitTitleGenerator.generate();
        commit.commitMessage = loremIpsumize(commit.commitMessage);

        List<Identity> reviewers = commit.reviewers.stream().map(this::getOrCreateIdentity).collect(Collectors.toList());
        commit.reviewers.clear();
        commit.reviewers.addAll(reviewers);

        commit.branch = getBranchForProject(commit.project, commit.branch);

        for (PatchSet patchSet : commit.patchSets) {
            patchSet.author = getOrCreateIdentity(patchSet.author);
            patchSet.uploader = getOrCreateIdentity(patchSet.uploader);
            for (Approval approval : patchSet.approvals) {
                approval.grantedBy = getOrCreateIdentity(approval.grantedBy);
            }
            for (PatchSetComment comment : patchSet.comments) {
                comment.reviewer = getOrCreateIdentity(comment.reviewer);
                comment.file = getOrCreateFilename(commit, comment.file);
                comment.message = loremIpsumize(comment.message);
            }
        }

        for (ChangeComment comment : commit.comments) {
            comment.reviewer = getOrCreateIdentity(comment.reviewer);
            comment.message = loremIpsumize(comment.message);
        }

        return commit;
    }
}
