import '../style/tables.scss';

import React from 'react';

import Panel from '../common/Panel';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

import CumulativeChartView from './charts/CumulativeChartView';

export default class CumulativeStatisticsPanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedUsers: this.props.selectedUsers,
        };
    }

    componentWillReceiveProps(nextProps) {
        if (!nextProps.selectedUsers.equals(this.state.selectedUsers)) {
            this.setState({
                selectedUsers: nextProps.selectedUsers,
            });
        }
    }

    render() {
        return (
            <Panel title='Cumulative statistics' size='full'>
                <p>Written and received comments and commits by user, displayed cumulatively over time.</p>
                <CumulativeChartView {...this.props} />
            </Panel>
        );
    }
}

CumulativeStatisticsPanel.displayName = 'CumulativeStatisticsPanel';

CumulativeStatisticsPanel.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
};