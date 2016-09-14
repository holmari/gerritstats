import './ProfilePage.scss';

import React from 'react';
import {fromJS} from 'immutable';
import {ProgressBar} from 'react-bootstrap';

import ClearFloat from '../common/ClearFloat';
import GerritVersionAlerts from '../common/GerritVersionAlerts';
import PageFooter from '../common/PageFooter';
import NavigationBar from '../common/header/NavigationBar';
import PageHeader from '../common/header/PageHeader';
import UserdataLoader from '../common/loader/UserdataLoader';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

import NumberPanel from './components/NumberPanel';

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

        this.updateOnIdentifierChange(this.props.params.identifier);
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
            })
            this.loadDataForIdentifier(identifier);
        } else {
            console.error("ProfilePage opened without an identifier!");
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
                    <ReviewsGivenPanel {...componentProps} />
                    <ReviewsReceivedPanel {...componentProps} />
                    <CommitsPanel {...componentProps} />
                    <ClearFloat />
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
                    <IterationView {...componentProps} />
                    <ClearFloat />
                    <PerMonthStatsPanel {...componentProps} />
                    <ClearFloat />
                    <ProjectsContributedToPanel {...componentProps} />
                    <ClearFloat />
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
                if (selectedKey == 'overview') {
                    this.refs.overviewHashLink.scrollIntoView();
                } else if (selectedKey == 'teamGraph') {
                    this.refs.teamGraphHashLink.scrollIntoView();
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