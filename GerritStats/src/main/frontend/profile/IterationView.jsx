import './IterationView.scss';

import React from 'react';
import {Td} from 'reactable';
import Reactable from 'reactable';

import Panel from '../common/Panel';
import SimpleSortableTable from '../common/SimpleSortableTable';
import GerritUserdata from '../common/model/GerritUserdata';
import {getPatchSetCountForKind} from '../common/model/GerritUserdata';
import {HIGH_PATCH_SET_COUNT_THRESHOLD} from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

import IterationTimelineChartView from './charts/IterationTimelineChartView';

export default class IterationView extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedUsers: this.props.selectedUsers,
            highlightedIdentifier: null,
        };
    }

    getColumnMetadata() {
        return {
            'title': {
                header: 'Title',
                description: 'The title of the commit.',
                cell: (data, index) => (
                    <Td key={'title_' + index} column='title' value={data.title}><a href={data.url}>{data.title}</a></Td>
                ),
                sortFunction: Reactable.Sort.CaseInsensitive,
            },
            'iterations': {
                header: 'Iterations',
                description: 'How many iterative code reviews were done on this commit. '
                           + 'An iterative code review has an unusually amount of back-and-forth in the review process,'
                           + 'and can be time-consuming for both the author and the reviewer(s).',
                cell: (data, index) => (
                    <Td key={'iter_' + index} column='iterations'>{data.iterationCount}</Td>
                ),
                sortFunction: Reactable.Sort.NumericInteger,
            },
        };
    }

    makeDataForTable() {
        const exceedingCommits = this.props.userdata.getCommitsWithHighPatchSetCount();

        return exceedingCommits.map(function(commit, index) {
            return {
                url: commit.url,
                title: commit.subject,
                iterationCount: getPatchSetCountForKind(commit, 'REWORK'),
                createdOnDate: commit.createdOnDate,
                index: index,
            };
        }.bind(this));
    }

    onHighlightedRowIndexChanged(selectedIndex) {
        this.setState({
            highlightedRowIndex: selectedIndex,
        });
    }

    render() {
        const exceedingCommitData = this.makeDataForTable();
        const tableProps = {
            columnMetadata: this.getColumnMetadata(),
            rowData: exceedingCommitData,
            onHighlightedRowIndexChanged: this.onHighlightedRowIndexChanged.bind(this),
            highlightedRowIndex: this.state.highlightedRowIndex
        };
        const chartProps = {
            exceedingCommitData: exceedingCommitData,
            onGraphSelectionChanged: this.onHighlightedRowIndexChanged.bind(this),
            highlightedRowIndex: this.state.highlightedRowIndex,
        };

        return (
            <div>
                <Panel title='Iteration' size='third'>
                    <p>Commits that were reworked
                       over <span className='highPatchSetCountThreshold'>{HIGH_PATCH_SET_COUNT_THRESHOLD}</span> times
                       after the initial review, and then submitted:
                    </p>
                    <SimpleSortableTable {...tableProps} />
                </Panel>
                <Panel title='Iteration timeline' size='twoThirds'>
                    <IterationTimelineChartView {...chartProps} />
                </Panel>
            </div>
        );
    }
}

IterationView.displayName = 'IterationView';

IterationView.defaultProps = {
    highlightedRowIndex: -1,
};

IterationView.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
};