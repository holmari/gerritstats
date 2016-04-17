// User data for each user is stored here
// key is IdentityRecord.filenameStem.
var userdata = {}

function formatPrintableDuration(durationMsec) {
    var durationInSecs = parseInt(durationMsec / 1000);
    var days = parseInt(durationInSecs / (60 * 60 * 24));
    var hours = parseInt(durationInSecs / (60 * 60) % 24);
    var minutes = parseInt(durationInSecs / (60) % 60);

    return days + "d " + hours + "h " + minutes + "min"
}

// TODO all these methods should be somehow bound to the record; like,
// TODO attached to prototype or something?

function printableName(record) {
    var name = record.identity.name
    if (name.length == 0) {
        name = "Anonymous Coward"
    }
    if (record.identity.username.length > 0) {
        name += " (" + record.identity.username +")"
    }
    return name
}

function getReceivedCommentRatio(record) {
     var receivedComments = record.commentsReceived.length
     var commitCount = record.commits.length
     if (commitCount > 0) {
         return receivedComments / commitCount
     } else {
         return 0
     }
}

function getPatchSetCountForKind(commit, kind) {
    var count = 0;
    commit.patchSets.forEach(function(patchSet) {
        if (patchSet.kind == kind) {
            ++count
        }
    })
    return count
}

function getMaxPatchSetCount(record) {
    var commitCount = record.commits.length
    if (commitCount == 0) {
        return 0
    }

    var max = Number.MIN_VALUE;
    record.commits.forEach(function(commit) {
        max = Math.max(getPatchSetCountForKind(commit, "REWORK"), max);
    })
    return max;
}

function getReceivedReviewsForScore(record, score) {
    reviews = record.receivedReviews[score.toString()]
    if (!reviews) {
        reviews = 0
    }
    return reviews
}