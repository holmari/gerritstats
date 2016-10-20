import numeral from 'numeral';
import React from 'react';
import {Td, Tr} from 'reactable';
import {Link} from 'react-router';
import Reactable from 'reactable';

import SimpleSortableTable from '../common/SimpleSortableTable';
import {getPrintableName, getProfilePageLinkForIdentity} from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';
import {formatPrintableDuration} from '../common/time/TimeUtils';

import TableCellHighlighter from './TableCellHighlighter';

function decimalComparator(left, right) {
    const lNum = +left;
    const rNum = +right;
    if (lNum > rNum) {
        return 1;
    } else if (lNum < rNum) {
        return -1;
    } else {
        return 0;
    }
}

export default class OverviewTable extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            selectedUsers: this.props.selectedUsers,
            columnMetadata: this.getDefaultColumnMetadata()
        };
    }

    getDefaultColumnMetadata() {
        const overviewUserdata = this.props.overviewUserdata;
        // Use props as this is only called in constructor
        const selectedUsers = this.props.selectedUsers;

        return {
            'selected': {
                header: () => (
                    <input name='selectAll' type='checkbox'
                        checked={this.state.selectedUsers.isAllUsersSelected()}
                        onChange={this.onSelectAllCheckboxValueChanged.bind(this)} />
                ),
                cell: (record, index) => (
                    <Td key={'selected' + index} column='selected'>
                        <input data-identifier={record.identifier}
                               type='checkbox'
                               checked={this.state.selectedUsers.isUserSelected(record.identifier)}
                               onChange={() => this.onIdentityCheckboxValueChanged(record.identifier)} />
                    </Td>
                ),
            },
            'name': {
                sortFunction: Reactable.Sort.CaseInsensitive,
                description: 'The name of the user, as shown in Gerrit.',
                header: 'Name',
                cell: (record, index) => (
                    <Td key={'name' + index} column='name' value={getPrintableName(record.identity)}>
                        <Link to={getProfilePageLinkForIdentity(record.identifier)}>{getPrintableName(record.identity)}</Link>
                    </Td>
                ),
            },
            'reviewCountPlus2': {
                sortFunction: Reactable.Sort.NumericInteger,
                highlighter: new TableCellHighlighter(overviewUserdata, selectedUsers, 'reviewCountPlus2'),
                description: 'Number of +2 reviews given by this user.',
                header: (<span>+2<br/>given</span>),
                cell: (record, index) => (
                    <Td key={'reviewCountPlus2' + index} column='reviewCountPlus2'
                        style={this.computeCellStyle(index, 'reviewCountPlus2')}>
                        {record.reviewCountPlus2}
                    </Td>
                ),
            },
            'reviewCountPlus1': {
                sortFunction: Reactable.Sort.NumericInteger,
                highlighter: new TableCellHighlighter(overviewUserdata, selectedUsers, 'reviewCountPlus1'),
                description: 'Number of +1 reviews given by this user.',
                header: (<span>+1<br/>given</span>),
                cell: (record, index) => (
                    <Td key={'reviewCountPlus1' + index} column='reviewCountPlus1'
                        style={this.computeCellStyle(index, 'reviewCountPlus1')}>
                        {record.reviewCountPlus1}
                    </Td>
                ),
            },
            'reviewCountMinus1': {
                sortFunction: Reactable.Sort.NumericInteger,
                highlighter: new TableCellHighlighter(overviewUserdata, selectedUsers, 'reviewCountMinus1'),
                description: 'Number of -1 reviews given by this user.',
                header: (<span>-1<br/>given</span>),
                cell: (record, index) => (
                    <Td key={'reviewCountMinus1' + index} column='reviewCountMinus1'
                        style={this.computeCellStyle(index, 'reviewCountMinus1')}>
                        {record.reviewCountMinus1}
                    </Td>
                ),
            },
            'reviewCountMinus2': {
                sortFunction: Reactable.Sort.NumericInteger,
                highlighter: new TableCellHighlighter(overviewUserdata, selectedUsers, 'reviewCountMinus2'),
                description: 'Number of -2 reviews given by this user.',
                header: (<span>-2<br/>given</span>),
                cell: (record, index) => (
                    <Td key={'reviewCountMinus2' + index} column='reviewCountMinus2'
                        style={this.computeCellStyle(index, 'reviewCountMinus2')}>
                        {record.reviewCountMinus2}
                    </Td>
                ),
            },
            'allCommentsWritten': {
                sortFunction: Reactable.Sort.NumericInteger,
                highlighter: new TableCellHighlighter(overviewUserdata, selectedUsers, 'allCommentsWritten'),
                description: 'Number of review comments written to other people\'s commits by this user.',
                header: (<span>Comments<br/>written</span>),
                cell: (record, index) => (
                    <Td key={'allCommentsWritten' + index}
                        column='allCommentsWritten' style={this.computeCellStyle(index, 'allCommentsWritten')}>
                        {record.allCommentsWritten}
                    </Td>
                ),
            },
            'allCommentsReceived': {
                sortFunction: Reactable.Sort.NumericInteger,
                highlighter: new TableCellHighlighter(overviewUserdata, selectedUsers, 'allCommentsReceived')
                                 .setIsAscending(true)
                                 .setIgnoreFunction((element) => element.commitCount == 0),
                description: 'Number of review comments received by this user.',
                header: (<span>Comments<br/>received</span>),
                cell: (record, index) => (
                    <Td key={'allCommentsReceived' + index} column='allCommentsReceived'
                        style={this.computeCellStyle(index, 'allCommentsReceived')}>
                        {record.allCommentsReceived}
                    </Td>
                ),
            },
            'commitCount': {
                sortFunction: Reactable.Sort.NumericInteger,
                highlighter: new TableCellHighlighter(overviewUserdata, selectedUsers, 'commitCount')
                                 .setIgnoreZeroes(true),
                description: 'Number of commits made by this user.',
                header: 'Commits',
                cell: (record, index) => (
                    <Td key={'commitCount' + index} column='commitCount'
                        style={this.computeCellStyle(index, 'commitCount')}>
                        {record.commitCount}
                    </Td>
                ),
            },
            'receivedCommentRatio': {
                sortFunction: decimalComparator,
                highlighter: new TableCellHighlighter(overviewUserdata, selectedUsers, 'receivedCommentRatio')
                                 .setIsAscending(true)
                                 .setIgnoreFunction((element) => element.commitCount == 0),
                description: 'The ratio of comments received by user per commit.',
                header: (<span>Comments<br/>/ commit</span>),
                cell: (record, index) => (
                    <Td key={'receivedCommentRatio' + index} column='receivedCommentRatio'
                        value={record.receivedCommentRatio}
                        style={this.computeCellStyle(index, 'receivedCommentRatio')}>
                        {numeral(record.receivedCommentRatio).format('0.000')}
                    </Td>
                ),
            },
            'reviewCommentRatio': {
                sortFunction: decimalComparator,
                highlighter: new TableCellHighlighter(overviewUserdata, selectedUsers, 'reviewCommentRatio'),
                description: 'The ratio of comments written by this user per a review request.',
                header: (<span>Comments<br/>/ review<br/>requests</span>),
                cell: (record, index) => (
                    <Td key={'reviewCommentRatio' + index} column='reviewCommentRatio'
                        value={record.reviewCommentRatio}
                        style={this.computeCellStyle(index, 'reviewCommentRatio')}>
                        {numeral(record.reviewCommentRatio).format('0.000')}
                    </Td>
                ),
            },
            'addedAsReviewerToCount': {
                sortFunction: Reactable.Sort.NumericInteger,
                highlighter: new TableCellHighlighter(overviewUserdata, selectedUsers, 'addedAsReviewerToCount'),
                description: 'Number of times this user was added as a reviewer.',
                header: (<span>Added as<br/>reviewer</span>),
                cell: (record, index) => (
                    <Td key={'addedAsReviewerToCount' + index} column='addedAsReviewerToCount'
                        style={this.computeCellStyle(index, 'addedAsReviewerToCount')}>
                        {record.addedAsReviewerToCount}
                    </Td>
                ),
            },
            'selfReviewedCommitCount': {
                sortFunction: Reactable.Sort.NumericInteger,
                highlighter: new TableCellHighlighter(overviewUserdata, selectedUsers, 'selfReviewedCommitCount')
                                 .setIsAscending(true)
                                 .setHighlightPositiveEntries(false)
                                 .setIgnoreZeroes(true),
                description: 'Number of times the user merged a change after self-reviewing it, without having any reviews in any patch set from other users.',
                header: 'Self-reviews',
                cell: (record, index) => (
                    <Td key={'selfReviewedCommitCount' + index} column='selfReviewedCommitCount'
                        style={this.computeCellStyle(index, 'selfReviewedCommitCount')}>
                        {record.selfReviewedCommitCount}
                    </Td>
                ),
            },
            'averageTimeInCodeReview': {
                sortFunction: Reactable.Sort.NumericInteger,
                highlighter: new TableCellHighlighter(overviewUserdata, selectedUsers, 'averageTimeInCodeReview')
                                 .setIsAscending(true)
                                 .setIgnoreZeroes(true),
                description: 'Average time the user\'s commits spent in review.',
                header: (<span>Average time<br/>in review</span>),
                cell: (record, index) => (
                    <Td key={'averageTimeInCodeReview' + index}
                        column='averageTimeInCodeReview' value={record.averageTimeInCodeReview}
                        style={this.computeCellStyle(index, 'averageTimeInCodeReview')}>
                        {formatPrintableDuration(record.averageTimeInCodeReview)}
                    </Td>
                ),
            },
        };
    }

    componentWillReceiveProps(nextProps) {
        Object.keys(this.state.columnMetadata).forEach(function(columnName) {
            const metadata = this.state.columnMetadata[columnName];
            if (metadata.highlighter) {
                metadata.highlighter.setOverviewUserdata(nextProps.overviewUserdata);
            }
        }.bind(this));

        if (!nextProps.selectedUsers.equals(this.state.selectedUsers)) {
            this.setState({
                selectedUsers: nextProps.selectedUsers,
            });
        }
    }

    onSelectAllCheckboxValueChanged() {
        const selectedUsers = this.state.selectedUsers;
        const isAllSelected = selectedUsers.isAllUsersSelected();
        const newSelectedUsers = isAllSelected ? selectedUsers.selectNone() : selectedUsers.selectAll();
        this.updateSelectedUsersForHighlighters(newSelectedUsers);

        this.setState({
            selectedUsers: newSelectedUsers,
        }, this.emitUserSelectionUpdate);
    }

    onIdentityCheckboxValueChanged(identifier) {
        const newSelectedUsers = this.state.selectedUsers.toggleSelection(identifier);
        this.updateSelectedUsersForHighlighters(newSelectedUsers);

        this.setState({
            selectedUsers: newSelectedUsers,
        }, this.emitUserSelectionUpdate);
    }

    updateSelectedUsersForHighlighters(selectedUsers) {
        Object.keys(this.state.columnMetadata).forEach(function(columnName) {
            const metadata = this.state.columnMetadata[columnName];
            if (metadata.highlighter) {
                metadata.highlighter.setSelectedUsers(selectedUsers);
            }
        }.bind(this));
    }

    emitUserSelectionUpdate() {
        if (this.props.onUserSelectionChanged) {
            this.props.onUserSelectionChanged(this.state.selectedUsers);
        }
    }

    computeCellStyle(index, columnName) {
        const metadata = this.state.columnMetadata[columnName];
        var style = {};
        if (metadata.highlighter) {
            const backgroundColor = metadata.highlighter.getHighlightColor(index);
            if (backgroundColor && backgroundColor.length > 0) {
                style.backgroundColor = backgroundColor;
            }
        }
        return style;
    }

    renderRow(index, overviewRecord) {
        const isUserSelected = this.state.selectedUsers.isUserSelected(overviewRecord.identifier);

        const selectionStyle = {
            color: isUserSelected ? '' : OverviewTable.COLOR_UNSELECTED
        };

        var rowCells = Object.keys(this.state.columnMetadata).map(function(columnName) {
            const metadata = this.state.columnMetadata[columnName];
            return metadata.cell(overviewRecord, index);
        }.bind(this));

        return (
            <Tr key={'r_' + index} style={selectionStyle}>
                {rowCells}
            </Tr>
        );
    }

    render() {
        return (
            <SimpleSortableTable
                columnMetadata={this.state.columnMetadata}
                rowData={this.props.overviewUserdata}
                rowRenderer={this.renderRow.bind(this)} />
        );
    }
}

OverviewTable.displayName = 'OverviewTable';

OverviewTable.defaultProps = {
    datasetOverview: {},
    overviewUserdata: [],
    onUserSelectionChanged: null
};

OverviewTable.propTypes = {
    datasetOverview: React.PropTypes.object,
    overviewUserdata: React.PropTypes.array,
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    onUserSelectionChanged: React.PropTypes.func
};

OverviewTable.COLOR_UNSELECTED = '#cccccc';