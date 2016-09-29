import '../style/tables.scss';

import React from 'react';
import {Table, Thead, Th, Td, Tr} from 'reactable';
import Reactable from 'reactable';

import Panel from '../common/Panel';
import GerritUserdata from '../common/model/GerritUserdata';
import SelectedUsers from '../common/model/SelectedUsers';

export default class ProjectsContributedToPanel extends React.Component {
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

    makeTableSortRules() {
        return [
            { column: 'name', sortFunction: Reactable.Sort.CaseInsensitive },
            { column: 'commits', sortFunction: Reactable.Sort.NumericInteger },
            { column: 'commentsWritten', sortFunction: Reactable.Sort.NumericInteger },
        ];
    }

    renderData(perProjectData) {
        return perProjectData.map(function(project) {
            return (
                <Tr key={project.name}>
                    <Td column='name' value={project.name}><a href={project.url}>{project.name}</a></Td>
                    <Td column='commits' value={project.commitCount}>{project.commitCount}</Td>
                    <Td column='commentsWritten' value={project.commentsWritten}>{project.commentsWritten}</Td>
                </Tr>
            );
        });
    }

    renderContent() {
        const perProjectData = this.props.userdata.getPerProjectData(this.state.selectedUsers);
        return (
            <Table className='table table-striped' sortable={this.makeTableSortRules()}>
                <Thead>
                    <Th column='name' title='Project'>Project</Th>
                    <Th column='commits' title='# of commits'>Commits</Th>
                    <Th column='commentsWritten' title='# of comments written'># of comments<br/>written</Th>
                </Thead>
                {this.renderData(perProjectData)}
            </Table>
        );
    }

    render() {
        return (
            <Panel title='Projects contributed to' size='full'>
                {this.renderContent()}
            </Panel>
        );
    }
}

ProjectsContributedToPanel.displayName = 'ProjectsContributedToPanel';

ProjectsContributedToPanel.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
};