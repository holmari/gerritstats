import './OverviewTeamGraphView.scss';

import classnames from 'classnames';
import React from 'react';

import Panel from '../common/Panel';
import SelectedUsers from '../common/model/SelectedUsers';

import TeamGraphUserPickerTable from './TeamGraphUserPickerTable';

export default class UserPickerPanel extends React.Component {

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

    getClassNames() {
        return classnames(super.getClassNames(), 'userPickerTable');
    }

    emitUserSelectionUpdate() {
        if (this.props.onUserSelectionChanged) {
            this.props.onUserSelectionChanged(this.state.selectedUsers);
        }
    }

    onUserSelectionChanged(newSelectedUsers) {
        this.setState({
            selectedUsers: newSelectedUsers
        }, this.emitUserSelectionUpdate);
    }

    onHighlightedIdentifierChanged(highlightedIdentifier) {
        this.setState({
            highlightedIdentifier: highlightedIdentifier,
        });
        if (this.props.onHighlightedIdentifierChanged) {
            this.props.onHighlightedIdentifierChanged(highlightedIdentifier);
        }
    }

    render() {
        const tableProps = {
            selectedUsers: this.state.selectedUsers,
            overviewUserdata: this.props.overviewUserdata,
            highlightedIdentifier: this.state.highlightedIdentifier,
            onUserSelectionChanged: this.onUserSelectionChanged.bind(this),
            onHighlightedIdentifierChanged: this.onHighlightedIdentifierChanged.bind(this),
        };
        return (
            <Panel title='Users in analysis' size='fourth'>
                <div className='userPickerTable'>
                    <TeamGraphUserPickerTable {...tableProps} />
                </div>
            </Panel>
        );
    }
}

UserPickerPanel.displayName = 'UserPickerPanel';

UserPickerPanel.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    overviewUserdata: React.PropTypes.array.isRequired,
    highlightedIdentifier: React.PropTypes.string,
    onUserSelectionChanged: React.PropTypes.func,
    onHighlightedIdentifierChanged: React.PropTypes.func,
};