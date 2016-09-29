import './IterationTimelineChartView.scss';

import React from 'react';

import D3BaseComponent from '../../common/charts/D3BaseComponent';

import IterationTimelineChart from './IterationTimelineChart';

export default class IterationTimelineChartView extends D3BaseComponent {
    constructor(props) {
        super(props);
        this.state = {
            highlightedRowIndex: -1,
        };
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.highlightedRowIndex != this.state.highlightedRowIndex) {
            this.setState({
                highlightedRowIndex: nextProps.highlightedRowIndex,
            });
            if (this.state.chart) {
                this.state.chart.setSelectedCommitIndex(nextProps.highlightedRowIndex);
            }
        }
    }

    onGraphSelectionChanged(selectedCommitUrl, previousSelection) {
        if (this.props.onGraphSelectionChanged) {
            this.props.onGraphSelectionChanged(selectedCommitUrl, previousSelection);
        }
    }

    componentDidMount() {
        const chart = new IterationTimelineChart(this.refs.graphContent, this.props.exceedingCommitData, {
            height: 550,
            selectionChangedListener: this.onGraphSelectionChanged.bind(this),
        });
        chart.render();
        this.setState({
            chart: chart,
        });
    }
}

IterationTimelineChartView.displayName = 'IterationTimelineChartView';

IterationTimelineChartView.propTypes = {
    exceedingCommitData: React.PropTypes.array.isRequired,
    onGraphSelectionChanged: React.PropTypes.func,
    highlightedRowIndex: React.PropTypes.number,
};