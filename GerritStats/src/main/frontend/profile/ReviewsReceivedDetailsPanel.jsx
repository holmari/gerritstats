import React from 'react';

import ClearFloat from '../common/ClearFloat';
import HorizontalCenterDiv from '../common/HorizontalCenterDiv';
import Panel from '../common/Panel';
import {formatPrintableDuration} from '../common/time/TimeUtils';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

import NumberPanel from './components/NumberPanel';

export default class ReviewsReceivedDetailsPanel extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const userdata = this.props.userdata;
        const selectedUsers = this.props.selectedUsers;

        return (
            <Panel title='Reviews received: details' size='half'>
                <HorizontalCenterDiv>
                    <NumberPanel
                        size='wide'
                        title='comments per commit'
                        tooltip='How many comments this user received per commit, on average.'
                        value={userdata.getReceivedCommentRatio(selectedUsers)} />
                    <ClearFloat />
                    <NumberPanel
                        size='xWide'
                        title='avg time in code review'
                        tooltip='The average time the commits of this user spent in code review, before they were merged.'
                        value={formatPrintableDuration(userdata.getAverageTimeInCodeReview())} />
                </HorizontalCenterDiv>
            </Panel>
        );
    }
}

ReviewsReceivedDetailsPanel.displayName = 'ReviewsReceivedDetailsPanel';

ReviewsReceivedDetailsPanel.propTypes = {
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
};