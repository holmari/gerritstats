datasetOverviewScope.initialize(datasetOverview);

var userIdentifier = getUrlParameter('user');
var usersInAnalysis = new SelectedUsers(overviewUserdata, datasetOverview.hashCode);
var reviewerApprovalGraph = null;
var teamGraph = null;
var iterationTimelineGraph = null;
var iterationGraph = null;

function addReviewerDataToTable(tableJQuery, reviewerData) {
    var tableBody = $(tableJQuery).find('tbody');

    reviewerData.forEach(function(item) {
        tableBody.append('<tr data-identifier="' + item.identity.identifier + '">\
            <td><a href="profile.html?user=' + item.identity.identifier + '">' + getShortPrintableName(item.identity) + '</a></td>\
            <td class="dataField">' + item.approvalData.addedAsReviewerCount + '</td>\
            <td class="dataField">' + item.approvalData.approvalCount + '</td>\
        </tr>');
    });
    tableBody.find('tr').mouseover(function(event) {
        var identifier = event.currentTarget.attributes['data-identifier'].value;
        reviewerApprovalGraph.setSelectedItemByIdentifier(identifier);
    });
    tableBody.find('tr').mouseout(function() {
        reviewerApprovalGraph.setSelectedItemByIdentifier(null);
    });
}

function addTableCaptionRow(tableJQuery, separatorText) {
    var tableBody = $(tableJQuery).find('tbody');
    tableBody.append('<tr><th colspan="2" class="captionRow">' + (separatorText ? separatorText : '') + '</th></tr>');
}


function addReviewRequestorData(identityRecord, reviewerData) {
    var table = $('#reviewRequestorTable');
    var tableBody = table.find('tbody');

    var reviewsWrittenForUser = {};
    identityRecord.commentsWritten.forEach(function(commentSet) {
        var commitOwner = commentSet.commit.owner.identifier;
        if (!usersInAnalysis.isUserSelected(commitOwner)) {
            return;
        }

        var userRecord = reviewsWrittenForUser[commitOwner];
        if (!userRecord) {
            userRecord = 0;
        }

        userRecord += commentSet.commentsByUser.length;
        reviewsWrittenForUser[commitOwner] = userRecord;
    });

    var getReviewsWrittenToUser = function(identifier) {
        var reviewCount = reviewsWrittenForUser[identifier];
        return reviewCount ? reviewCount : 0;
    }

    reviewerData.forEach(function(item) {
        var html = '<tr data-identifier="' + item.identity.identifier + '">\
                        <td><a href="profile.html?user=' + item.identity.identifier + '">'
                        + getShortPrintableName(item.identity) + '</a></td>'
                     + '<td class="dataField">' + item.approvalData.addedAsReviewerCount + '</td>'
                     + '<td class="dataField">' + getReviewsWrittenToUser(item.identity.identifier) + '</td>'
                 + '</tr>'
        tableBody.append(html);
    });
    tableBody.find('tr').mouseover(function(event) {
        var identifier = event.currentTarget.attributes['data-identifier'].value;
        teamGraph.setSelectedIdentifier(identifier);
    });
    tableBody.find('tr').mouseout(function() {
        teamGraph.setSelectedIdentifier(null);
    });

    table.tablesorter();
}

function addNonRespondingUserData(identityRecord, reviewerData) {
    var table = $('#nonRespondingUsersTable');
    var tableBody = $(table).find('tbody');

    reviewerData.forEach(function(item) {
        var html = '<tr data-identifier="' + item.identity.identifier + '">\
                        <td><a href="profile.html?user=' + item.identity.identifier + '">'
                         + getShortPrintableName(item.identity) + '</a></td>';
        html += '</tr>';
        tableBody.append(html);
    });
    tableBody.find('tr').mouseover(function(event) {
        var identifier = event.currentTarget.attributes['data-identifier'].value;
        teamGraph.setSelectedIdentifier(identifier);
    });
    tableBody.find('tr').mouseout(function() {
        teamGraph.setSelectedIdentifier(null);
    });

    table.tablesorter();
}

function highlightTableRow(tableQuery, currentSelection, previousSelection) {
    var body = $(tableQuery).find('tbody');
    if (currentSelection) {
        var identifier = currentSelection;
        body.find("tr[data-identifier='" + identifier + "']").addClass('highlightedRow');
    }
    if (previousSelection) {
        var prevIdentifier = previousSelection;
        body.find("tr[data-identifier='" + prevIdentifier + "']").removeClass('highlightedRow');
    }
}

/**
 * Adds all review comments by user to the table at the bottom of the view.
 */
function addReviewCommentsByUser(record, elementQuery) {
    var commits = record.getCommitsWithWrittenCommentsSortedByDate();

    for (var i = 0; i < commits.length; ++i) {
        var commitAndComments = commits[i];
        var commit = commitAndComments.commit;
        var headerCreated = false;

        commit.patchSets.forEach(function(patchSet) {
            // ignore self-reviews and replies
            if (patchSet.author.email == record.identity.email
            || !usersInAnalysis.isUserSelected(patchSet.author.identifier)) {
                return;
            }

            var comments = patchSet.comments;
            comments.forEach(function(comment) {
                if (comment.reviewer.email != record.identity.email) {
                    return;
                }

                if (!headerCreated) {
                    headerCreated = true;
                    var commitCreatedDate = moment(commit.createdOnDate);
                    $(elementQuery).append('<h4><a href="' + commit.url + '">' + commit.url + '</a> (Created: '
                        +  commitCreatedDate.format("YYYY-MM-DD") + ')</h4><ul></ul>');
                }

                var urlForComment = getGerritUrlForComment(commit, patchSet, comment)
                var altText = comment.file + ':' + comment.line;
                $(elementQuery + " ul").last().append('<li><a href="' + urlForComment + '" alt="' + altText + '">'
                    + escapeHtml(comment.message) + '</a></li>');
            });
        });
    }
}

function addHighPatchSetCommits(exceedingCommits) {
    var tableBody = $('#iterationTable').find('tbody');

    exceedingCommits.forEach(function(commit) {
        tableBody.append('<tr data-identifier="' + commit.url + '">\
            <td><a href="' + commit.url + '">' + commit.subject + '</a></td>\
            <td class="dataField">' + userdataScope.getPatchSetCountForKind(commit, 'REWORK') + '</td>\
        </tr>');
    });
    tableBody.find('tr').mouseover(function(event) {
        var commitUrl = event.currentTarget.attributes['data-identifier'].value;
        iterationGraph.setSelectedCommitUrl(commitUrl);
    });
    tableBody.find('tr').mouseout(function() {
        iterationGraph.setSelectedCommitUrl(null);
    });
}

function addRepositories(identityRecord) {
    var table = $('#repositoriesTable');
    var tableBody = table.find('tbody');

    var repositories = identityRecord.repositories;
    var reviewCountsPerRepo = identityRecord.getUserReviewCountsPerRepository(usersInAnalysis);

    var getCommentCount = function(repository) {
        var statsObject = reviewCountsPerRepo[repository.name];
        return statsObject ? statsObject.commentsWrittenByUser : 0;
    }

    repositories.forEach(function(repository) {
        tableBody.append('<tr data-identifier="' + repository.name + '">\
            <td><a href="' + repository.url + '">' + repository.name + '</a></td>\
            <td class="dataField">' + repository.commitCountForUser + '</td>\
            <td class="dataField">' + getCommentCount(repository) + '</td>\
        </tr>');
    });

    table.tablesorter();
}

function hasIdentifier(reviewerData, identifierToSearch) {
    for (var i = 0; i < reviewerData.length; ++i) {
        if (reviewerData[i].identity.identifier == identifierToSearch) {
            return true;
        }
    }
    return false;
}

function updateViewFromUserdata() {
    var record = userdata[userIdentifier];
    userdataScope.initializeRecord(record);
    var teamIdentities = record.getTeamIdentities(usersInAnalysis);

    $('#datasetOverviewBranchList').html(datasetOverview.branchList);
    $('#datasetOverviewFromDate').html(moment(datasetOverview.fromDate).format("YYYY-MM-DD"));
    $('#datasetOverviewToDate').html(moment(datasetOverview.toDate).format("YYYY-MM-DD"));
    $('#datasetOverviewIdentityCount').html(usersInAnalysis.getPrintableUserCount());
    $('#generatedTimestamp').html(moment(datasetOverview.generatedDate).format('YYYY-MM-DD'))

    $('#pageTitleHead').html("GerritStats: " + record.printableName());
    $('#pageTitle').html(record.shortPrintableName());
    $('#pageSubtitle').html(record.printableEmailAndIdentity());

    $('#profileUsername').html(record.printableUsername());
    $('#profileEmail').html(record.printableEmail());

    $('#commitsSize').html(record.commits.length);
    $('#commentsWritten').html(record.getAllCommentsByUser());
    $('#commentsReceived').html(record.commentsReceived.length);
    $('#receivedCommentRatio').html(record.getReceivedCommentRatio().toFixed(outputConfig.decimalPrecision));
    $('#addedAsReviewerTo').html(record.addedAsReviewerTo.length);
    $('#reviewCommentRatio').html(record.addedAsReviewerTo.length > 0
        ? (record.commentsWritten.length / record.addedAsReviewerTo.length).toFixed(outputConfig.decimalPrecision)
        : "&dash;");
    $('#maxPatchSetCount').html(record.getMaxPatchSetCount());
    $('#reviewCountGivenPlus2').html(record.reviewCountPlus2);
    $('#reviewCountGivenPlus1').html(record.reviewCountPlus1);
    $('#reviewCountGivenMinus1').html(record.reviewCountMinus1);
    $('#reviewCountGivenMinus2').html(record.reviewCountMinus2);
    $('#selfReviews').html(record.selfReviewedCommitCount);
    $('#reviewCountReceivedPlus2').html(record.getReceivedReviewsForScore(2));
    $('#reviewCountReceivedPlus1').html(record.getReceivedReviewsForScore(1));
    $('#reviewCountReceivedMinus1').html(record.getReceivedReviewsForScore(-1));
    $('#reviewCountReceivedMinus2').html(record.getReceivedReviewsForScore(-2));
    $('#averageTimeInCodeReview').html(formatPrintableDuration(record.averageTimeInCodeReview));
    $('#commitsAbandoned').html(record.abandonedCommitCount);
    $('#commitsInReview').html(record.inReviewCommitCount);

    var firstActiveDate = moment(record.firstActiveDate);
    var activeDayCount = firstActiveDate.isValid() && record.lastActiveDate
                       ? Math.round((record.lastActiveDate - record.firstActiveDate) / (1000 * 60 * 60 * 24))
                       : "&dash;";
    $('#firstActiveDate').html(firstActiveDate.isValid() ? moment(firstActiveDate).format('YYYY-MM-DD') : "&dash;");
    $('#lastActiveDate').html(record.lastActiveDate ? moment(record.lastActiveDate).format('YYYY-MM-DD') : "&dash;");
    $('#totalDayCount').html(activeDayCount);
    $('#activeDayCount').html(record.activeDayCount);

    var reviewerData = record.getFilteredReviewerDataForOwnCommits(usersInAnalysis);
    reviewerApprovalGraph = new ReviewersAndApprovalsGraph('#reviewersAndApprovalsSvg', reviewerData);

    addReviewerDataToTable('#addedAsReviewersTable', reviewerData);
    $('#addsThemAsReviewersCount').html(record.reviewersForOwnCommits.length);
    $('#addedAsReviewersTable').tablesorter();

    var reviewRequestorData = record.getReviewRequestors();
    var nonRespondingUserData = [];

    // append all users in 'team' who did not request reviews
    for (var i = 0; i < reviewerData.length; ++i) {
        var reviewerItem = reviewerData[i];
        var identifier = reviewerItem.identity.identifier;
        if (!hasIdentifier(reviewRequestorData, identifier)) {
            nonRespondingUserData.push({
                'approvalData': {
                    'addedAsReviewerCount': -1,
                    'approvalCount': -1
                },
                'identity': reviewerItem.identity
            });
        }
    }

    addReviewRequestorData(record, reviewRequestorData);
    addNonRespondingUserData(record, nonRespondingUserData);
    $('#theyAddThisPersonAsReviewerCount').html(record.reviewRequestors.length);
    $('#reviewRequestCount').html(record.reviewRequestors.reduce(function(prevValue, currentValue) {
        return prevValue += currentValue.approvalData.addedAsReviewerCount;
    }, 0));

    addReviewCommentsByUser(record, '#reviewComments');

    $('.highPatchSetCountThreshold').html(outputConfig.highPatchSetCountThreshold.toString());

    addHighPatchSetCommits(record.getCommitsWithHighPatchSetCount());
    addRepositories(record);

    new PerMonthStats('#perMonthStats', record);
    new FrequencyTable("#commentsPerDaySvg", record, groupReviewCommentsByDate(record.getReviewCommentDates()));
    new CumulativeGraph('#cumulativeGraphSvg', record);

    iterationGraph = new IterationTimelineGraph("#highPatchSetCountCommitsSvg", record);
    iterationGraph.highPatchSetCountThreshold = outputConfig.highPatchSetCountThreshold;
    iterationGraph.selectionChangedListener = function(currentSelection, previousSelection) {
        highlightTableRow('#iterationTable', currentSelection, previousSelection);
    };
    iterationGraph.render();

    reviewerApprovalGraph.selectionChangedListener = function(currentSelection, previousSelection) {
        highlightTableRow('#addedAsReviewersTable', currentSelection, previousSelection);
    };
    reviewerApprovalGraph.render();

    var identityGraph = createIdentityGraph(overviewUserdata, teamIdentities);
    teamGraph = new ProximityGraph(identityGraph, usersInAnalysis, '#teamGraphSvg');
    teamGraph.width = 490;
    teamGraph.height = 490;
    teamGraph.relativeLinkValueThreshold = 0.025;
    teamGraph.charge = -200;
    teamGraph.linkDistance = 25;
    teamGraph.drawCrosshair = true;
    teamGraph.highlightSelection = true;
    teamGraph.defaultItemOpacity = 0.6;
    teamGraph.centeredIdentifier = userIdentifier;
    teamGraph.selectionChangedListener = function(currentSelection, previousSelection) {
        highlightTableRow('#reviewRequestorTable', currentSelection, previousSelection);
        highlightTableRow('#nonRespondingUsersTable', currentSelection, previousSelection);
    };
    teamGraph.create();
}

/**
  * Processes the data into the following format:
  * [{date: "2015-06-19", "count": 1},
  *  {date: "2015-08-10", "count": 1},
  *  {date: "2015-08-14", "count": 1},
  *  {date: "2015-08-15", "count": 3},
  *   ...
  * ];
*/
function groupReviewCommentsByDate(comments) {
    var frequencies = comments.reduce(function (previousValue, currentValue, index, array) {
        var date = currentValue.date;
        if (typeof previousValue[date] == 'undefined') {
            previousValue[date] = 1;
        } else {
            previousValue[date] += 1;
        }
        return previousValue;
    }, {});

    var data = Object.keys(frequencies).map(function(date, index) {
        return { "date": new Date(date), "count": frequencies[date] };
    });
    data.sort(function(left, right) {
        return (left.date <  right.date) ? -1 : 1;
    });
    return data;
}

class ProfilePage {

    constructor() {
    }

    navTabClickHandler(event) {
        event.preventDefault();
        var navElement = $(this);

        var navType = $(navElement).attr('data-type');
        if (navType === undefined) {
            throw new Error("navType is not defined!");
        }
        navElement.tab('show');

        window.location.href = '#' + navType;
    }

    documentReady() {
        $('nav li').click(this.navBarClickHandler);

        loadUserdataForUserIdentifier(userIdentifier, function onLoad() {
            updateViewFromUserdata();
        });

        $('[data-toggle="tooltip"]').tooltip()

        if (datasetOverview.gerritVersion.isUnknown()) {
            $('#unknownGerritVersionAlert').show();
        } else if (!datasetOverview.gerritVersion.isAtLeast(2, 9)) {
            $('#gerritOldVersionField').text(datasetOverview.gerritVersion.toString());
            $('#oldGerritVersionAlert').show();
        }
    }
}

var profilePage = new ProfilePage(datasetOverview);
$(document).ready(() => profilePage.documentReady());