import {fromJS} from 'immutable';
import React from 'react';

import ClearFloat from '../common/ClearFloat';
import GerritVersionAlerts from '../common/GerritVersionAlerts';
import PageHeader from '../common/header/PageHeader';
import Panel from '../common/Panel';
import NavigationBar from '../common/header/NavigationBar';
import GlobalJavascriptLoader from '../common/loader/GlobalJavascriptLoader';
import SelectedUsers from '../common/model/SelectedUsers';

import OverviewTable from './OverviewTable';
import OverviewTeamGraphView from './OverviewTeamGraphView';

export default class OverviewPage extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            overviewUserdata: [],
            currentSelection: this.props.route.currentSelection
        };
    }

    componentDidMount() {
        var jsLoader = new GlobalJavascriptLoader();
        jsLoader.loadJavascriptFile('./data/overview.js', function() {
            this.setState({
                overviewUserdata: window.overviewUserdata,
            });
        }.bind(this));
    }

    onUserSelectionChanged(newSelectedUsers) {
        const newSelection = {
            selectedUsers: newSelectedUsers
        };
        this.setState({
            currentSelection: newSelection
        }, this.emitUserSelectionUpdate);
    }

    emitUserSelectionUpdate() {
        if (this.props.route.onCurrentSelectionChanged) {
            this.props.route.onCurrentSelectionChanged(this.state.currentSelection);
        }
    }

    getSubtitleFromDatasetName() {
        const datasetOverview = this.props.route.datasetOverview;
        var dataSetName = datasetOverview['projectName'];
        var filenames = datasetOverview['filenames'];
        if (filenames && filenames.length > 20) {
            var firstFilename = filenames[0];
            var lastFilename = filenames[datasetOverview.filenames.length - 1];
            dataSetName = filenames.length + ' files, from '
                        + firstFilename + ' to ' + lastFilename;
        }
        return dataSetName;
    }

    render() {
        document.title = 'GerritStats for ' + window.datasetOverview['projectName'];

        const navBarProps = {
            elements: fromJS([
                { key: 'overview', displayName: 'Overview', },
                { key: 'teamGraph', displayName: 'Team graph', },
            ]),
            onSelectedListener: function(selectedKey) {
                if (selectedKey == 'overview') {
                    this.refs.overviewHashLink.scrollIntoView();
                } else if (selectedKey == 'teamGraph') {
                    this.refs.teamGraphHashLink.scrollIntoView();
                }
            }.bind(this),
        };
        const headerProps = {
            datasetOverview: this.props.route.datasetOverview,
            selectedUsers: this.state.currentSelection.selectedUsers,
            subtitle: this.getSubtitleFromDatasetName(),
        };
        const overviewProps = {
            datasetOverview: this.props.route.datasetOverview,
            overviewUserdata: this.state.overviewUserdata,
            selectedUsers: this.state.currentSelection.selectedUsers,
            onUserSelectionChanged: this.onUserSelectionChanged.bind(this),
        };
        const teamGraphProps = {
            overviewUserdata: this.state.overviewUserdata,
            selectedUsers: this.state.currentSelection.selectedUsers,
            onUserSelectionChanged: this.onUserSelectionChanged.bind(this),
            highlightedIdentifier: this.state.highlightedIdentifier,
        };

        return (
            <div>
                <PageHeader {...headerProps} />
                <NavigationBar {...navBarProps} />
                <content>
                    <div className='centered'>
                        <GerritVersionAlerts datasetOverview={this.props.route.datasetOverview} />
                    </div>
                    <a name='overview' ref='overviewHashLink'></a>
                    <Panel title='Overview' size='flex'>
                        <OverviewTable {...overviewProps} />
                    </Panel>
                    <ClearFloat />
                    <a name='teamGraph' ref='teamGraphHashLink'></a>
                    <OverviewTeamGraphView {...teamGraphProps} />
                </content>
            </div>
        );
    }
}

OverviewPage.displayName = 'OverviewPage';

OverviewPage.propTypes = {
    route: React.PropTypes.shape({
        datasetOverview: React.PropTypes.object.isRequired,
        currentSelection: React.PropTypes.shape({
            selectedUsers: React.PropTypes.instanceOf(SelectedUsers)
        }),
        onCurrentSelectionChanged: React.PropTypes.func
    })
};