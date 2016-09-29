import './OverviewTeamGraphView.scss';

import React from 'react';

import Panel from '../common/Panel';
import ProximityGraphView from '../common/charts/ProximityGraphView';

import SelectedUsers from '../common/model/SelectedUsers';

import UserPickerPanel from './UserPickerPanel';

export default class OverviewTeamGraphView extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedUsers: this.props.selectedUsers,
            highlightedIdentifier: null,
        };
    }

    componentWillReceiveProps(nextProps) {
        if (!nextProps.selectedUsers.equals(this.state.selectedUsers)) {
            this.setState({
                selectedUsers: nextProps.selectedUsers,
            });
        }
        if (nextProps.highlightedIdentifier != this.state.highlightedIdentifier) {
            this.setState({
                highlightedIdentifier: nextProps.highlightedIdentifier,
            });
        }
    }

    emitUserSelectionUpdate() {
        if (this.props.onUserSelectionChanged) {
            this.props.onUserSelectionChanged(this.state.selectedUsers);
        }
    }

    onUserSelectionChanged(newSelectedUsers) {
        this.setState({
            selectedUsers: newSelectedUsers,
        }, this.emitUserSelectionUpdate);
    }

    onHighlightedIdentifierChanged(highlightedIdentifier) {
        this.setState({
            highlightedIdentifier: highlightedIdentifier,
        });
    }

    render() {
        const userPickerPanelProps = {
            selectedUsers: this.state.selectedUsers,
            overviewUserdata: this.props.overviewUserdata,
            highlightedIdentifier: this.state.highlightedIdentifier,
            onUserSelectionChanged: this.onUserSelectionChanged.bind(this),
            onHighlightedIdentifierChanged: this.onHighlightedIdentifierChanged.bind(this),
        };
        const teamGraphProps = {
            selectedUsers: this.state.selectedUsers,
            overviewUserdata: this.props.overviewUserdata,
            highlightedIdentifier: this.state.highlightedIdentifier,
            onHighlightedIdentifierChanged: this.onHighlightedIdentifierChanged.bind(this),
        };

        return (
            <div className='centered'>
                <UserPickerPanel {...userPickerPanelProps} />
                <Panel title='Team graph' size='threeFourths'>
                    <ProximityGraphView {...teamGraphProps} />
                </Panel>
            </div>
        );
    }
}

OverviewTeamGraphView.displayName = 'OverviewTeamGraphView';

OverviewTeamGraphView.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    overviewUserdata: React.PropTypes.array.isRequired,
    onUserSelectionChanged: React.PropTypes.func,
};