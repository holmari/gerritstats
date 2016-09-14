import React from 'react';

import ClearFloat from '../common/ClearFloat';
import Panel from '../common/Panel';
import NumberPanel from './components/NumberPanel';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

export default class ReviewsReceivedPanel extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const userdata = this.props.userdata;
        const selectedUsers = this.props.selectedUsers;

        return (
            <Panel title='Reviews received' size='third'>
                <NumberPanel
                    title='+1'
                    tooltip='Number of +1 reviews received by this user.'
                    value={userdata.getReviewsReceivedForScore(1, selectedUsers)} />
                <NumberPanel
                    title='+2'
                    tooltip='Number of +2 reviews received by this user.'
                    value={userdata.getReviewsReceivedForScore(2, selectedUsers)} />
                <ClearFloat />
                <NumberPanel
                    title='-1'
                    tooltip='Number of -1 reviews received by this user.'
                    value={userdata.getReviewsReceivedForScore(-1, selectedUsers)} />
                <NumberPanel
                    title='-2'
                    tooltip='Number of -2 reviews received by this user.'
                    value={userdata.getReviewsReceivedForScore(-2, selectedUsers)} />
                <ClearFloat />
                <NumberPanel
                    title='comments received'
                    tooltip='Number of comments received by this user.'
                    value={userdata.getAllCommentsReceived(selectedUsers)} />
                <NumberPanel
                    title='self-reviews'
                    tooltip="Number of self-reviewed commits: commits that were +2'd by the author and then submitted, with no reviews by others in any of the patch sets. In standard Gerrit usage, this number should be very low, and a high number for a particular is often a red flag."
                    value={userdata.getSelfReviewedCommitCount()} />
            </Panel>
        );
    }
}

ReviewsReceivedPanel.displayName = 'ReviewsReceivedPanel';

ReviewsReceivedPanel.propTypes = {
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
};