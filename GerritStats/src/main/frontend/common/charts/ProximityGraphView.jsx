import './ProximityGraphView.scss';

import React from 'react';

import SelectedUsers from '../model/SelectedUsers';

import D3BaseComponent from './D3BaseComponent';
import {createIdentityGraph, ProximityGraph} from './ProximityGraph';

export default class ProximityGraphView extends D3BaseComponent {

    constructor(props) {
        super(props);
        this.state = {
            selectedUsers: this.props.selectedUsers,
            highlightedIdentifier: this.props.highlightedIdentifier,
            identityGraph: this.props.identityGraph || createIdentityGraph(this.props.overviewUserdata),
            graph: null,
        };
    }

    componentWillReceiveProps(nextProps) {
        const usersWillChange = !nextProps.selectedUsers.equals(this.state.selectedUsers)
            || this.props.overviewUserdata.length != nextProps.overviewUserdata.length;
        const highlightWillChange = (this.state.highlightedIdentifier != nextProps.highlightedIdentifier);

        if (usersWillChange || highlightWillChange) {
            const identityGraph = this.props.identityGraph || createIdentityGraph(nextProps.overviewUserdata);
            if (this.state.proximityGraph) {
                if (usersWillChange) {
                    this.state.proximityGraph.setSourceData(identityGraph, nextProps.selectedUsers);
                }
                if (highlightWillChange) {
                    this.state.proximityGraph.setSelectedIdentifier(nextProps.highlightedIdentifier);
                }
            }

            this.setState({
                selectedUsers: nextProps.selectedUsers,
                highlightedIdentifier: nextProps.highlightedIdentifier,
                identityGraph: identityGraph,
            });
        }
    }

    componentDidMount() {
        const config = this.props.graphConfig ? this.props.graphConfig : {
            width: 790,
            height: 600,
            highlightSelection: true,
        };
        const graph = new ProximityGraph(this.refs.graphContent, config);
        graph.setSourceData(this.state.identityGraph, this.state.selectedUsers);
        graph.selectionChangedListener = this.onGraphSelectionChanged.bind(this);

        this.setState({
            proximityGraph: graph,
        });
    }

    onGraphSelectionChanged(selectedIdentifier) {
        if (this.props.onHighlightedIdentifierChanged) {
            this.props.onHighlightedIdentifierChanged(selectedIdentifier);
        }

        this.setState({
            selectedIdentifier: selectedIdentifier,
        });
    }

    emitUserSelectionUpdate() {
        if (this.props.onUserSelectionChanged) {
            this.props.onUserSelectionChanged(this.state.selectedUsers);
        }
    }

    onUserSelectionChanged(newSelectedUsers) {
        this.setState({
            selectedUsers: newSelectedUsers
        }, this.emitUserSelectionUpdate);
    }
}

ProximityGraphView.displayName = 'ProximityGraphView';

ProximityGraphView.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    overviewUserdata: React.PropTypes.array.isRequired,
    highlightedIdentifier: React.PropTypes.string,
    onHighlightedIdentifierChanged: React.PropTypes.func,
    identityGraph: React.PropTypes.object,
    graphConfig: React.PropTypes.object,
};