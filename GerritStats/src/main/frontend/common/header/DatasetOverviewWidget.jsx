import './DatasetOverviewWidget.scss';

import moment from 'moment';
import React from 'react';
import {Glyphicon} from 'react-bootstrap';

import SelectedUsers from '../model/SelectedUsers';

export default class DatasetOverviewWidget extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedUsers: this.props.selectedUsers
        };
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            selectedUsers: nextProps.selectedUsers
        });
    }

    renderBranches(branchList) {
        if (typeof branchList == 'string') {
            return branchList;
        } else if (!branchList || !branchList.length) {
            return '\u2013';
        } else if (branchList.length == 1) {
            return branchList[0];
        } else {
            return `${branchList.length} branches`;
        }
    }

    renderDate(date) {
        if (date) {
            return moment(date).format('YYYY-MM-DD');
        } else {
            return '\u2013';
        }
    }

    renderUserCount() {
        var userSelection = this.state.selectedUsers;
        if (userSelection) {
            return userSelection.getSelectedUserCount() + ' / ' + userSelection.getTotalUserCount();
        } else {
            return '\u2013';
        }
    }

    render() {
        return (
            <table className="filterBox">
                <tbody>
                    <tr>
                        <th><img src={require('./img/ic_branches.png')}></img>Branches:</th>
                        <td className="filterBoxValue">{this.renderBranches(this.props.datasetOverview['branchList'])}</td>
                    </tr>
                    <tr>
                        <th><Glyphicon glyph="calendar" />From:</th>
                        <td className="filterBoxValue">{this.renderDate(this.props.datasetOverview['fromDate'])}</td>
                    </tr>
                    <tr>
                        <th><Glyphicon glyph="calendar" />To:</th>
                        <td className="filterBoxValue">{this.renderDate(this.props.datasetOverview['toDate'])}</td>
                    </tr>
                    <tr>
                        <th><Glyphicon glyph="user" />Users in analysis:</th>
                        <td className="filterBoxValue">{this.renderUserCount()}</td>
                    </tr>
                </tbody>
            </table>
        );
    }
}

DatasetOverviewWidget.displayName = 'DatasetOverviewWidget';

DatasetOverviewWidget.defaultProps = {
    datasetOverview: {
        branchList: [],
        fromDate: 0,
        toDate: 0,
    },
    selectedUsers: null
};

DatasetOverviewWidget.propTypes = {
    datasetOverview: React.PropTypes.shape({
        fromDate: React.PropTypes.number.isRequired,
        toDate: React.PropTypes.number.isRequired,
    }),
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired
};