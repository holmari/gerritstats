package com.holmsted.gerrit;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.holmsted.gerrit.GerritStatParser.ParserContext;
import com.holmsted.json.JsonUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Commit {
    private static final long SEC_TO_MSEC = 1000;

    public final String project;
    public final String branch;
    public final String id;
    public final int commitNumber;
    public final String subject;
    public final Identity owner;
    public final String url;
    public final String commitMessage;
    public final long createdOnDate;
    public final long lastUpdatedDate;
    public final boolean isOpen;
    public final String status;

    public final ImmutableList<Identity> reviewers;
    public final ImmutableList<ChangeComment> comments;
    public final ImmutableList<PatchSet> patchSets;

    public enum PatchSetKind {
        REWORK,
        TRIVIAL_REBASE,
        NO_CODE_CHANGE,
        NO_CHANGE
    }

    public static class Identity {
        public final String name;
        public final String email;
        public final String username;

        public Identity(@Nullable String name, @Nullable String email, @Nullable String username) {
            this.name = name;
            this.email = email;
            this.username = username;
        }

        public static Identity fromJson(JSONObject ownerJson) {
            return new Identity(ownerJson.optString("name"),
                    ownerJson.optString("email"),
                    ownerJson.optString("username"));
        }

        @Nonnull
        static List<Identity> fromJsonArray(@Nullable JSONArray identitiesJson) {
            List<Identity> result = new ArrayList<>();
            if (identitiesJson != null) {
                for (int i = 0; i < identitiesJson.length(); ++i) {
                    JSONObject identityJson = identitiesJson.getJSONObject(i);
                    result.add(Identity.fromJson(identityJson));
                }
            }
            return result;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getUsername() {
            return username;
        }

        public String getIdentifier() {
            String identifier = username;
            if (Strings.isNullOrEmpty(identifier)) {
                identifier = Strings.nullToEmpty(email).replace(".", "_");
                int atMarkIndex = identifier.indexOf('@');
                if (atMarkIndex != -1) {
                    identifier = identifier.substring(0, atMarkIndex);
                } else {
                    identifier = "anonymous_coward";
                }
            }
            return identifier;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Identity)) {
                return false;
            }
            Identity otherIdentity = (Identity) other;
            return getIdentifier().equals(otherIdentity.getIdentifier());
        }

        public int compareTo(@Nonnull Identity other) {
            if (email != null && other.email != null) {
                return email.compareTo(other.email);
            } else if (username != null && other.username != null) {
                return username.compareTo(other.username);
            } else {
                return 0;
            }
        }

        @Override
        public int hashCode() {
            return getIdentifier().hashCode();
        }

        @Override
        public String toString() {
            if (email != null && !email.isEmpty()) {
                return email;
            } else if (name != null && !name.isEmpty()) {
                return name;
            } else if (username != null && !username.isEmpty()) {
                return username;
            } else {
                return super.toString();
            }
        }
    }

    public static class ChangeComment {
        public final long timestamp;
        @Nullable
        public final Identity reviewer;
        @Nullable
        public final String message;

        public ChangeComment(long timestamp, @Nullable Identity reviewer, @Nullable String message) {
            this.timestamp = timestamp;
            this.reviewer = reviewer;
            this.message = message;
        }

        static ChangeComment fromJson(JSONObject commentJson) {
            return new ChangeComment(
                    commentJson.optLong("timestamp") * SEC_TO_MSEC,
                    Identity.fromJson(commentJson.optJSONObject("reviewer")),
                    commentJson.optString("message"));
        }

        static List<ChangeComment> fromJsonArray(@Nullable JSONArray comments) {
            List<ChangeComment> result = new ArrayList<>();
            if (comments != null) {
                for (int i = 0; i < comments.length(); ++i) {
                    JSONObject commentJson = comments.getJSONObject(i);
                    result.add(ChangeComment.fromJson(commentJson));
                }
            }
            return result;
        }
    }

    public static class Approval {
        public static final String TYPE_CODE_REVIEW = "Code-Review";
        public static final String TYPE_SUBMITTED = "SUBM";

        public final String type;
        public final String description;
        public final int value;
        public final long grantedOnDate;
        public final Identity grantedBy;

        public Approval(@Nullable String type,
                        @Nullable String description,
                        int value,
                        long grantedOnDate,
                        @Nullable Identity grantedBy) {
            this.type = type;
            this.description = description;
            this.value = value;
            this.grantedOnDate = grantedOnDate;
            this.grantedBy = grantedBy;
        }

        @Nonnull
        public static List<Approval> fromJson(@Nullable JSONArray approvals) {
            List<Approval> result = new ArrayList<>();
            if (approvals != null) {
                for (int i = 0; i < approvals.length(); ++i) {
                    result.add(Approval.fromJson(approvals.getJSONObject(i)));
                }
            }
            return result;
        }

        public static Approval fromJson(JSONObject approvalJson) {
            return new Approval(
                    approvalJson.optString("type"),
                    approvalJson.optString("description"),
                    approvalJson.optInt("value"),
                    approvalJson.optLong("grantedOn") * SEC_TO_MSEC,
                    Identity.fromJson(approvalJson.getJSONObject("by"))
            );
        }
    }

    public static class PatchSetComment {
        @Nullable
        public final String file;
        public final int line;
        @Nullable
        public final Identity reviewer;
        public final String message;

        // Note: there is no timestamp field in the json from Gerrit.
        // To have a better approximation on when a comment was written,
        // this field is set when initializing these objects.
        public final long patchSetTimestamp;

        public PatchSetComment(@Nullable String file,
                               int line,
                               @Nullable Identity reviewer,
                               @Nullable String message,
                               long patchSetTimestamp) {
            this.file = file;
            this.line = line;
            this.reviewer = reviewer;
            this.message = message;
            this.patchSetTimestamp = patchSetTimestamp;
        }

        @Nonnull
        public static PatchSetComment fromJson(JSONObject commentJson, long createdOnDate) {
            return new PatchSetComment(
                    commentJson.optString("file"),
                    commentJson.optInt("line"),
                    Identity.fromJson(commentJson.getJSONObject("reviewer")),
                    commentJson.optString("message"),
                    createdOnDate
            );
        }

        @Nonnull
        public static List<PatchSetComment> fromJson(@Nullable JSONArray comments, long createdOnDate) {
            List<PatchSetComment> result = new ArrayList<>();
            if (comments != null) {
                for (int i = 0; i < comments.length(); ++i) {
                    result.add(PatchSetComment.fromJson(comments.getJSONObject(i), createdOnDate));
                }
            }
            return result;
        }

        @Nullable
        public String getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }

        @Nullable
        public Identity getReviewer() {
            return reviewer;
        }

        @Nullable
        public String getMessage() {
            return message;
        }
    }

    public static class PatchSet {
        public final int number;
        public final String revision;
        public final List<String> parents;
        public final String ref;
        public final Identity uploader;
        public final long createdOnDate;
        public final Identity author;
        public final boolean isDraft;
        public final PatchSetKind kind;
        public final List<Approval> approvals;
        public final List<PatchSetComment> comments;
        public final int sizeInsertions;
        public final int sizeDeletions;

        @SuppressWarnings("PMD")
        public PatchSet(int number,
                        @Nullable String revision,
                        @Nonnull List<String> parents,
                        @Nullable String ref,
                        @Nullable Identity uploader,
                        long createdOnDate,
                        @Nullable Identity author,
                        boolean isDraft,
                        @Nonnull PatchSetKind kind,
                        @Nonnull List<Approval> approvals,
                        @Nonnull List<PatchSetComment> comments,
                        int sizeInsertions,
                        int sizeDeletions) {
            this.number = number;
            this.revision = revision;
            this.parents = parents;
            this.ref = ref;
            this.uploader = uploader;
            this.createdOnDate = createdOnDate;
            this.author = author;
            this.isDraft = isDraft;
            this.kind = kind;
            this.approvals = ImmutableList.copyOf(approvals);
            this.comments = ImmutableList.copyOf(comments);
            this.sizeInsertions = sizeInsertions;
            this.sizeDeletions = sizeDeletions;
        }

        static List<PatchSet> fromJsonArray(@Nullable JSONArray patchSetsJson, @Nonnull ParserContext context) {
            List<PatchSet> result = new ArrayList<>();
            if (patchSetsJson != null) {
                for (int i = 0; i < patchSetsJson.length(); ++i) {
                    JSONObject patchSetJson = patchSetsJson.getJSONObject(i);
                    result.add(PatchSet.fromJson(patchSetJson, context));
                }
            }
            return result;
        }

        public int getNumber() {
            return number;
        }

        public Identity getAuthor() {
            return author;
        }

        public Date getCreatedOnDate() {
            return new Date(createdOnDate);
        }

        public List<PatchSetComment> getComments() {
            return comments;
        }

        public boolean contains(PatchSetComment patchSetComment) {
            return comments.indexOf(patchSetComment) != -1;
        }

        static PatchSet fromJson(@Nonnull JSONObject patchSetJson, @Nonnull ParserContext context) {
            Identity uploader = Identity.fromJson(patchSetJson.optJSONObject("uploader"));

            JSONObject authorJson = patchSetJson.optJSONObject("author");
            Identity author;
            if (authorJson != null) {
                author = Identity.fromJson(authorJson);
            } else {
                author = uploader;
            }

            String kindString = patchSetJson.optString("kind");
            PatchSetKind patchSetKind = PatchSetKind.REWORK;
            try {
                patchSetKind = PatchSetKind.valueOf(kindString);
            } catch (IllegalArgumentException e) {
                if (context.version.isAtLeast(2, 9)) {
                    System.err.println("Unknown patch set kind '" + kindString + "'");
                } else {
                    // the 'kind' field does not exist before Gerrit 2.9 or so.
                    patchSetKind = PatchSetKind.REWORK;
                }
            }

            long createdOnDate = patchSetJson.optLong("createdOn") * SEC_TO_MSEC;

            return new PatchSet(
                    patchSetJson.optInt("number"),
                    patchSetJson.optString("revision"),
                    JsonUtils.readStringArray(patchSetJson.optJSONArray("parents")),
                    patchSetJson.optString("ref"),
                    uploader,
                    createdOnDate,
                    author,
                    patchSetJson.optBoolean("isDraft"),
                    patchSetKind,
                    Approval.fromJson(patchSetJson.optJSONArray("approvals")),
                    PatchSetComment.fromJson(patchSetJson.optJSONArray("comments"), createdOnDate),
                    patchSetJson.optInt("sizeInsertions"),
                    patchSetJson.optInt("sizeDeletions")
            );
        }
    }

    @SuppressWarnings("PMD")
    public Commit(@Nullable String project,
                  @Nullable String branch,
                  @Nullable String id,
                  int commitNumber,
                  @Nullable String subject,
                  @Nullable Identity owner,
                  @Nullable String url,
                  @Nullable String commitMessage,
                  long createdOnDate,
                  long lastUpdatedDate,
                  boolean isOpen,
                  @Nullable String status,
                  @Nonnull List<Identity> reviewers,
                  @Nonnull List<ChangeComment> comments,
                  @Nonnull List<PatchSet> patchSets) {
        this.project = project;
        this.branch = branch;
        this.id = id;
        this.commitNumber = commitNumber;
        this.subject = subject;
        this.owner = owner;
        this.url = url;
        this.commitMessage = commitMessage;
        this.createdOnDate = createdOnDate;
        this.lastUpdatedDate = lastUpdatedDate;
        this.isOpen = isOpen;
        this.status = status;
        this.reviewers = ImmutableList.copyOf(reviewers);
        this.comments = ImmutableList.copyOf(comments);
        this.patchSets = ImmutableList.copyOf(patchSets);
    }

    static Commit fromJson(JSONObject commitJson, @Nonnull ParserContext context) {
        List<Identity> reviewers = Identity.fromJsonArray(commitJson.optJSONArray("allReviewers"));
        List<ChangeComment> changeComments = ChangeComment.fromJsonArray(commitJson.optJSONArray("comments"));
        List<PatchSet> patchSets = PatchSet.fromJsonArray(commitJson.optJSONArray("patchSets"), context);

        return new Commit(
                commitJson.optString("project"),
                commitJson.optString("branch"),
                commitJson.optString("id"),
                commitJson.optInt("number"),
                commitJson.optString("subject"),
                Identity.fromJson(commitJson.optJSONObject("owner")),
                commitJson.optString("url"),
                commitJson.optString("commitMessage"),
                commitJson.optLong("createdOn") * SEC_TO_MSEC,
                commitJson.optLong("lastUpdated") * SEC_TO_MSEC,
                commitJson.optBoolean("open"),
                commitJson.optString("status"),
                reviewers,
                changeComments,
                patchSets
        );
    }

    @Nonnull
    public Commit.PatchSet getPatchSetForComment(@Nonnull PatchSetComment patchSetComment) {
        for (PatchSet patchSet : patchSets) {
            if (patchSet.contains(patchSetComment)) {
                return patchSet;
            }
        }
        throw new IllegalArgumentException("Attempted to query for a comment not in the patch set!");
    }

    public int getPatchSetCountForKind(@Nonnull PatchSetKind kind) {
        int count = 0;
        for (PatchSet patchSet : patchSets) {
            if (patchSet.kind == kind) {
                ++count;
            }
        }
        return count;
    }

    public int getFirstPatchSetIndexWithNonAuthorReview() {
        for (int i = 0; i < patchSets.size(); ++i) {
            PatchSet patchSet = patchSets.get(i);
            for (PatchSetComment comment : patchSet.comments) {
                if (!Objects.equals(owner, comment.reviewer)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @Nonnull
    public ImmutableList<PatchSet> getPatchSets() {
        return patchSets;
    }

    @Nonnull
    public Date getCreatedOnDate() {
        return new Date(createdOnDate);
    }

    static boolean isCommit(@Nonnull JSONObject lineJson) {
        return lineJson.opt("status") != null;
    }
}
