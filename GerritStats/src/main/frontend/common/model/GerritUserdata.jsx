import moment from 'moment';

import DatedList from './DatedList';

export const DECIMAL_PRECISION = 3;
export const PERCENTAGE_FORMAT = '0.0%';

export const HIGH_PATCH_SET_COUNT_THRESHOLD = 5;

export function getShortPrintableName(identity) {
    var name = identity['name'];
    if (name.length == 0) {
        name = 'Anonymous Coward';
    }
    return name;
}

export function getPrintableName(identity) {
    if (identity['name'].length == 0 && identity['username'].length > 0) {
        return identity['username'];
    } else {
        var name = getShortPrintableName(identity);
        if (identity['username'].length > 0) {
            name += ` (${identity['username']})`;
        }
        return name;
    }
}

/**
 * Counts the patch sets of given kind in the passed commit.
 */
export function getPatchSetCountForKind(commit, kind) {
    var count = 0;
    commit.patchSets.forEach(function(patchSet) {
        if (patchSet.kind == kind) {
            ++count;
        }
    });
    return count;
}

export function getProfilePageLinkForIdentity(identityOrIdentifier) {
    const identifier = (typeof identityOrIdentifier === 'string')
        ? identityOrIdentifier : identityOrIdentifier['identifier'];
    return `/profile/${identifier}`;
}

function filterReviewerData(reviewerData, selectedUsers) {
    var result = [];
    reviewerData.forEach(function(item) {
        if (selectedUsers.isUserSelected(item.identity['identifier'])) {
            result.push(item);
        }
    });
    return result;
}

/**
 * Compares two identities and returns 0, 1 or -1.
 */
function identityCompare(l, r) {
    var lValue = l['email'];
    var rValue = r['email'];
    return lValue.localeCompare(rValue);
}

/**
 * Returns the first patch set index which has different reviewer than author,
 * or -1 if no such patch set is found.
 */
function getFirstPatchSetIndexWithNonAuthorReview(commit) {
    for (var i = 0; i < commit.patchSets.length; ++i) {
        var patchSet = commit.patchSets[i];
        for (var j = 0; j < patchSet.comments.length; ++j) {
            var comment = patchSet.comments[j];
            if (commit.owner['email'] != comment.reviewer['email']) {
                return i;
            }
        }
    }
    return -1;
}

/**
 * Sorts the passed reviewer data by the times each identity was added as a reviewer.
 */
function sortReviewDataByAddedAsReviewerCount(reviewTableData) {
    var reviewerData = reviewTableData;
    reviewerData.sort(function(l, r) {
        var lValue = l.approvalData.addedAsReviewerCount;
        var rValue = r.approvalData.addedAsReviewerCount;
        return (lValue > rValue) ? -1 : ((lValue < rValue) ?
            1 : identityCompare(l.identity, r.identity));
    });
    return reviewerData;
}

/**
 * Returns the number of comments written by the user, given the passed project and
 * the set of selected users.
 */
function getUserCommentCountsInProject(project, selectedUsers) {
    return project.reviewRequestors.reduce(function(prevValue, reviewerData) {
        if (selectedUsers.isUserSelected(reviewerData.identity)) {
            return prevValue + reviewerData.approvalData.commentCount;
        } else {
            return prevValue;
        }
    }, 0);
}

export default class GerritUserdata {

    constructor(userdataRecord) {
        this.record = userdataRecord;
        this.fromDate = this.record.firstActiveDate;
        this.toDate = this.record.lastActiveDate;

        // reshuffle the comments so that they're all added separately;
        // else, the count per month will show number of commits where comments were added,
        // not the total number of comments.
        var allCommentsByUser = [];
        this.record.commentsWritten.forEach(function(commitCommentPair) {
            for (var i = 0; i < commitCommentPair.commentsByUser.length; ++i) {
                var comment = commitCommentPair.commentsByUser[i];
                comment.commit = commitCommentPair.commit;
                allCommentsByUser.push(comment);
            }
        });

        this.datedCommitTable = new DatedList(this.record.commits,
            (item) => item.createdOnDate);
        this.datedCommentTable = new DatedList(allCommentsByUser,
            (comment) => comment.commit.createdOnDate);
    }

    getFromDate() {
        if (this.fromDate) {
            return new Date(moment(this.fromDate).format('YYYY-MM-DD'));
        } else {
            return null;
        }
    }

    getToDate() {
        if (this.toDate) {
            return new Date(moment(this.toDate).format('YYYY-MM-DD'));
        } else {
            return null;
        }
    }

    getFirstActiveDate() {
        return this.record.firstActiveDate;
    }

    getLastActiveDate() {
        return this.record.lastActiveDate;
    }

    getActiveDayCount() {
        return this.record.activeDayCount;
    }

    getPrintableName() {
        return getPrintableName(this.record.identity);
    }

    getShortPrintableName() {
        return getShortPrintableName(this.record.identity);
    }

    getPrintableEmailAndIdentity() {
        const identity = this.record.identity;
        const email = identity['email'];
        var mailAndIdentity = email ? email : '';
        if (mailAndIdentity.length && this.hasUsername()) {
            mailAndIdentity += ' (' + identity['username'] + ')';
        } else if (!mailAndIdentity.length) {
            mailAndIdentity = identity['username'];
        }
        return mailAndIdentity;
    }

    getPrintableUsername() {
        if (this.hasUsername()) {
            return this.record.identity['username'];
        } else {
            return '\u2013';
        }
    }

    getPrintableEmail() {
        const email = this.getEmail();
        if (email && email.length) {
            return email;
        } else {
            return '\u2013';
        }
    }

    getEmail() {
        return this.record.identity['email'];
    }

    getReceivedCommentRatio(selectedUsers) {
        return this.getAllCommentsReceived(selectedUsers) / this.getCommitCount();
    }

    getMaxPatchSetCount() {
        return this.record.commits.reduce(function(prev, commit) {
            return Math.max(prev, getPatchSetCountForKind(commit, 'REWORK'));
        }, 0);
    }

    getReviewsGivenForScore(score, selectedUsers) {
        const scoreKey = score + '';
        return this.record.reviewRequestors.reduce(function(prev, value) {
            const isSelected = selectedUsers.isUserSelected(value.identity);
            return prev + (isSelected ? value.approvalData.approvals[scoreKey] || 0 : 0);
        }.bind(this), 0);
    }

    getReviewsReceivedForScore(score, selectedUsers) {
        const scoreKey = score + '';
        return this.record.reviewersForOwnCommits.reduce(function(prev, value) {
            const isSelected = selectedUsers.isUserSelected(value.identity);
            return prev + (isSelected ? value.approvalData.approvals[scoreKey] || 0 : 0);
        }.bind(this), 0);
    }

    getCommentsWrittenCount(selectedUsers) {
        return this.record.reviewRequestors.reduce(function(prev, value) {
            const isSelected = selectedUsers.isUserSelected(value.identity);
            return prev + (isSelected ? value.approvalData.commentCount || 0 : 0);
        }, 0);
    }

    getReviewerDataForOwnCommits() {
        return sortReviewDataByAddedAsReviewerCount(this.record.reviewersForOwnCommits);
    }

    getReviewerApprovalDataForOwnCommits() {
        var reviewerData = this.record.reviewersForOwnCommits;
        reviewerData.sort(function(l, r) {
            var lValue = l.approvalData.approvalCount;
            var rValue = r.approvalData.approvalCount;
            return (lValue > rValue) ? -1 : ((lValue < rValue) ? 1 : 0);
        });
        return reviewerData;
    }

    getFilteredReviewerDataForOwnCommits(selectedUsers) {
        var reviewerData = this.getReviewerDataForOwnCommits();
        return filterReviewerData(reviewerData, selectedUsers);
    }

    /**
     * Returns a Set of all users that the user had interaction with.
     * The result will be filtered so that only users in analysis are included.
     */
    getTeamIdentities(selectedUsers) {
        var identities = new Set();
        var reviewerData = this.getReviewerDataForOwnCommits();
        reviewerData.forEach(function(item) {
            identities.add(item.identity['identifier']);
        });
        reviewerData = this.getReviewRequestors(selectedUsers);
        reviewerData.forEach(function(item) {
            identities.add(item.identity['identifier']);
        });

        // add the user themselves to the team, too
        identities.add(this.record.identity['identifier']);

        return identities;
    }

    getAllCommentsReceived(selectedUsers) {
        return this.record.reviewersForOwnCommits.reduce(function(prev, value) {
            const isSelected = selectedUsers.isUserSelected(value.identity);
            return prev + (isSelected ? value.approvalData.commentCount : 0);
        }, 0);
    }

    getAddedAsReviewerToCount(selectedUsers) {
        return this.record.reviewRequestors.reduce(function(prev, value) {
            const isSelected = selectedUsers.isUserSelected(value.identity);
            return prev + (isSelected ? value.approvalData.addedAsReviewerCount : 0);
        }, 0);
    }

    getReviewRequestors(selectedUsers) {
        var filteredResult = [];
        this.record.reviewRequestors.forEach(function(item) {
            if (selectedUsers.isUserSelected(item.identity)) {
                filteredResult.push(item);
            }
        });

        return sortReviewDataByAddedAsReviewerCount(filteredResult);
    }

    getCommitCount() {
        return this.record.commits.length;
    }

    getAbandonedCommitCount() {
        return this.record.abandonedCommitCount;
    }

    getInReviewCommitCount() {
        return this.record.inReviewCommitCount;
    }

    getCommits() {
        return this.record.commits.slice();
    }

    getCommitsWithWrittenCommentsSortedByDate() {
        var orderedCommitsAndComments = this.record.commentsWritten.slice();

        orderedCommitsAndComments.sort(function(l, r) {
            var lValue = l.commit.createdOnDate;
            var rValue = r.commit.createdOnDate;
            return (lValue > rValue) ? 1 : ((lValue < rValue) ? -1 : 0);
        });
        return orderedCommitsAndComments;
    }

    getReviewCommentDates() {
        var commentDates = [];
        var record = this.record;
        this.record.commentsWritten.forEach(function(commitAndComments) {
            var commit = commitAndComments.commit;
            commit.patchSets.forEach(function(patchSet) {
                // skip self-reviews
                if (patchSet.author['email'] == record.identity['email']) {
                    return;
                }
                patchSet.comments.forEach(function(comment) {
                    if (comment.reviewer['email'] == record.identity['email']) {
                        commentDates.push({
                            'date': moment(patchSet.createdOnDate).format('YYYY-MM-DD')
                        });
                    }
                });
            });
        });
        return commentDates;
    }

    /*
     * Returns the ratio of comments written per added as reviewer.
     * Returns NaN if the user was never added as a reviewer to commits by the currently active users.
     */
    getReviewCommentRatio(selectedUsers) {
        const addedAsReviewerTo = this.getAddedAsReviewerToCount(selectedUsers);
        const commentsWritten = this.getCommentsWrittenCount(selectedUsers);
        return commentsWritten / addedAsReviewerTo;
    }

    getCommitsWithHighPatchSetCount() {
        var exceedingCommits = [];
        this.record.commits.forEach(function(commit) {
            var patchSetCount = getPatchSetCountForKind(commit, 'REWORK');
            if (patchSetCount <= HIGH_PATCH_SET_COUNT_THRESHOLD) {
                return;
            }
            var firstNonAuthorCommentPatchSetIndex = getFirstPatchSetIndexWithNonAuthorReview(commit);
            if (firstNonAuthorCommentPatchSetIndex != -1 &&
                commit.patchSets.length - firstNonAuthorCommentPatchSetIndex > HIGH_PATCH_SET_COUNT_THRESHOLD) {
                exceedingCommits.push(commit);
            }
        });
        exceedingCommits.sort(function(l, r) {
            var lValue = getPatchSetCountForKind(l, 'REWORK');
            var rValue = getPatchSetCountForKind(r, 'REWORK');
            return (lValue > rValue) ? -1 : ((lValue < rValue) ? 1 : 0);
        });
        return exceedingCommits;
    }

    getDatedCommitsWithHighPatchSetCount() {
        var exceedingCommits = this.getCommitsWithHighPatchSetCount();
        return exceedingCommits.map(function(commit) {
            return {
                date: commit.createdOnDate,
                count: getPatchSetCountForKind(commit, 'REWORK'),
                commit: commit,
            };
        });
    }

    getAddedAsReviewedToWithFilter(selectedUsers) {
        var filteredCommits = [];
        this.record.addedAsReviewerTo.forEach(function(commit) {
            if (selectedUsers.isUserSelected(commit.owner['identifier'])) {
                filteredCommits.push(commit);
            }
        });
        return filteredCommits;
    }

    getPerProjectData(selectedUsers) {
        const projects = this.record.projects;
        return projects.map(function(project) {
            return {
                url: project.url,
                name: project.name,
                commitCount: project.commitCountForUser,
                commentsWritten: getUserCommentCountsInProject(project, selectedUsers),
            };
        });
    }

    getIdentifier() {
        return this.record.identity['identifier'];
    }

    getDatedCommitTable() {
        return this.datedCommitTable;
    }

    getDatedCommentTable() {
        return this.datedCommentTable;
    }

    getSelfReviewedCommitCount() {
        return this.record.selfReviewedCommitCount;
    }

    getAverageTimeInCodeReview() {
        return this.record.averageTimeInCodeReview;
    }

    getCommentsWritten() {
        return this.record.commentsWritten.slice();
    }

    getCommentsReceived() {
        return this.record.commentsReceived.slice();
    }

    hasUsername() {
        return this.record.identity['username'] &&
            this.record.identity['username'].length;
    }
}
