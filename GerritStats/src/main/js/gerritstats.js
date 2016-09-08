/**
 * User data for each user is stored here.
 * key is IdentityRecord.filenameStem.
 */
var userdata = {};

/**
 * Output settings. These are user-configurable.
 */
var outputConfig = {
    'highPatchSetCountThreshold': 5,
    'decimalPrecision': 3,
    'percentageFormat': '0.0%'
}

///////////////////////////////////////////////////////////////////////////////
// Utility functions
///////////////////////////////////////////////////////////////////////////////

function escapeHtml(text) {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function getUrlParameter(param) {
    var pageURL = decodeURIComponent(window.location.search.substring(1));
    var urlVariables = pageURL.split('&');

    for (var i = 0; i < urlVariables.length; ++i) {
        var parameterName = urlVariables[i].split('=');
        if (parameterName[0] === param) {
            return parameterName[1] === undefined ? true : parameterName[1];
        }
    }
    return ''
}

function loadJavascriptFile(filename, onLoadCallback) {
    var element = document.createElement("script");
    element.src = filename;
    element.onload = onLoadCallback;
    document.body.appendChild(element);
}

/**
 * Loads the userdata .js file for the given user's unique identifier.
 */
function loadUserdataForUserIdentifier(userIdentifier, onLoadCallback) {
    loadJavascriptFile('data/users/' + userIdentifier + '.js', onLoadCallback);
}

function hashCode(text) {
    if (!text || text.length == 0) {
        return 0;
    }
    var hash = 0;
    for (var i = 0; i < text.length; ++i) {
        char = text.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash; // Convert to 32bit
    }
    return hash;
}

///////////////////////////////////////////////////////////////////////////////
// GerritStats-specific functions
///////////////////////////////////////////////////////////////////////////////

function formatPrintableDuration(durationMsec) {
    var durationInSecs = parseInt(durationMsec / 1000);
    var days = parseInt(durationInSecs / (60 * 60 * 24));
    var hours = parseInt(durationInSecs / (60 * 60) % 24);
    var minutes = parseInt(durationInSecs / (60) % 60);

    var daysPart = days > 0 ? days + 'd ' : '';
    var hoursPart = hours > 0 ? hours + 'h ' : '';
    var minPart = minutes > 0 ? minutes + 'min' : '';
    if (!days && !hours && minutes == 0 && durationInSecs > 0) {
        minPart = '< 1min';
    }

    if (durationInSecs > 0) {
        return daysPart + hoursPart + minPart;
    } else {
        return '&dash;';
    }
}

function getShortPrintableName(identity) {
    var name = identity.name;
    if (name.length == 0) {
        name = "Anonymous Coward";
    }
    return name;
}

function getPrintableName(identity) {
    if (identity.name.length == 0 && identity.username.length > 0) {
        return identity.username;
    } else {
        var name = getShortPrintableName(identity);
        if (identity.username.length > 0) {
            name += " (" + identity.username + ")";
        }
        return name;
    }
}

/**
 * Creates a code comment URL.
 *
 * Commit url: https://gerrit.domain.com/29251
 * Output: https://gerrit.domain.com/#/c/29251/13/src/main/java/HelloWorld.java
 */
function getGerritUrlForComment(commit, patchSet, comment) {
    var url = commit.url;
    var baseUrl = url.substring(0, url.lastIndexOf('/'));
    return baseUrl + "/#/c/" + commit.commitNumber + "/" + patchSet.number + "/" + comment.file;
}

function filterReviewerData(reviewerData, usersInAnalysis) {
    var result = [];
    reviewerData.forEach(function(item) {
        if (usersInAnalysis.isUserSelected(item.identity.identifier)) {
            result.push(item);
        }
    });
    return result;
}

/**
 * The methods here are bound to the scope of each userdata['..'] object,
 * to allow for easy access to the per-person data.
 */
var userdataScope = {

    _fromDate: null,
    _toDate: null,

    initializeRecord: function(record) {
        // attach all userdata-related objects to record, to make it behave like an object
        for (var objectName in userdataScope) {
            record[objectName] = userdataScope[objectName];
        }

        record['datedCommitTable'] = new DatedList(record.commits, function(item) {
            return item.createdOnDate;
        });

        record._fromDate = record.firstActiveDate;
        record._toDate = record.lastActiveDate;

        // reshuffle the comments so that they're all added separately;
        // else, the count per month will show number of commits where comments were added,
        // not the total number of comments.
        var allCommentsByUser = []
        record.commentsWritten.forEach(function(commitCommentPair) {
            for (var i = 0; i < commitCommentPair.commentsByUser.length; ++i) {
                var comment = commitCommentPair.commentsByUser[i];
                comment.commit = commitCommentPair.commit;
                allCommentsByUser.push(comment);
            }
        });

        record['datedCommentTable'] = new DatedList(allCommentsByUser, function(comment) {
            return comment.commit.createdOnDate;
        });
    },

    getFromDate: function() {
        if (this._fromDate) {
            return new Date(moment(this._fromDate).format('YYYY-MM-DD'));
        } else {
            return null;
        }
    },

    getToDate: function() {
        if (this._toDate) {
            return new Date(moment(this._toDate).format('YYYY-MM-DD'));
        } else {
            return null;
        }
    },

    printableName: function() {
        return getPrintableName(this.identity);
    },

    shortPrintableName: function() {
        return getShortPrintableName(this.identity);
    },

    printableEmailAndIdentity: function() {
        var email = this.identity.email;
        var mailAndIdentity = email ? email : '';
        if (mailAndIdentity.length && this.hasUsername()) {
            mailAndIdentity += ' (' + this.identity.username + ')'
        } else if (!mailAndIdentity.length) {
            mailAndIdentity = this.identity.username;
        }
        return mailAndIdentity;
    },

    hasUsername: function() {
        return this.identity.username && this.identity.username.length;
    },

    printableUsername: function() {
        if (this.hasUsername()) {
            return this.identity.username;
        } else {
            return "&dash;";
        }
    },

    printableEmail: function() {
        if (this.identity.email && this.identity.email.length) {
            return this.identity.email;
        } else {
            return "&dash;"
        }
    },

    getReceivedCommentRatio: function() {
         var receivedComments = this.commentsReceived.length;
         var commitCount = this.commits.length;
         if (commitCount > 0) {
             return receivedComments / commitCount;
         } else {
             return 0;
         }
    },

    getMaxPatchSetCount: function() {
        var commitCount = this.commits.length;
        if (commitCount == 0) {
            return 0;
        }

        var max = Number.MIN_VALUE;
        this.commits.forEach(function(commit) {
            max = Math.max(userdataScope.getPatchSetCountForKind(commit, "REWORK"), max);
        })
        return max;
    },

    getReceivedReviewsForScore: function(score) {
        var reviews = this.receivedReviews[score.toString()];
        if (!reviews) {
            reviews = 0;
        }
        return reviews;
    },

    getAllCommentsByUser: function() {
        var count = 0;
        this.commentsWritten.forEach(function(commitCommentPair) {
            count += commitCommentPair.commentsByUser.length;
        });
        return count;
    },

    getReviewerDataForOwnCommits: function() {
        return userdataScope.sortReviewDataByAddedAsReviewerCount(this.reviewersForOwnCommits);
    },

    getReviewerApprovalDataForOwnCommits: function() {
        var reviewerData = this.reviewersForOwnCommits;
        reviewerData.sort(function(l, r) {
            var lValue = l.approvalData.approvalCount
            var rValue = r.approvalData.approvalCount
            return (lValue > rValue) ? -1 : ((lValue < rValue) ? 1 : 0)
        });
        return reviewerData;
    },

    getFilteredReviewerDataForOwnCommits: function(usersInAnalysis) {
        var reviewerData = this.getReviewerDataForOwnCommits();
        return filterReviewerData(reviewerData, usersInAnalysis);
    },

    /**
     * Returns a Set of all users that the user had interaction with.
     * The result will be filtered so that only users in analysis are included.
     */
    getTeamIdentities: function(usersInAnalysis) {
        var identities = new Set();
        var reviewerData = this.getReviewerDataForOwnCommits();
        reviewerData.forEach(function(item) {
            identities.add(item.identity.identifier);
        });
        reviewerData = this.getReviewRequestors();
        reviewerData.forEach(function(item) {
            identities.add(item.identity.identifier);
        });

        var filteredResult = new Set();
        identities.forEach(function(identifier) {
            if (usersInAnalysis.isUserSelected(identifier)) {
                filteredResult.add(identifier);
            }
        });
        // add the user themselves to the team, too
        filteredResult.add(this.identity.identifier);

        return filteredResult;
    },

    getReviewRequestors: function() {
        return userdataScope.sortReviewDataByAddedAsReviewerCount(this.reviewRequestors);
    },

    getCommitsWithWrittenCommentsSortedByDate: function() {
        var orderedCommitsAndComments = this.commentsWritten;

        orderedCommitsAndComments.sort(function(l, r) {
            var lValue = l.commit.createdOnDate;
            var rValue = r.commit.createdOnDate;
            return (lValue > rValue) ? 1 : ((lValue < rValue) ? -1 : 0)
        });
        return orderedCommitsAndComments;
    },

    getReviewCommentDates: function() {
        var commentDates = [];
        var record = this;
        this.commentsWritten.forEach(function(commitAndComments) {
            var commit = commitAndComments.commit;
            commit.patchSets.forEach(function(patchSet) {
                // skip self-reviews
                if (patchSet.author.email == record.identity.email) {
                    return;
                }
                patchSet.comments.forEach(function(comment) {
                    if (comment.reviewer.email == record.identity.email) {
                        commentDates.push({
                            'date': moment(patchSet.createdOnDate).format('YYYY-MM-DD')
                        });
                    }
                });
            });
        });
        return commentDates;
    },

    getCommitsWithHighPatchSetCount: function() {
        var highPatchSetCountThreshold = outputConfig.highPatchSetCountThreshold;
        var exceedingCommits = [];
        this.commits.forEach(function(commit) {
            var patchSetCount = userdataScope.getPatchSetCountForKind(commit, 'REWORK');
            if (patchSetCount <= highPatchSetCountThreshold) {
                return;
            }
            var firstNonAuthorCommentPatchSetIndex = userdataScope.getFirstPatchSetIndexWithNonAuthorReview(commit);
            if (firstNonAuthorCommentPatchSetIndex != -1
                    && commit.patchSets.length - firstNonAuthorCommentPatchSetIndex > highPatchSetCountThreshold) {
                exceedingCommits.push(commit);
            }
        });
        exceedingCommits.sort(function(l, r) {
            var lValue = userdataScope.getPatchSetCountForKind(l, 'REWORK');
            var rValue = userdataScope.getPatchSetCountForKind(r, 'REWORK');
            return (lValue > rValue) ? -1 : ((lValue < rValue) ? 1 : 0)
        });
        return exceedingCommits;
    },

    getDatedCommitsWithHighPatchSetCount: function() {
        var exceedingCommits = this.getCommitsWithHighPatchSetCount();
        var datedCommits = [];
        exceedingCommits.forEach(function(commit) {
            datedCommits.push({
                'date': commit.createdOnDate,
                'count': userdataScope.getPatchSetCountForKind(commit, 'REWORK'),
                'commit': commit
            });
        });
        return datedCommits;
    },

    getAddedAsReviewedToWithFilter: function(usersInAnalysis) {
        var filteredCommits = [];
        this.addedAsReviewerTo.forEach(function(commit) {
            if (usersInAnalysis.isUserSelected(commit.owner.identifier)) {
                filteredCommits.push(commit);
            }
        });
        return filteredCommits;
    },

    getUserReviewCountsPerRepository: function(usersInAnalysis) {
        var userIdentifier = this.identity.identifier;
        var filteredCommits = this.getAddedAsReviewedToWithFilter(usersInAnalysis);

        var results = {};
        filteredCommits.forEach(function(commit) {
            commit.patchSets.forEach(function(patchSet) {
                patchSet.comments.forEach(function(comment) {
                    if (comment.reviewer.identifier != userIdentifier) {
                        return;
                    }

                    var result = results[commit.project];
                    if (!result) {
                        result = {
                            'commentsWrittenByUser': 0
                        };
                    }

                    result['commentsWrittenByUser'] += 1;
                    results[commit.project] = result;
                });
            });
        });
        return results;
    },

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    sortReviewDataByAddedAsReviewerCount: function(reviewTableData) {
        var reviewerData = reviewTableData;
        reviewerData.sort(function(l, r) {
            var lValue = l.approvalData.addedAsReviewerCount
            var rValue = r.approvalData.addedAsReviewerCount
            return (lValue > rValue) ? -1 : ((lValue < rValue)
                ? 1 : userdataScope.identityCompare(l.identity, r.identity))
        });
        return reviewerData;
    },

    /**
     * Compares two identities and returns 0, 1 or -1.
     */
    identityCompare: function(l, r) {
        var lValue = l.email
        var rValue = r.email
        return lValue.localeCompare(rValue)
    },

    getPatchSetCountForKind: function(commit, kind) {
        var count = 0;
        commit.patchSets.forEach(function(patchSet) {
            if (patchSet.kind == kind) {
                ++count;
            }
        });
        return count;
    },

    getFirstPatchSetIndexWithNonAuthorReview: function(commit) {
        for (var i = 0; i < commit.patchSets.length; ++i) {
            var patchSet = commit.patchSets[i];
            for (var j = 0; j < patchSet.comments.length; ++j) {
                var comment = patchSet.comments[j];
                if (commit.owner.email != comment.reviewer.email) {
                    return i;
                }
            }
        }
        return -1;
    },

}; // userdataScope

var datasetOverviewScope = {

    initialize: function(overviewRecord) {
        overviewRecord.gerritVersion.isAtLeast = function(major, minor) {
            return overviewRecord.gerritVersion.major >= major
                && overviewRecord.gerritVersion.minor >= minor;
        };

        overviewRecord.gerritVersion.toString = function() {
            return overviewRecord.gerritVersion.major + "."
                 + overviewRecord.gerritVersion.minor + "."
                 + overviewRecord.gerritVersion.patch;
        };

        overviewRecord.gerritVersion.isUnknown = function() {
            return overviewRecord.gerritVersion.major == -1
                && overviewRecord.gerritVersion.minor == -1
                && overviewRecord.gerritVersion.patch == -1;
        };
    }
}; // datasetOverviewScope

var MonthlyTimeFormat = {
    formatFloat: function(value) {
        if (Number.isFinite(value)) {
            return numeral(value).format(outputConfig.percentageFormat);
        } else if (Number.isNaN(value)) {
            return "N/A";
        } else {
            return "&infin;%";
        }
    },

    getSafeRateOfChange: function(prevValue, currentValue) {
        if (prevValue == currentValue) {
            return 0;
        } else if (prevValue != 0) {
            var delta = (currentValue / prevValue);
            return delta < 1 ? -(1 - delta) : delta - 1;
        } else {
            return currentValue > 0 ? Number.POSITIVE_INFINITY : Number.NEGATIVE_INFINITY;
        }
    },

    formatRateOfChange: function(prevValue, nextValue) {
        return MonthlyTimeFormat.formatFloat(MonthlyTimeFormat.getSafeRateOfChange(prevValue, nextValue));
    },

    monthToQuarter: function(month) {
        return Math.floor((month - 1) / 3);
    }
}

function YearlyItemList(epochFunction) {
    this.itemsPerMonth = {};
    this.items = [];

    this.push = function(item) {
        var unixEpoch = epochFunction(item);
        var month = moment(unixEpoch).month() + 1;
        if (month < 1 || month > 12) {
            throw new Error("Month must be in [1..12] range");
        }
        this.itemsPerMonth[month].push(item);
        this.items.push(item);
    };

    this.getMonthlyItemCount = function(month) {
        return this.itemsPerMonth[month].length;
    };

    this.getMonthOnMonthChange = function(month) {
         if (month > 1) {
             var itemsInThisMonth = this.itemsPerMonth[month].size();
             var itemsInPrevMonth = this.itemsPerMonth[month - 1].size();
             return MonthlyTimeFormat.getSafeRateOfChange(itemsInPrevMonth, itemsInThisMonth);
         } else {
             return Number.NaN;
         }
    };

    this.getQuarterOnQuarterChange = function(month) {
        var quarter = MonthlyTimeFormat.monthToQuarter(month);
        var quarterStartMonth = 1 + (quarter * 3);
        if (quarterStartMonth > 1) {
            var quarterItemCount = this.getQuarterlyItemCount(quarter);
            var prevQuarterItemCount = 0;
            var prevQuarterStartMonth = quarterStartMonth - 3;

            for (var i = prevQuarterStartMonth; i <= prevQuarterStartMonth + 2; ++i) {
                prevQuarterItemCount += this.itemsPerMonth[i].length;
            }
            return MonthlyTimeFormat.getSafeRateOfChange(prevQuarterItemCount, quarterItemCount);
        } else {
            return Number.NaN;
        }
    };

    this.getDisplayableQuarterOnQuarterChange = function(month) {
        return MonthlyTimeFormat.formatFloat(this.getQuarterOnQuarterChange(month));
    };

    this.getDisplayableMonthOnMonthChange = function(month) {
        return MonthlyTimeFormat.formatFloat(this.getMonthOnMonthChange(month));
    };

    this.getMonthOnMonthChange = function(month) {
        if (month > 1) {
            var itemsInThisMonth = this.itemsPerMonth[month].length;
            var itemsInPrevMonth = this.itemsPerMonth[month - 1].length;
            return MonthlyTimeFormat.getSafeRateOfChange(itemsInPrevMonth, itemsInThisMonth);
        } else {
            return Number.NaN;
        }
    };

    this.getQuarterlyItemCount = function(quarter) {
        var quarterStartMonth = 1 + (quarter * 3);
        var quarterItemCount = 0;
        for (var i = quarterStartMonth; i <= quarterStartMonth + 2; ++i) {
            quarterItemCount += this.itemsPerMonth[i].length;
        }
        return quarterItemCount;
    };

    // initialize
    for (var i = 1; i <= 12; ++i) {
        this.itemsPerMonth[i] = [];
    }
}

/**
 * Keeps track of items in a calendar-like order (years & months).
 * epochFunction must return the date of the given item as unix epoch.
 */
function DatedList(items, epochFunction) {
    this.itemsPerYear = {}
    this.minDate = Number.MAX_VALUE;
    this.maxDate = Number.MIN_VALUE;
    this.monthsInYear = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

    this.push = function(item) {
        var unixEpoch = epochFunction(item);
        var year = moment(unixEpoch).year();

        this.minDate = Math.min(this.minDate, unixEpoch);
        this.maxDate = Math.max(this.maxDate, unixEpoch);

        if (!this.itemsPerYear[year]) {
            this.itemsPerYear[year] = new YearlyItemList(epochFunction);
        }
        this.itemsPerYear[year].push(item);
    };

    this.getActiveYears = function() {
        var years = Object.keys(this.itemsPerYear)
        years.sort(function(l, r) {
            return (l > r) ? -1 : ((l < r) ? 1 : 0)
        });
        return years;
    };

    this.isDateWithinRange = function(year, month) {
        if (this.minDate == Number.MAX_VALUE || this.maxDate == Number.MIN_VALUE) {
            return false;
        }
        var startMoment = moment(this.minDate);
        var endMoment = moment(this.maxDate);

        var dateAtStartOfMonth = moment({ 'year': year, 'month': month - 1 }).startOf('month');
        var dateAtEndOfMonth = moment({ 'year': year, 'month': month - 1 }).endOf('month');

        return !this.isInFuture(year, month)
            && startMoment.isBefore(dateAtEndOfMonth)
            && endMoment.isAfter(dateAtStartOfMonth)

    };

    this.isInFuture = function(year, month) {
        var timeToTest = moment({ 'year': year, 'month': month - 1});
        var now = moment();
        return timeToTest.isAfter(now);
    }

    this.getDisplayableMonthOnMonthChange = function(year, month) {
        if (!this.isDateWithinRange(year, month)) {
            return "";
        }

        var items = this.itemsPerYear[year];
        if (!items) {
            return MonthlyTimeFormat.formatFloat(Float.NaN);
        }

        if (month > 1) {
            return items.getDisplayableMonthOnMonthChange(month);
        }

        var prevYearItems = this.itemsPerYear[year - 1];
        if (!prevYearItems) {
            return MonthlyTimeFormat.formatFloat(Number.NaN);
        } else {
            var itemsForLastMonthOfPrevYear = prevYearItems.getMonthlyItemCount(12);
            var itemsForFirstMonth = items.getMonthlyItemCount(1);
            return MonthlyTimeFormat.formatRateOfChange(itemsForLastMonthOfPrevYear, itemsForFirstMonth);
        }
    };

    this.getDisplayableQuarterOnQuarterChange = function(year, month) {
        if (!this.isDateWithinRange(year, month)) {
            return "";
        }
        var items = this.itemsPerYear[year];
        if (!items) {
            return MonthlyTimeFormat.formatFloat(Number.NaN);
        }

        var quarter = MonthlyTimeFormat.monthToQuarter(month);
        if (quarter > 0) {
            return items.getDisplayableQuarterOnQuarterChange(month);
        }

        var prevYearItems = this.itemsPerYear[year - 1];
        if (!prevYearItems) {
            return MonthlyTimeFormat.formatFloat(Number.NaN);
        } else {
            var itemsForLastQuarterOfPrevYear = prevYearItems.getQuarterlyItemCount(3);
            var itemsForFirstQuarter = items.getQuarterlyItemCount(0);
            return MonthlyTimeFormat.formatRateOfChange(itemsForLastQuarterOfPrevYear, itemsForFirstQuarter);
        }
    };

    this.getPrintableMonthlyItemCount = function(year, month) {
        if (!this.isDateWithinRange(year, month)) {
            return "";
        }
        var items = this.itemsPerYear[year];
        if (!items) {
            return '0';
        } else {
            return items.getMonthlyItemCount(month).toString();
        }
    };

    // initialize
    for (var i = 0; i < items.length; ++i) {
        this.push(items[i]);
    }
}