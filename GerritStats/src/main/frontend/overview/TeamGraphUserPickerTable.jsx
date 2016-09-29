import '../style/tables.scss';

import React from 'react';
import {Td} from 'reactable';
import Reactable from 'reactable';

import SimpleSortableTable from '../common/SimpleSortableTable';
import {getShortPrintableName} from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

function scrollIntoView(element) {
    if (element.scrollIntoViewIfNeeded) {
        element.scrollIntoViewIfNeeded();
    } else {
        element.scrollIntoView();
    }
}

export default class TeamGraphUserPickerTable extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedUsers: this.props.selectedUsers,
            highlightedIdentifier: null,
            columnMetadata: this.getDefaultColumnMetadata(),
        };
    }

    componentWillReceiveProps(nextProps) {
        if (!nextProps.selectedUsers.equals(this.state.selectedUsers)) {
            this.setState({
                selectedUsers: nextProps.selectedUsers,
            });
        }
        if (nextProps.highlightedIdentifier != this.state.highlightedIdentifier) {
            if (nextProps.highlightedIdentifier) {
                const element = this.refs.table.refs[nextProps.highlightedIdentifier];
                scrollIntoView(element);
            }

            this.setState({
                highlightedIdentifier: nextProps.highlightedIdentifier,
            });
        }
    }

    getDefaultColumnMetadata() {
        return {
            selected: {
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
            name: {
                sortFunction: Reactable.Sort.CaseInsensitive,
                description: 'The name of the user, as shown in Gerrit.',
                header: 'Name',
                cell: (record, index) => (
                    <Td key={'name' + index} column='name' value={getShortPrintableName(record.identity)}>
                        <div ref={record.identifier}>{getShortPrintableName(record.identity)}</div>
                    </Td>
                ),
            }
        };
    }

    emitUserSelectionUpdate() {
        if (this.props.onUserSelectionChanged) {
            this.props.onUserSelectionChanged(this.state.selectedUsers);
        }
    }

    onIdentityCheckboxValueChanged(identifier) {
        const newSelectedUsers = this.state.selectedUsers.toggleSelection(identifier);

        this.setState({
            selectedUsers: newSelectedUsers,
        }, this.emitUserSelectionUpdate);
    }

    onSelectAllCheckboxValueChanged() {
        const selectedUsers = this.state.selectedUsers;
        const isAllSelected = selectedUsers.isAllUsersSelected();

        this.setState({
            selectedUsers: isAllSelected ? selectedUsers.selectNone() : selectedUsers.selectAll(),
        }, this.emitUserSelectionUpdate);
    }

    emitHighlightedIdentifierChange() {
        if (this.props.onHighlightedIdentifierChanged) {
            this.props.onHighlightedIdentifierChanged(this.state.highlightedIdentifier);
        }
    }

    onHighlightedRowIndexChanged(index) {
        const identifier = (index != -1 ? this.props.overviewUserdata[index].identifier : null);
        this.setState({
            highlightedIdentifier: identifier,
        }, this.emitHighlightedIdentifierChange);
    }

    getHighlightedRowIndex() {
        if (this.state.highlightedIdentifier) {
            return this.props.overviewUserdata.findIndex(item =>
                item.identifier == this.state.highlightedIdentifier);
        } else {
            return -1;
        }
    }

    render() {
        const tableProps = {
            columnMetadata: this.state.columnMetadata,
            rowData: this.props.overviewUserdata,
            onHighlightedRowIndexChanged: this.onHighlightedRowIndexChanged.bind(this),
            highlightedRowIndex: this.getHighlightedRowIndex()
        };
        return (
            <SimpleSortableTable ref='table' {...tableProps} />
        );
    }
}

TeamGraphUserPickerTable.displayName = 'TeamGraphUserPickerTable';

TeamGraphUserPickerTable.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    overviewUserdata: React.PropTypes.array.isRequired,
    onUserSelectionChanged: React.PropTypes.func,
    onHighlightedIdentifierChanged: React.PropTypes.func,
};