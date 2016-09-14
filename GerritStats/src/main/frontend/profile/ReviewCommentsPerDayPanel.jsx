import React from 'react';

import Panel from '../common/Panel';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

import ReviewCommentsPerDayView from './charts/ReviewCommentsPerDayView';

export default class ReviewCommentsPerDayPanel extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <Panel title='Review comments per day' size='full'>
                <p>Comments written by this user per day. The timescale includes weekends, while the average value excludes them.</p>
                <ReviewCommentsPerDayView {...this.props} />
            </Panel>
        );
    }
}

ReviewCommentsPerDayPanel.displayName = 'ReviewCommentsPerDayPanel';

ReviewCommentsPerDayPanel.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
};