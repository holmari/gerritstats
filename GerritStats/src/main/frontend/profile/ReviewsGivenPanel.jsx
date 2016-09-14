import React from 'react';

import ClearFloat from '../common/ClearFloat';
import Panel from '../common/Panel';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

import NumberPanel from './components/NumberPanel';

export default class ReviewsGivenPanel extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const userdata = this.props.userdata;
        const selectedUsers = this.props.selectedUsers;

        return (
            <Panel title='Reviews given' size='third'>
                <NumberPanel
                    title='+1'
                    tooltip='Number of +1 reviews given to others by this user.'
                    value={userdata.getReviewsGivenForScore(1, selectedUsers)} />
                <NumberPanel
                    title='+2'
                    tooltip='Number of +2 reviews given to others by this user.'
                    value={userdata.getReviewsGivenForScore(2, selectedUsers)} />
                <ClearFloat />
                <NumberPanel
                    title='-1'
                    tooltip='Number of -1 reviews given to others by this user.'
                    value={userdata.getReviewsGivenForScore(-1, selectedUsers)} />
                <NumberPanel
                    title='-2'
                    tooltip='Number of -2 reviews given to others by this user.'
                    value={userdata.getReviewsGivenForScore(-2, selectedUsers)} />
                <ClearFloat />
                <NumberPanel
                    title='comments written'
                    tooltip='Number of review comments written to others by this user.'
                    value={userdata.getCommentsWrittenCount(selectedUsers)} />
                <NumberPanel
                    title='comments / review req.'
                    tooltip='How many comments this user wrote to others, per a review request. A higher number indicates that the user tends to respond often to review requests with written feedback.'
                    value={userdata.getReviewCommentRatio(selectedUsers)} />
            </Panel>
        );
    }
}

ReviewsGivenPanel.displayName = 'ReviewsGivenPanel';

ReviewsGivenPanel.propTypes = {
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
};