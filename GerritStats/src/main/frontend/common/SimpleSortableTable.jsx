import React from 'react';
import {Table, Thead, Th, Tr} from 'reactable';

/**
 * Renders a sortable <Table> with reactable, using the following props to render data:
 *   - columnMetadata, a map describing each column and its cells are rendered:
 *     For example, an item in the map can be:
        'fullName': {
             sortFunction: Reactable.Sort.CaseInsensitive,
             description: 'The name of the user, as shown in Gerrit.',
             header: 'Name',
             cell: (record, index) => (record.name),
        },
 *
 *   - rowData, which is an array that will be mapped by cell rendering function to rendered content.
 *   - rowRenderer (optional), which allows overriding the row rendering behavior.
 */
export default class SimpleSortableTable extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            highlightedRowIndex: this.props.highlightedRowIndex,
        };
    }

    componentWillReceiveProps(nextProps) {
        if (this.state.highlightedRowIndex != nextProps.highlightedRowIndex) {
            this.setState({
                highlightedRowIndex: nextProps.highlightedRowIndex,
            });
        }
    }

    makeTableSortRules() {
        var sortRules = [];
        Object.keys(this.props.columnMetadata).forEach(function(colName) {
            const metadata = this.props.columnMetadata[colName];
            if (metadata.sortFunction) {
                sortRules.push({
                    column: colName,
                    sortFunction: this.props.columnMetadata[colName].sortFunction
                });
            } else {
                sortRules.push(colName);
            }
        }.bind(this));
        return sortRules;
    }

    renderTableHead() {
        var thElements = [];
        Object.keys(this.props.columnMetadata).forEach(function(columnName) {
            const metadata = this.props.columnMetadata[columnName];
            const title = metadata.title || '';
            const content = (typeof metadata.header === 'function')
                          ? metadata.header() : metadata.header;
            thElements.push(
                <Th key={columnName} column={columnName} title={title}>{content}</Th>
            );
        }.bind(this));
        return (
            <Thead>
                {thElements}
            </Thead>
        );
    }

    emitHighlightedRowIndexChange() {
        if (this.props.onHighlightedRowIndexChanged) {
            this.props.onHighlightedRowIndexChanged(this.state.highlightedRowIndex);
        }
    }

    onMouseEnterRow(identifier) {
        this.setState({
            highlightedRowIndex: identifier,
        }, this.emitHighlightedRowIndexChange);
    }

    onMouseLeaveRow() {
        this.setState({
            highlightedRowIndex: -1,
        }, this.emitHighlightedRowIndexChange);
    }

    renderRows() {
        const rowRenderer = this.props.rowRenderer || this.renderRow.bind(this);
        return this.props.rowData.map((row, i) => rowRenderer(i, row));
    }

    renderRow(index, rowData) {
        var rowCells = Object.keys(this.props.columnMetadata).map(function(columnName) {
            const cellValueOrFunc = this.props.columnMetadata[columnName].cell;
            return (typeof cellValueOrFunc === 'function') ? cellValueOrFunc(rowData, index) : cellValueOrFunc;
        }.bind(this));

        const isHighlighted = (this.state.highlightedRowIndex == index);

        return (
            <Tr key={'r_' + index}
                onMouseEnter={() => this.onMouseEnterRow(index)}
                onMouseLeave={() => this.onMouseLeaveRow()}
                className={isHighlighted ? 'highlightedRow' : ''}>
                {rowCells}
            </Tr>
        );
    }

    render() {
        return (
            <Table className='table table-striped' sortable={this.makeTableSortRules()}>
                {this.renderTableHead()}
                {this.renderRows()}
            </Table>
        );
    }
}

SimpleSortableTable.defaultProps = {
    highlightedRowIndex: -1,
    rowData: [],
};

SimpleSortableTable.propTypes = {
    columnMetadata: React.PropTypes.object.isRequired,
    rowRenderer: React.PropTypes.func,
    rowData: React.PropTypes.array,
    onHighlightedRowIndexChanged: React.PropTypes.func,
    highlightedRowIndex: React.PropTypes.number,
};