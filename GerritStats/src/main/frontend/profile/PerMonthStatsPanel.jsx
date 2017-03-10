import './PerMonthStatsPanel.scss';

import React from 'react';

import Panel from '../common/Panel';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

export default class PerMonthStatsPanel extends React.Component {
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
    }

    getElementsPerMonth(dataTable, year, valueFunction) {
        const elements = [];
        for (let month = 1; month <= 12; ++month) {
            const value = dataTable[valueFunction](year, month);
            elements.push(
                <td key={'yr_' + year + '_m_' + month + '_' + valueFunction}>{value}</td>
            );
        }
        return elements;
    }

    renderCounts(table, year) {
        return this.getElementsPerMonth(table, year, 'getPrintableMonthlyItemCount');
    }

    renderMoMChange(table, year) {
        return this.getElementsPerMonth(table, year, 'getDisplayableMonthOnMonthChange');
    }

    renderQoQChange(table, year) {
        return this.getElementsPerMonth(table, year, 'getDisplayableQuarterOnQuarterChange');
    }

    renderPerYearStats() {
        const commitTable = this.props.userdata.datedCommitTable;
        const commentTable = this.props.userdata.datedCommentTable;
        var years = commitTable.getActiveYears().concat(commentTable.getActiveYears());
        years.sort(function(l, r) {
            return (l > r) ? -1 : ((l < r) ? 1 : 0);
        });
        years = years.filter(function(item, pos, a) {
            return !pos || item != a[pos - 1];
        });

        const elements = [];
        years.forEach(function(year) {
            const keyPrefix = 'yr_' + year;
            elements.push(
                <tr key={keyPrefix + '_0'}>
                    <th colSpan='13' className='monthlyCommitYearTitle'>{year}</th>
                </tr>
            );
            elements.push(
                <tr key={keyPrefix + '_1'}>
                    <th></th>
                    <th>Jan</th>
                    <th>Feb</th>
                    <th>Mar</th>
                    <th>Apr</th>
                    <th>May</th>
                    <th>Jun</th>
                    <th>Jul</th>
                    <th>Aug</th>
                    <th>Sep</th>
                    <th>Oct</th>
                    <th>Nov</th>
                    <th>Dec</th>
                </tr>
            );
            elements.push(
                <tr key={keyPrefix + '_2'} className='commitsSection'>
                    <th>Commits</th>
                    {this.renderCounts(commitTable, year)}
                </tr>
            );
            elements.push(
                <tr key={keyPrefix + '_3'} className='commentsSection'>
                    <th>Comments</th>
                    {this.renderCounts(commentTable, year)}
                </tr>
            );
            elements.push(
                <tr key={keyPrefix + '_4'} className='commitsMoMSection'>
                    <th>Commits MoM %</th>
                    {this.renderMoMChange(commitTable, year)}
                </tr>
            );
            elements.push(
                <tr key={keyPrefix + '_5'} className='commitsQoQSection'>
                    <th>Commits QoQ %</th>
                    {this.renderQoQChange(commitTable, year)}
                </tr>
            );
            elements.push(
                <tr key={keyPrefix + '_6'} className='commentsMoMSection'>
                    <th>Comments MoM %</th>
                    {this.renderMoMChange(commentTable, year)}
                </tr>
            );
            elements.push(
                <tr key={keyPrefix + '_7'} className='commentsQoQSection'>
                    <th>Comments QoQ %</th>
                    {this.renderQoQChange(commentTable, year)}
                </tr>
            );
        }.bind(this));
        return elements;
    }

    render() {
        return (
            <Panel title='Per-month stats' size='full'>
                <table className='perMonthStats'>
                    <tbody>
                        {this.renderPerYearStats()}
                    </tbody>
                </table>
            </Panel>
        );
    }
}

PerMonthStatsPanel.displayName = 'PerMonthStatsPanel';

PerMonthStatsPanel.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
};