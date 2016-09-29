import moment from 'moment';
import React from 'react';

import ClearFloat from '../common/ClearFloat';
import HorizontalCenterDiv from '../common/HorizontalCenterDiv';
import Panel from '../common/Panel';
import NumberPanel from './components/NumberPanel';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

export default class TimelinePanel extends React.Component {
    constructor(props) {
        super(props);
    }

    getFormattedDate(dateUnixEpoch) {
        const momentDate = moment(dateUnixEpoch);
        return (momentDate.isValid() && dateUnixEpoch != 0)
             ? momentDate.format('YYYY-MM-DD') : '\u2013';
    }

    render() {
        const userdata = this.props.userdata;

        const firstActiveDate = userdata.getFirstActiveDate();
        const lastActiveDate = userdata.getLastActiveDate();
        const totalDayCount = moment(firstActiveDate).isValid() && moment(lastActiveDate).isValid()
                          ? Math.round((lastActiveDate - firstActiveDate) / (1000 * 60 * 60 * 24))
                          : '\u2013';
        const activeDayCount = userdata.getActiveDayCount();

        return (
            <Panel title='Timeline' size='half'>
                <HorizontalCenterDiv>
                    <NumberPanel
                        title='first seen'
                        size='wide'
                        tooltip='The date when activity was first seen from this user.'
                        value={this.getFormattedDate(firstActiveDate)} />
                    <NumberPanel
                        title='last seen'
                        size='wide'
                        tooltip='The date when activity was last seen from this user.'
                        value={this.getFormattedDate(lastActiveDate)} />
                    <ClearFloat />
                    <NumberPanel
                        title='total days'
                        tooltip='The number of days between the last and first seen dates.'
                        value={totalDayCount} />
                    <NumberPanel
                        title='active days'
                        tooltip='The number of days the user has been active.'
                        value={activeDayCount} />
                </HorizontalCenterDiv>
            </Panel>
        );
    }
}

TimelinePanel.displayName = 'TimelinePanel';

TimelinePanel.propTypes = {
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
};