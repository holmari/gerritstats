import React from 'react';

import D3BaseComponent from '../../common/charts/D3BaseComponent';

import GerritUserdata from '../../common/model/GerritUserdata';
import SelectedUsers from '../../common/model/SelectedUsers';

import ReviewersAndApprovalsChart from './ReviewersAndApprovalsChart';

export default class ReviewersAndApprovalsChartView extends D3BaseComponent {
    constructor(props) {
        super(props);
        this.state = {
            selectedUsers: this.props.selectedUsers,
        };
    }

    componentWillReceiveProps(nextProps) {
        if (this.state.highlightedIdentifier != nextProps.highlightedIdentifier) {
            this.setState({
                highlightedIdentifier: nextProps.highlightedIdentifier,
            });
            if (this.state.chart) {
                this.state.chart.setSelectedItemByIdentifier(nextProps.highlightedIdentifier);
            }
        }
        if (!nextProps.selectedUsers.equals(this.state.selectedUsers)) {
            this.setState({
                selectedUsers: nextProps.selectedUsers,
            });
        }
    }

    onGraphSelectionChanged(selectedIdentifier, previousSelection) {
        if (this.props.onGraphSelectionChanged) {
            this.props.onGraphSelectionChanged(selectedIdentifier, previousSelection);
        }
        this.setState({
            highlightedIdentifier: selectedIdentifier,
        });
    }

    componentDidMount() {
        // FIXME: this will break (among with some other cases) if selection is changed while in profile page,
        // because this is called only once
        const reviewerData = this.props.userdata.getFilteredReviewerDataForOwnCommits(this.state.selectedUsers);
        const chart = new ReviewersAndApprovalsChart(this.refs.graphContent, reviewerData, {
            height: 520,
            selectionChangedListener: this.onGraphSelectionChanged.bind(this),
        });
        chart.render();

        this.setState({
            chart: chart,
        });
    }
}

ReviewersAndApprovalsChartView.displayName = 'ReviewersApprovalsChartView';

ReviewersAndApprovalsChartView.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
    onGraphSelectionChanged: React.PropTypes.func,
    highlightedIdentifier: React.PropTypes.string,
};