package com.holmsted.gerrit.processors.reviewers;

class PatchSetCommentData {
    final long patchSetDate;
    String reviewerEmail;
    String authorEmail;

    PatchSetCommentData(long patchSetDate) {
        this.patchSetDate = patchSetDate;
    }
}
