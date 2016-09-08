/** Called when the document has loaded. */
function populateDatasetOverviewWidget() {
    document.title = "GerritStats for " + datasetOverview.projectName;
    var dataSetName = datasetOverview.projectName;
    if (datasetOverview.filenames.length > 20) {
        var firstFilename = datasetOverview.filenames[0];
        var lastFilename = datasetOverview.filenames[datasetOverview.filenames.length - 1];
        dataSetName = datasetOverview.filenames.length + " files, from "
                    + firstFilename + " to " + lastFilename
    }
    $('#subtitleDataSetName').html(dataSetName);

    $('#datasetOverviewBranchList').html(datasetOverview.branchList);
    $('#datasetOverviewFromDate').html(moment(datasetOverview.fromDate).format("YYYY-MM-DD"));
    $('#datasetOverviewToDate').html(moment(datasetOverview.toDate).format("YYYY-MM-DD"));
    $('#datasetOverviewIdentityCount').html(usersInAnalysis.getPrintableUserCount());
    $('#generatedTimestamp').html(moment(datasetOverview.generatedDate).format('YYYY-MM-DD'))
}

function initTableSorter() {
    $.tablesorter.addParser({
        id: 'timeInReview',
        is: function(s) {
            return false;
        },
        format: function(s, table, cell, cellIndex) {
            return $(cell).attr('data-unixtime');
        },
        type: 'numeric'
    });
    $.tablesorter.addParser({
            id: 'decimal',
            is: function(s) {
                return false;
            },
            format: function(s, table, cell, cellIndex) {
                return $(cell).attr('data-value');
            },
            type: 'numeric'
    });

    $('#identities').tablesorter({
        headers: {
            0: { // checkbox column
                sorter: false
            },
            9: {
                sorter: 'decimal'
            },
            10: {
                sorter: 'decimal'
            },
            13: {
                sorter:'timeInReview'
            }
        }
    });
}

var usersInAnalysis = new SelectedUsers(overviewUserdata, datasetOverview.hashCode);
var indexTableHighlighter = null;

function updateOnUserSelectionChange() {
    usersInAnalysis.writeToStorage();

    $('#datasetOverviewIdentityCount').html(usersInAnalysis.getPrintableUserCount());

    var proximityGraphObject = $('#proximityGraph');
    if (proximityGraphObject.is(':visible')) {
        proximityGraph.render();
    }

    indexTableHighlighter.setNeedsUpdate();
    indexTableHighlighter.highlight();
}

function onIdentityCheckboxClicked(checkbox) {
    var userIdentifier = $(checkbox).attr('data-identifier');

    usersInAnalysis.setUserSelected(userIdentifier, checkbox.checked);
    updateOnUserSelectionChange();

    var row = $(checkbox).parent().parent();
    indexTableHighlighter.applySelectionStyle(row, checkbox.checked);
}

function onSelectAllCheckboxClicked(checkbox) {
    $('.identityCheckbox').each(function() {
        $(this).prop('checked', checkbox.checked);
        var userIdentifier = $(this).attr('data-identifier');
        usersInAnalysis.setUserSelected(userIdentifier, checkbox.checked);
    });

    updateOnUserSelectionChange();
}

class IndexTableHighlighter {

    constructor(overviewUserdata, usersInAnalysis) {
        this.tbody = $('#overviewTableBody');

        this.overviewUserdata = overviewUserdata;

        this.highlighters = {
            'reviewCountPlus2': new TableCellHighlighter(overviewUserdata, usersInAnalysis, 'reviewCountPlus2'),
            'reviewCountPlus1': new TableCellHighlighter(overviewUserdata, usersInAnalysis, 'reviewCountPlus1'),
            'reviewCountMinus1': new TableCellHighlighter(overviewUserdata, usersInAnalysis, 'reviewCountMinus1'),
            'reviewCountMinus2': new TableCellHighlighter(overviewUserdata, usersInAnalysis, 'reviewCountMinus2'),
            'allCommentsWritten': new TableCellHighlighter(overviewUserdata, usersInAnalysis, 'allCommentsWritten'),
            'allCommentsReceived': new TableCellHighlighter(overviewUserdata, usersInAnalysis, 'allCommentsReceived')
                                        .setIsAscending(true)
                                        .setIgnoreFunction(function(element, key) {
                                            return element.commitCount == 0;
                                        }),
            'commitCount': new TableCellHighlighter(overviewUserdata, usersInAnalysis, 'commitCount')
                                        .setIgnoreZeroes(true),
            'receivedCommentRatio': new TableCellHighlighter(overviewUserdata, usersInAnalysis, 'receivedCommentRatio')
                                        .setIsAscending(true)
                                        .setIgnoreFunction(function(element, key) {
                                            return element.commitCount == 0;
                                        }),
            'reviewCommentRatio': new TableCellHighlighter(overviewUserdata, usersInAnalysis, 'reviewCommentRatio'),
            'addedAsReviewerToCount': new TableCellHighlighter(overviewUserdata, usersInAnalysis, 'addedAsReviewerToCount'),
            'selfReviewedCommitCount': new TableCellHighlighter(overviewUserdata, usersInAnalysis, 'selfReviewedCommitCount')
                                        .setIsAscending(true)
                                        .setHighlightPositiveEntries(false)
                                        .setIgnoreZeroes(true),
            'averageTimeInCodeReview': new TableCellHighlighter(overviewUserdata, usersInAnalysis, 'averageTimeInCodeReview')
                                           .setIsAscending(true)
                                           .setIgnoreZeroes(true),
        };
    }

    setNeedsUpdate() {
        for (var fieldName in this.highlighters) {
            this.highlighters[fieldName].setNeedsUpdate();
        }
    }

    applySelectionStyle(row, isSelected) {
        row.css('color', isSelected ? '' : '#cccccc');
    }

    highlight() {
        var that = this;
        for (var fieldName in this.highlighters) {
            var rows = this.tbody.find('tr');
            rows.each(function(index) {
                var userdataOverviewIndex = $(this).attr('data-index');
                var highlighter = that.highlighters[fieldName];
                if (!highlighter) {
                    throw new Error("Highlighter for name '" + fieldName + "' not found!");
                }

                var field = $(this).find('td.' + fieldName);
                if (!field) {
                    throw new Error("Field with name '" + fieldName + "' not found!")
                }

                var backgroundColor = highlighter.getHighlightColor(that.overviewUserdata[userdataOverviewIndex]);
                field.css('background-color', backgroundColor);
            });
        }
    }
}

function loadOverviewUserdata() {
    var tbody = $('#overviewTableBody');
    for (var i = 0; i < overviewUserdata.length; ++i) {
        var item = overviewUserdata[i];
        var isSelected = usersInAnalysis.isUserSelected(item);

        tbody.append(
        '<tr data-index="' + i + '">\
            <td><input class="identityCheckbox" \
                       data-identifier="' + item.identifier + '"\
                       type="checkbox" '
                    + (isSelected ? 'checked' : '') + '\
                       onclick="onIdentityCheckboxClicked(this)" /></td>\
            <td><a href="profile.html?user=' + item.identifier + '">' + getPrintableName(item.identity) + '</a></td>\
            <td class="dataField reviewCountPlus2">' + item.reviewCountPlus2 + '</td>\
            <td class="dataField reviewCountPlus1">' + item.reviewCountPlus1 + '</td>\
            <td class="dataField reviewCountMinus1">' + item.reviewCountMinus1 + '</td>\
            <td class="dataField reviewCountMinus2">' + item.reviewCountMinus2 + '</td>\
            <td class="dataField allCommentsWritten">' + item.allCommentsWritten + '</td>\
            <td class="dataField allCommentsReceived">' + item.allCommentsReceived + '</td>\
            <td class="dataField commitCount">' + item.commitCount + '</td>\
            <td class="dataField receivedCommitRatio" data-value="' + item.receivedCommentRatio + '">' + numeral(item.receivedCommentRatio).format('0.000') + '</td>\
            <td class="dataField reviewCommentRatio" data-value="' + item.reviewCommentRatio + '">' + numeral(item.reviewCommentRatio).format('0.000') + '</td>\
            <td class="dataField addedAsReviewerToCount">' + item.addedAsReviewerToCount + '</td>\
            <td class="dataField selfReviewedCommitCount">' + item.selfReviewedCommitCount + '</td>\
            <td class="dataField averageTimeInCodeReview" data-unixtime="' + item.averageTimeInCodeReview + '">'
                + formatPrintableDuration(item.averageTimeInCodeReview) + '</td>\
        </tr>');
        indexTableHighlighter.applySelectionStyle(tbody.find('tr').last(), isSelected);
    };

    indexTableHighlighter.highlight();
}

class IndexPage {
    constructor(datasetOverview) {
        datasetOverviewScope.initialize(datasetOverview);
    }

    /**
     * Moves the navigation tab bar so that it starts horizontally after the 'Name' column.
     *
     * FIXME this is UI hack for the lack of better design and will be replaced
     *       because this is unusable with large projects with large numbers of contributors.
     */
    updateTabBarPosition() {
        var navBar = $('#navBar');

        var checkboxCol = $('#checkboxColumn');
        var connectionsLeftMargin = this.nameColumn.clientLeft + this.nameColumn.clientWidth + 50;
        navBar.css('margin-left', connectionsLeftMargin);

        var content = $("#content");
        var proximityGraphObject = $('#proximityGraph');

        if (proximityGraphObject.is(':visible')) {
            proximityGraphObject.css('left', connectionsLeftMargin);
            proximityGraphObject.css('top', navBar.position().top + navBar.height());
            proximityGraphObject.css('height', content.outerHeight());
            proximityGraphObject.css('width', content.outerWidth() - this.nameColumn.clientWidth);
            this.proximityGraph.updateSize();
        }
    }

    navTabClickHandler(event) {
        event.preventDefault();
        var navElement = $(this);

        var navType = $(navElement).attr('data-type');
        if (navType === undefined) {
            throw new Error("navType is not defined!");
        }
        navElement.tab('show');

        switch (navType) {
        case 'users':
            $('#proximityGraph').hide();
            break;
        case 'connections':
            $('#proximityGraph').show();
            indexPage.updateTabBarPosition();
            break;
        }
    }

    createProximityGraph() {
        this.identityGraph = createIdentityGraph(overviewUserdata);
        this.proximityGraph = new ProximityGraph(this.identityGraph, usersInAnalysis, '#proximityGraph');
        this.proximityGraph.create();

        if (datasetOverview.gerritVersion.isUnknown()) {
            $('#unknownGerritVersionAlert').show();
        } else if (!datasetOverview.gerritVersion.isAtLeast(2, 9)) {
            $('#gerritOldVersionField').text(datasetOverview.gerritVersion.toString());
            $('#oldGerritVersionAlert').show();
        }
    }

    documentReady() {
        populateDatasetOverviewWidget();

        indexTableHighlighter = new IndexTableHighlighter(overviewUserdata, usersInAnalysis);
        loadOverviewUserdata();
        initTableSorter();

        this.createProximityGraph();

        this.nameColumn = document.getElementById('nameColumn');
        new ResizeSensor(this.nameColumn, this.updateTabBarPosition);

        $('nav li').click(this.navTabClickHandler);
    }
}

var indexPage = new IndexPage(datasetOverview);
$(document).ready(() => indexPage.documentReady());