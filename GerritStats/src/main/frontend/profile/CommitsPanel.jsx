import React from 'react';

import ClearFloat from '../common/ClearFloat';
import HorizontalCenterDiv from '../common/HorizontalCenterDiv';
import Panel from '../common/Panel';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

import NumberPanel from './components/NumberPanel';

export default class CommitsPanel extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const userdata = this.props.userdata;
        const selectedUsers = this.props.selectedUsers;
        return (
            <Panel title='Commits' size='third'>
                <NumberPanel
                    title='total commits'
                    tooltip='Number of total commits for the user.'
                    value={userdata.getCommitCount()} />
                <NumberPanel
                    title='added as reviewer'
                    tooltip='Number of times that the user was added as a reviewer.'
                    value={userdata.getAddedAsReviewerToCount(selectedUsers)} />
                <ClearFloat />
                <NumberPanel
                    title='max patch set count'
                    tooltip="The highest number of patch sets of all the user's commits. See 'Iteration' for details."
                    value={userdata.getMaxPatchSetCount()} />
                <NumberPanel
                    title='abandoned'
                    tooltip='Number of abandoned commits, which were never merged to a branch.'
                    value={userdata.getAbandonedCommitCount()} />
                <ClearFloat />
                <HorizontalCenterDiv>
                    <NumberPanel
                        title='in review'
                        tooltip='Number of commits currently in review by this user.'
                        value={userdata.getInReviewCommitCount()} />
                </HorizontalCenterDiv>
            </Panel>
        );
    }
}

CommitsPanel.displayName = 'CommitsPanel';

CommitsPanel.propTypes = {
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
};