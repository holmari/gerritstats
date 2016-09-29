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

    renderCommitCounts(commitTable, year) {
        return this.getElementsPerMonth(commitTable, year, 'getPrintableMonthlyItemCount');
    }

    renderCommentCounts(commentTable, year) {
        return this.getElementsPerMonth(commentTable, year, 'getPrintableMonthlyItemCount');
    }

    renderCommitsMoMChange(commitTable, year) {
        return this.getElementsPerMonth(commitTable, year, 'getDisplayableMonthOnMonthChange');
    }

    renderCommitsQoQChange(commitTable, year) {
        return this.getElementsPerMonth(commitTable, year, 'getDisplayableQuarterOnQuarterChange');
    }

    renderCommentsMoMChange(commentTable, year) {
        return this.getElementsPerMonth(commentTable, year, 'getDisplayableMonthOnMonthChange');
    }

    renderCommentsQoQChange(commentTable, year) {
        return this.getElementsPerMonth(commentTable, year, 'getDisplayableQuarterOnQuarterChange');
    }

    renderPerYearStats() {
        const commitTable = this.props.userdata.datedCommitTable;
        const years = this.props.userdata.datedCommitTable.getActiveYears();

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
                    {this.renderCommitCounts(commitTable, year)}
                </tr>
            );
            elements.push(
                <tr key={keyPrefix + '_3'} className='commentsSection'>
                    <th>Comments</th>
                    {this.renderCommentCounts(commitTable, year)}
                </tr>
            );
            elements.push(
                <tr key={keyPrefix + '_4'} className='commitsMoMSection'>
                    <th>Commits MoM %</th>
                    {this.renderCommitsMoMChange(commitTable, year)}
                </tr>
            );
            elements.push(
                <tr key={keyPrefix + '_5'} className='commitsQoQSection'>
                    <th>Commits QoQ %</th>
                    {this.renderCommitsQoQChange(commitTable, year)}
                </tr>
            );
            elements.push(
                <tr key={keyPrefix + '_6'} className='commentsMoMSection'>
                    <th>Comments MoM %</th>
                    {this.renderCommentsMoMChange(commitTable, year)}
                </tr>
            );
            elements.push(
                <tr key={keyPrefix + '_7'} className='commentsQoQSection'>
                    <th>Comments QoQ %</th>
                    {this.renderCommentsQoQChange(commitTable, year)}
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