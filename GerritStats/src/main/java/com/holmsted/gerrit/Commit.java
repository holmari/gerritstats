package com.holmsted.gerrit;

import com.google.common.base.Strings;
import com.holmsted.gerrit.GerritStatParser.ParserContext;
import com.holmsted.json.JsonUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

public class Commit {
    private static final long SEC_TO_MSEC = 1000;

    public enum PatchSetKind {
        REWORK,
        TRIVIAL_REBASE,
        NO_CODE_CHANGE,
        NO_CHANGE
    }

    public static class Identity {
        public String name;
        public String email;
        public String username;

        public boolean hasEmail() {
            return !Strings.isNullOrEmpty(email);
        }

        public boolean hasUsername() {
            return !Strings.isNullOrEmpty(username);
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

        public static Identity fromJson(JSONObject ownerJson, @Nonnull ParserContext context) {
            Identity identity = new Identity();
            identity.name = ownerJson.optString("name");
            identity.email = ownerJson.optString("email");
            identity.username = ownerJson.optString("username");
            return identity;
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
        public long timestamp;
        public Identity reviewer;
        public String message;

        static ChangeComment fromJson(JSONObject commentJson, @Nonnull ParserContext context) {
            ChangeComment comment = new ChangeComment();
            comment.timestamp = commentJson.optLong("timestamp") * SEC_TO_MSEC;
            comment.reviewer = Identity.fromJson(commentJson.optJSONObject("reviewer"), context);
            comment.message = commentJson.optString("message");
            return comment;
        }
    }

    public static class Approval {

        public static final String TYPE_CODE_REVIEW = "Code-Review";
        public static final String TYPE_VERIFIED = "Verified";
        public static final String TYPE_SUBM = "SUBM";

        public String type;
        public String description;
        public int value;
        public long grantedOnDate;
        public Identity grantedBy;

        public Date getGrantedOnDate() {
            return new Date(grantedOnDate);
        }

        public static List<Approval> fromJson(JSONArray approvals, @Nonnull ParserContext context) {
            List<Approval> result = new ArrayList<>();
            if (approvals != null) {
                for (int i = 0; i < approvals.length(); ++i) {
                    result.add(Approval.fromJson(approvals.getJSONObject(i), context));
                }
            }
            return result;
        }

        public static Approval fromJson(JSONObject approvalJson, @Nonnull ParserContext context) {
            Approval approval = new Approval();
            approval.type = approvalJson.optString("type");
            approval.description = approvalJson.optString("description");
            approval.value = approvalJson.optInt("value");
            approval.grantedOnDate = approvalJson.optLong("grantedOn") * SEC_TO_MSEC;
            approval.grantedBy = Identity.fromJson(approvalJson.getJSONObject("by"), context);
            return approval;
        }
    }

    public static class PatchSetComment {
        public String file;
        public int line;
        public Identity reviewer;
        public String message;

        // Note: there is no timestamp field in the json from Gerrit.
        // To have a better approximation on when a comment was written,
        // this field is set when initializing these objects.
        public long patchSetTimestamp;

        public String getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }

        public Identity getReviewer() {
            return reviewer;
        }

        public String getMessage() {
            return message;
        }

        public static List<PatchSetComment> fromJson(JSONArray comments, @Nonnull ParserContext context) {
            List<PatchSetComment> result = new ArrayList<>();
            if (comments != null) {
                for (int i = 0; i < comments.length(); ++i) {
                    result.add(PatchSetComment.fromJson(comments.getJSONObject(i), context));
                }
            }
            return result;
        }

        public static PatchSetComment fromJson(JSONObject commentJson, @Nonnull ParserContext context) {
            PatchSetComment comment = new PatchSetComment();
            comment.file = commentJson.optString("file");
            comment.line = commentJson.optInt("line");
            comment.reviewer = Identity.fromJson(commentJson.getJSONObject("reviewer"), context);
            comment.message = commentJson.optString("message");
            return comment;
        }
    }

    public static class PatchSet {
        public int number;
        public String revision;
        public final List<String> parents = new ArrayList<>();
        public String ref;
        public Identity uploader;
        public long createdOnDate;
        public Identity author;
        public boolean isDraft;
        public PatchSetKind kind;
        public final List<Approval> approvals = new ArrayList<>();
        public final List<PatchSetComment> comments = new ArrayList<>();
        public int sizeInsertions;
        public int sizeDeletions;

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

        static PatchSet fromJson(JSONObject patchSetJson, @Nonnull ParserContext context) {
            final PatchSet patchSet = new PatchSet();
            patchSet.number = patchSetJson.optInt("number");
            patchSet.revision = patchSetJson.optString("revision");
            patchSet.parents.addAll(JsonUtils.readStringArray(patchSetJson.optJSONArray("parents")));
            patchSet.ref = patchSetJson.optString("ref");
            patchSet.uploader = Identity.fromJson(patchSetJson.optJSONObject("uploader"), context);
            patchSet.createdOnDate = patchSetJson.optLong("createdOn") * SEC_TO_MSEC;

            JSONObject authorJson = patchSetJson.optJSONObject("author");
            if (authorJson != null) {
                patchSet.author = Identity.fromJson(authorJson, context);
            } else {
                patchSet.author = patchSet.uploader;
            }
            patchSet.isDraft = patchSetJson.optBoolean("isDraft");

            String patchSetKind = patchSetJson.optString("kind");
            try {
                patchSet.kind = PatchSetKind.valueOf(patchSetKind);
            } catch (IllegalArgumentException e) {
                if (context.version.isAtLeast(2, 9)) {
                    System.err.println("Unknown patch set kind '" + patchSetKind + "'");
                } else {
                    // the 'kind' field does not exist before Gerrit 2.9 or so.
                    patchSet.kind = PatchSetKind.REWORK;
                }
            }

            patchSet.approvals.addAll(Approval.fromJson(patchSetJson.optJSONArray("approvals"), context));
            patchSet.comments.addAll(PatchSetComment.fromJson(
                    patchSetJson.optJSONArray("comments"), context));
            for (PatchSetComment comment : patchSet.comments) {
                comment.patchSetTimestamp = patchSet.createdOnDate;
            }
            patchSet.sizeInsertions = patchSetJson.optInt("sizeInsertions");
            patchSet.sizeDeletions = patchSetJson.optInt("sizeDeletions");
            return patchSet;
        }
    }

    public String project;
    public String branch;
    public String id;
    public int commitNumber;
    public String subject;
    public Identity owner;
    public String url;
    public String commitMessage;
    public long createdOnDate;
    public long lastUpdatedDate;
    public boolean isOpen;
    public String status;

    public final List<Identity> reviewers = new ArrayList<>();
    public final List<ChangeComment> comments = new ArrayList<>();
    public final List<PatchSet> patchSets = new ArrayList<>();

    public int getPatchSetCountForKind(String kind) {
        return getPatchSetCountForKind(PatchSetKind.valueOf(kind));
    }

    public Commit.PatchSet getPatchSetForComment(PatchSetComment patchSetComment) {
        for (PatchSet patchSet : patchSets) {
            if (patchSet.contains(patchSetComment)) {
                return patchSet;
            }
        }
        throw new IllegalArgumentException("Attempted to query for a comment not in the patch set!");
    }

    public int getPatchSetCountForKind(PatchSetKind kind) {
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
                if (!owner.equals(comment.reviewer)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public String getUrl() {
        return url;
    }

    public List<PatchSet> getPatchSets() {
        return patchSets;
    }

    public Date getCreatedOnDate() {
        return new Date(createdOnDate);
    }

    static boolean isCommit(JSONObject lineJson) {
        return lineJson.opt("status") != null;
    }

    static Commit fromJson(JSONObject commitJson, @Nonnull ParserContext context) {
        Commit commit = new Commit();
        commit.project = commitJson.optString("project");
        commit.branch = commitJson.optString("branch");

        commit.id = commitJson.optString("id");
        commit.commitNumber = commitJson.optInt("number");
        commit.subject = commitJson.optString("subject");
        commit.owner = Identity.fromJson(commitJson.optJSONObject("owner"), context);
        commit.url = commitJson.optString("url");
        commit.commitMessage = commitJson.optString("commitMessage");
        commit.createdOnDate = commitJson.optLong("createdOn") * SEC_TO_MSEC;
        commit.lastUpdatedDate = commitJson.optLong("lastUpdated") * SEC_TO_MSEC;
        commit.isOpen = commitJson.optBoolean("open");
        commit.status = commitJson.optString("status");

        commit.setReviewersFromJson(commitJson.optJSONArray("allReviewers"), context);
        commit.setCommentsFromJson(commitJson.optJSONArray("comments"), context);
        commit.setPatchSetsFromJson(commitJson.optJSONArray("patchSets"), context);

        return commit;
    }

    private void setReviewersFromJson(JSONArray reviewers, @Nonnull ParserContext context) {
        if (reviewers != null) {
            for (int i = 0; i < reviewers.length(); ++i) {
                JSONObject identityJson = reviewers.getJSONObject(i);
                this.reviewers.add(Identity.fromJson(identityJson, context));
            }
        }
    }

    private void setCommentsFromJson(JSONArray comments, @Nonnull ParserContext context) {
        for (int i = 0; i < comments.length(); ++i) {
            JSONObject commentJson = comments.getJSONObject(i);
            this.comments.add(ChangeComment.fromJson(commentJson, context));
        }
    }

    private void setPatchSetsFromJson(JSONArray patchSets, @Nonnull ParserContext context) {
        for (int i = 0; i < patchSets.length(); ++i) {
            JSONObject patchSetJson = patchSets.getJSONObject(i);
            this.patchSets.add(PatchSet.fromJson(patchSetJson, context));
        }
    }
}
