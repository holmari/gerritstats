import './ReviewCommentsPanel.scss';

import moment from 'moment';
import React from 'react';

import Panel from '../common/Panel';
import GerritUserdata from '../common/model/GerritUserdata';
import {getShortPrintableName} from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

/**
 * Creates a code comment URL.
 *
 * Commit url: https://gerrit.domain.com/29251
 * Output: https://gerrit.domain.com/#/c/29251/13/src/main/java/HelloWorld.java
 */
function getGerritUrlForComment(commit, patchSet, comment) {
    var url = commit.url;
    var baseUrl = url.substring(0, url.lastIndexOf('/'));
    return baseUrl + '/#/c/' + commit.commitNumber + '/' + patchSet.number + '/' + comment.file;
}

function escapeHtml(text) {
    return text
        .replace(/&/g, '\u0026')
        .replace(/</g, '\u003c')
        .replace(/>/g, '\u003e')
        .replace(/"/g, '\u0022')
        .replace(/'/g, '\u0027');
}

export default class ReviewCommentsPanel extends React.Component {
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

    renderComment(commit, patchSet, comment, index) {
        const urlForComment = getGerritUrlForComment(commit, patchSet, comment);
        const altText = comment.file + ':' + comment.line;
        const key = urlForComment + '_' + index;
        return (
            <li key={key}><a href={urlForComment} alt={altText}>{escapeHtml(comment.message)}</a></li>
        );
    }

    renderData() {
        const userdata = this.props.userdata;
        const commits = userdata.getCommitsWithWrittenCommentsSortedByDate();
        const renderedContent = [];

        for (let i = 0; i < commits.length; ++i) {
            const commitAndComments = commits[i];
            const commit = commitAndComments.commit;

            const renderedComments = [];
            commit.patchSets.forEach(function(patchSet) {
                // ignore self-reviews and replies
                if (patchSet.author['email'] == userdata.getEmail()
                || !this.state.selectedUsers.isUserSelected(patchSet.author['identifier'])) {
                    return;
                }

                const comments = patchSet.comments;
                var j = 0;
                comments.forEach(function(comment) {
                    if (comment.reviewer['email'] != userdata.getEmail()) {
                        return;
                    }
                    renderedComments.push(this.renderComment(commit, patchSet, comment, j));
                    ++j;
                }.bind(this));
            }.bind(this));

            if (!renderedComments.length) {
                continue;
            }
            const date = moment(commit.createdOnDate);
            renderedContent.push(
                <div key={commit.url}>
                    <h4><a href={commit.url}>{commit.subject} (# {commit.commitNumber})</a></h4>
                    <h5>by {getShortPrintableName(commit.owner)}, created on {date.format('YYYY-MM-DD')}</h5>
                    <ul>{renderedComments}</ul>
                </div>
            );
        }
        return renderedContent;
    }

    render() {
        return (
            <Panel title='Review comments' size='full'>
                <div className='reviewComments'>
                    {this.renderData()}
                </div>
            </Panel>
        );
    }
}

ReviewCommentsPanel.displayName = 'ReviewCommentsPanel';

ReviewCommentsPanel.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
};