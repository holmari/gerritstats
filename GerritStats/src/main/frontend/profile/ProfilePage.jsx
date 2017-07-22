import './ProfilePage.scss';

import React from 'react';
import {fromJS} from 'immutable';
import {ProgressBar} from 'react-bootstrap';

import ClearFloat from '../common/ClearFloat';
import GerritVersionAlerts from '../common/GerritVersionAlerts';
import NavigationBar from '../common/header/NavigationBar';
import PageHeader from '../common/header/PageHeader';
import UserdataLoader from '../common/loader/UserdataLoader';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

import CommitsPanel from './CommitsPanel';
import CumulativeStatisticsPanel from './CumulativeStatisticsPanel';
import IterationView from './IterationView';
import PerMonthStatsPanel from './PerMonthStatsPanel';
import ProjectsContributedToPanel from './ProjectsContributedToPanel';
import ReviewersAndApprovalsView from './ReviewersAndApprovalsView';
import ReviewCommentsPerDayPanel from './ReviewCommentsPerDayPanel';
import ReviewsGivenPanel from './ReviewsGivenPanel';
import ReviewsReceivedDetailsPanel from './ReviewsReceivedDetailsPanel';
import ReviewsReceivedPanel from './ReviewsReceivedPanel';
import ReviewCommentsPanel from './ReviewCommentsPanel';
import TeamGraphView from './TeamGraphView';
import TimelinePanel from './TimelinePanel';


export default class ProfilePage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            dataLoaded: false,
            userdata: {},
        };

        const identifier = this.props.params.identifier;
        if (identifier && identifier.length) {
            this.loadDataForIdentifier(identifier);
        }
    }

    componentWillReceiveProps(nextProps) {
        const nextIdentifier = nextProps.params.identifier;
        if (this.props.params.identifier != nextIdentifier) {
            this.updateOnIdentifierChange(nextIdentifier);
        }
    }

    updateOnIdentifierChange(identifier) {
        if (identifier && identifier.length) {
            this.setState({
                dataLoaded: false,
                userdata: {},
            });
            this.loadDataForIdentifier(identifier);
        } else {
            throw new Error('ProfilePage opened without an identifier!');
        }
    }

    loadDataForIdentifier(identifier) {
        var userdataLoader = new UserdataLoader();
        userdataLoader.load(identifier, function(loadedData) {
            this.setState({
                dataLoaded: true,
                userdata: new GerritUserdata(loadedData)
            });
        }.bind(this));
    }

    renderContent() {
        if (this.state.dataLoaded) {
            const componentProps = {
                userdata: this.state.userdata,
                selectedUsers: this.props.route.currentSelection.selectedUsers
            };
            return (
                <div>
                    <a name='overall' ref='overallHashLink'></a>
                    <ReviewsGivenPanel {...componentProps} />
                    <ReviewsReceivedPanel {...componentProps} />
                    <CommitsPanel {...componentProps} />
                    <ClearFloat />
                    <a name='reviewers' ref='reviewersHashLink'></a>
                    <ReviewersAndApprovalsView {...componentProps} />
                    <ClearFloat />
                    <TeamGraphView {...componentProps} />
                    <ClearFloat />
                    <div>
                        <TimelinePanel {...componentProps} />
                        <ReviewsReceivedDetailsPanel {...componentProps} />
                    </div>
                    <ClearFloat />
                    <ReviewCommentsPerDayPanel {...componentProps} />
                    <ClearFloat />
                    <CumulativeStatisticsPanel {...componentProps} />
                    <ClearFloat />
                    <a name='iteration' ref='iterationHashLink'></a>
                    <IterationView {...componentProps} />
                    <ClearFloat />
                    <a name='perMonth' ref='perMonthHashLink'></a>
                    <PerMonthStatsPanel {...componentProps} />
                    <ClearFloat />
                    <ProjectsContributedToPanel {...componentProps} />
                    <ClearFloat />
                    <a name='comments' ref='commentsHashLink'></a>
                    <ReviewCommentsPanel {...componentProps} />
                </div>
            );
        } else {
            return (
                <ProgressBar active now={100} />
            );
        }
    }

    render() {
        const navBarProps = {
            elements: fromJS([
                { key: 'overall', displayName: 'Overall', },
                { key: 'reviewers', displayName: 'Reviewers and approvals', },
                { key: 'iteration', displayName: 'Iteration', },
                { key: 'perMonth', displayName: 'Per-month', },
                { key: 'comments', displayName: 'Comments', }
            ]),
            onSelectedListener: function(selectedKey) {
                if (selectedKey == 'overall') {
                    this.refs.overallHashLink.scrollIntoView();
                } else if (selectedKey == 'reviewers') {
                    this.refs.reviewersHashLink.scrollIntoView();
                } else if (selectedKey == 'iteration') {
                    this.refs.iterationHashLink.scrollIntoView();
                } else if (selectedKey == 'perMonth') {
                    this.refs.perMonthHashLink.scrollIntoView();
                } else if (selectedKey == 'comments') {
                    this.refs.commentsHashLink.scrollIntoView();
                }
            }.bind(this),
        };
        const userdata = this.state.userdata;
        var headerProps = {
            showBackButton: true,
            mainTitle: 'GerritStats',
            subtitle: '\u2013'
        };
        const identifier = this.props.params.identifier;
        if (this.state.dataLoaded) {
            headerProps = {
                showBackButton: true,
                mainTitle: userdata.getShortPrintableName(),
                subtitle: userdata.getPrintableEmailAndIdentity()
            };
            document.title = `GerritStats: ${userdata.getPrintableName()}`;
        } else {
            document.title = `GerritStats: loading for '${identifier}'`;
        }

        return (
            <div>
                <PageHeader
                     datasetOverview={this.props.route.datasetOverview} {...headerProps}
                     selectedUsers={this.props.route.currentSelection.selectedUsers} />
                <NavigationBar {...navBarProps} />
                <content className='centered'>
                    <GerritVersionAlerts datasetOverview={this.props.route.datasetOverview} />
                    {this.renderContent()}
                </content>
            </div>
        );
    }
}

ProfilePage.displayName = 'ProfilePage';

ProfilePage.propTypes = {
    route: React.PropTypes.shape({
        datasetOverview: React.PropTypes.object.isRequired,
        currentSelection: React.PropTypes.shape({
            selectedUsers: React.PropTypes.instanceOf(SelectedUsers)
        })
    })
};