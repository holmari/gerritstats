import React from 'react';
import {Link} from 'react-router';
import {Td} from 'reactable';
import Reactable from 'reactable';

import Panel from '../common/Panel';
import SimpleSortableTable from '../common/SimpleSortableTable';

import {getShortPrintableName, getProfilePageLinkForIdentity} from '../common/model/GerritUserdata';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

import ReviewersAndApprovalsChartView from './charts/ReviewersAndApprovalsChartView';

export default class ReviewersAndApprovalsView extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedUsers: this.props.selectedUsers,
            highlightedIdentifier: null,
        };
        this.computeData(this.state.selectedUsers);
    }

    computeData(selectedUsers) {
        this._reviewerData = this.props.userdata.getFilteredReviewerDataForOwnCommits(selectedUsers);
    }

    componentWillReceiveProps(nextProps) {
        if (!nextProps.selectedUsers.equals(this.state.selectedUsers)) {
            this.computeData(nextProps.selectedUsers);
            this.setState({
                selectedUsers: nextProps.selectedUsers,
            });
        }
    }

    getColumnMetadata() {
        return {
            'name': {
                header: 'Name',
                description: 'The name of the user, as shown in Gerrit.',
                sortFunction: Reactable.Sort.CaseInsensitive,
                cell: (data, index) => (
                    <Td key={index} column='name' value={getShortPrintableName(data.identity)}>
                        <Link to={getProfilePageLinkForIdentity(data.identity)}>
                            {getShortPrintableName(data.identity)}
                        </Link>
                    </Td>
                ),
            },
            'timesAdded': {
                header: () => (
                    <span>Times<br/>added</span>
                ),
                description: 'How many times this user was added as a reviewer.',
                sortFunction: Reactable.Sort.NumericInteger,
                cell: (data, index) => (
                    <Td key={index} column='timesAdded'>{data.approvalData.addedAsReviewerCount}</Td>
                ),
            },
            'approvalsGiven': {
                header: () => (
                    <span>Approvals<br/>given</span>
                ),
                description: 'How many approvals (-2..+2) in total did this user grant in the reviews.',
                sortFunction: Reactable.Sort.NumericInteger,
                cell: (data, index) => (
                    <Td key={index} column='approvalsGiven'>{data.approvalData.approvalCount}</Td>
                ),
            },
        };
    }

    onHighlightedRowIndexChanged(rowIndex) {
        const identifier = rowIndex != -1 ? this._reviewerData[rowIndex].identity['identifier'] : null;
        this.onSelectedIdentifierChanged(identifier);
    }

    onSelectedIdentifierChanged(selectedIdentifier) {
        this.setState({
            highlightedIdentifier: selectedIdentifier,
        });
    }

    getHighlightedRowIndex() {
        if (this.state.highlightedIdentifier) {
            return this._reviewerData.findIndex((item) =>
                item.identity['identifier'] == this.state.highlightedIdentifier);
        } else {
            return -1;
        }
    }

    render() {
        const tableProps = {
            columnMetadata: this.getColumnMetadata(),
            rowData: this._reviewerData,
            onHighlightedRowIndexChanged: this.onHighlightedRowIndexChanged.bind(this),
            highlightedRowIndex: this.getHighlightedRowIndex()
        };

        return (
            <div>
                <Panel title='Adds them as reviewers' size='half'>
                    <SimpleSortableTable {...tableProps} />
                </Panel>
                <Panel title='Reviewers and approvals' size='half'>
                    <ReviewersAndApprovalsChartView {...this.props}
                        onGraphSelectionChanged={this.onSelectedIdentifierChanged.bind(this)}
                        highlightedIdentifier={this.state.highlightedIdentifier}
                    />
                </Panel>
            </div>
        );
    }
}

ReviewersAndApprovalsView.displayName = 'ReviewersAndApprovalsView';

ReviewersAndApprovalsView.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
};