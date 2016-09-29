import '../style/tables.scss';

import React from 'react';
import {ProgressBar} from 'react-bootstrap';
import {Link} from 'react-router';
import {Td} from 'reactable';
import Reactable from 'reactable';

import SimpleSortableTable from '../common/SimpleSortableTable';
import ClearFloat from '../common/ClearFloat';
import Panel from '../common/Panel';
import ProximityGraphView from '../common/charts/ProximityGraphView';
import {createIdentityGraph} from '../common/charts/ProximityGraph';
import GlobalJavascriptLoader from '../common/loader/GlobalJavascriptLoader';
import {getShortPrintableName, getProfilePageLinkForIdentity} from '../common/model/GerritUserdata';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

function hasIdentifier(reviewerData, identifierToSearch) {
    for (let i = 0; i < reviewerData.length; ++i) {
        if (reviewerData[i].identity['identifier'] === identifierToSearch) {
            return true;
        }
    }
    return false;
}

export default class TeamGraphView extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedUsers: this.props.selectedUsers,
            highlightedIdentifier: null,
            overviewUserdata: window.overviewUserdata,
        };
        this.computeData(this.state.selectedUsers);

        if (!this.state.overviewUserdata) {
            var jsLoader = new GlobalJavascriptLoader();
            jsLoader.loadJavascriptFile('./data/overview.js', function() {
                this.setState({
                    overviewUserdata: window.overviewUserdata,
                });
            }.bind(this));
        }
    }

    componentWillReceiveProps(nextProps) {
        if (!nextProps.selectedUsers.equals(this.state.selectedUsers)) {
            this.setState({
                selectedUsers: nextProps.selectedUsers,
            });
            this.computeData(nextProps.selectedUsers);
        }
    }

    computeData(selectedUsers) {
        this._reviewRequestorData = this.props.userdata.getReviewRequestors(selectedUsers);
        this._nonRespondingUserData = [];

        const reviewerData = this.props.userdata.getFilteredReviewerDataForOwnCommits(selectedUsers);
        // append all users in 'team' who did not request reviews
        for (let i = 0; i < reviewerData.length; ++i) {
            var reviewerItem = reviewerData[i];
            var identifier = reviewerItem.identity['identifier'];
            if (!hasIdentifier(this._reviewRequestorData, identifier)) {
                this._nonRespondingUserData.push({
                    'approvalData': {
                        'addedAsReviewerCount': -1,
                        'approvalCount': -1
                    },
                    'identity': reviewerItem.identity
                });
            }
        }
    }

    getColumnMetadataForReviewRequestors() {
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
                    <span>Reviews<br/>requested</span>
                ),
                sortFunction: Reactable.Sort.NumericInteger,
                description: 'How many reviews did this user request. Each changeset counts as one.',
                cell: (data, index) => (
                    <Td key={index} column='timesAdded'>{data.approvalData.addedAsReviewerCount}</Td>
                ),
            },
            'approvalsReceived': {
                header: () => (
                    <span>Approvals<br/>received</span>
                ),
                sortFunction: Reactable.Sort.NumericInteger,
                description: 'How many approvals (-2..+2) this user received from the author of this profile.',
                cell: (data, index) => (
                    <Td key={index} column='approvalsReceived'>{data.approvalData.approvalCount}</Td>
                ),
            }
        };
    }

    getColumnMetadataForNoResponses() {
        return {
            'name': {
                header: 'Name',
                description: 'The name of the user, as shown in Gerrit.',
                sortFunction: Reactable.Sort.CaseInsensitive,
                cell: (data, index) => (
                    <Td key={'name_' + index} column='name'>
                        <Link to={getProfilePageLinkForIdentity(data.identity)}>
                            {getShortPrintableName(data.identity)}
                        </Link>
                    </Td>
                ),
            }
        };
    }

    getIdentifierForRowIndex(table, index) {
        return (index != -1 && index < table.length) ? table[index].identity['identifier'] : null;
    }

    onHighlightedRowChangedForReviewRequestorTable(highlightedIndex) {
        const identifier = this.getIdentifierForRowIndex(this._reviewRequestorData, highlightedIndex);
        this.onHighlightedIdentifierChanged(identifier);
    }

    onHighlightedRowChangedForNonRespondingTable(highlightedIndex) {
        const identifier = this.getIdentifierForRowIndex(this._nonRespondingUserData, highlightedIndex);
        this.onHighlightedIdentifierChanged(identifier);
    }

    onHighlightedIdentifierChanged(highlightedIdentifier) {
        this.setState({
            highlightedIdentifier: highlightedIdentifier,
        });
    }

    getRowIndexForIdentifier(table, identifierToSeek) {
        return table.findIndex((item) => item.identity['identifier'] == identifierToSeek);
    }

    render() {
        const highlightedIdentifier = this.state.highlightedIdentifier;
        const reviewRequestorTableProps = {
            columnMetadata: this.getColumnMetadataForReviewRequestors(),
            rowData: this._reviewRequestorData,
            onHighlightedRowIndexChanged: this.onHighlightedRowChangedForReviewRequestorTable.bind(this),
            highlightedRowIndex: this.getRowIndexForIdentifier(this._reviewRequestorData, highlightedIdentifier),
        };
        const noResponseTableProps = {
            columnMetadata: this.getColumnMetadataForNoResponses(),
            rowData: this._nonRespondingUserData,
            onHighlightedRowIndexChanged: this.onHighlightedRowChangedForNonRespondingTable.bind(this),
            highlightedRowIndex: this.getRowIndexForIdentifier(this._nonRespondingUserData, highlightedIdentifier),
        };

        var teamGraph = null;
        if (this.state.overviewUserdata) {
            const config = {
                width: 490,
                height: 490,
                relativeLinkValueThreshold: 0.025,
                charge: -200,
                linkDistance: 25,
                drawCrosshair: true,
                highlightSelection: true,
                defaultItemOpacity: 0.6,
                centeredIdentifier: this.props.userdata.getIdentifier(),
            };

            const teamIdentities = this.props.userdata.getTeamIdentities(this.state.selectedUsers);
            const teamGraphProps = {
                selectedUsers: this.state.selectedUsers,
                overviewUserdata: this.state.overviewUserdata,
                highlightedIdentifier: this.state.highlightedIdentifier,
                onHighlightedIdentifierChanged: this.onHighlightedIdentifierChanged.bind(this),
                identityGraph: createIdentityGraph(this.state.overviewUserdata, teamIdentities),
                graphConfig: config,
            };

            teamGraph = (
                <ProximityGraphView {...teamGraphProps} />
            );
        } else {
            teamGraph = (
                <ProgressBar active now={100} />
            );
        }

        return (
            <div>
                <div className='panelBox'>
                    <Panel title='They request reviews' size='half'>
                        <SimpleSortableTable {...reviewRequestorTableProps} />
                    </Panel>
                    <ClearFloat />
                    <Panel title='They never responded to review requests' size='half'>
                        <SimpleSortableTable {...noResponseTableProps} />
                    </Panel>
                </div>
                <Panel title='Team graph' size='half'>
                    {teamGraph}
                </Panel>
            </div>
        );
    }
}

TeamGraphView.displayName = 'TeamGraphView';

TeamGraphView.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
};