import './CumulativeChartView.scss';

import React from 'react';

import D3BaseComponent from '../../common/charts/D3BaseComponent';

import CumulativeChart from './CumulativeChart';

import GerritUserdata from '../../common/model/GerritUserdata';
import SelectedUsers from '../../common/model/SelectedUsers';

export default class CumulativeChartView extends D3BaseComponent {
    constructor(props) {
        super(props);
    }

    componentDidMount() {
        new CumulativeChart(this.refs.graphContent, this.props.userdata, {
            height: 550,
        });
    }
}

CumulativeChartView.displayName = 'CumulativeChartView';

CumulativeChartView.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
};