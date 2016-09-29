import './ProximityGraph.scss';

import * as d3 from 'd3';
import {fromJS} from 'immutable';

/**
 * Creates the identity graph node list.
 * @param whitelistedIdentifiers Optional argument containing the identifies that should be
 *                               included. Other identities are excluded.
 */
export function createIdentityGraph(userdata, whitelistedIdentifiers) {
    // deep copy the data; d3 modifies it

    const identityGraphNodes = fromJS(userdata).toJS();
    if (whitelistedIdentifiers) {
        for (let i = 0; i < identityGraphNodes.length; ++i) {
            if (!whitelistedIdentifiers.has(identityGraphNodes[i].identifier)) {
                identityGraphNodes.splice(i, 1);
                --i;
            }
        }
    }

    var identityGraph = {
        nodes: identityGraphNodes,
        links: []
    };

    for (let i = 0; i < identityGraphNodes.length; ++i) {
        const item = identityGraphNodes[i];

        for (let j = 0; j < item.myReviewerList.length; ++j) {
            var reviewerRecord = item.myReviewerList[j];
            if (whitelistedIdentifiers && !whitelistedIdentifiers.has(reviewerRecord.identity.identifier)) {
                continue;
            }

            if (reviewerRecord.reviewData.approvalCount != 0) {
                identityGraph.links.push({
                    source: identityGraphNodes[i],
                    target: identityGraphNodes[getIndexOfIdentity(reviewerRecord.identity, identityGraphNodes)],
                    value: reviewerRecord.reviewData.approvalCount
                });
            }
        }
    }
    return identityGraph;
}

function isVarDefined(variable) {
    return typeof variable !== 'undefined';
}

function safeIncrement(initialValue, toAdd) {
    return !isVarDefined(initialValue) ? toAdd : initialValue + toAdd;
}

function getIndexOfIdentity(identity, userData) {
    for (let i = 0; i < userData.length; ++i) {
        var user = userData[i];
        if ((user.identity.email !== undefined && user.identity.email == identity.email)
         || (user.identity.username !== undefined && user.identity.username == identity.username)) {
            return i;
        }
    }
    return -1;
}

function getObjectValues(iterable) {
    return Object.keys(iterable).map((key) => iterable[key]);
}

function numberComparator(left, right) {
    return left - right;
}

function filterObjectArray(objectList, key) {
    return objectList.map(function(currentValue) {
        return currentValue[key];
    }, []);
}

function getMaxValueFromArray(list) {
    return list.reduce(function(previousValue, currentValue) {
        return Math.max(previousValue, currentValue);
    }, -1);
}

function getMedianValueFromArrayExcludingZeroes(list) {
    var sortedList = list.slice().filter(function(currentValue) {
        return currentValue !== undefined;
    }).sort(numberComparator);

    var lastZero = sortedList.lastIndexOf(0);
    if (lastZero == -1) {
        lastZero = 0;
    }
    return sortedList[lastZero + Math.floor((sortedList.length - lastZero) / 2)];
}

/**
 * Creates a force-directed graph that illustrates team dynamics
 * and how many cross-reviews are done.
 */
export class ProximityGraph {

    constructor(objectSelector, config) {
        this.width = config.width || 1200;
        this.height = config.height || 800;

        this.lowConnectionColors = [
            '#e6550d', // less connections
            '#fd8d3c',
            '#fdae6b',
            '#fdd0a2', // more connections
        ];

        this.highConnectionColors = [
            '#c7e9c0', // less connections
            '#a1d99b',
            '#74c476',
            '#31a354', // more connections
        ];

        this.objectSelector = objectSelector;

        /** Filter out all the connections that are below the given threshold */
        this.relativeLinkValueThreshold = config.relativeLinkValueThreshold || 0.1;

        /** If set, this item will be locked in to center of the view. */
        this.centeredIdentifier = config.centeredIdentifier || null;
        this.centeredItemRadius = config.centeredItemRadius || 15;

        this.drawCrosshair = config.drawCrosshair || false;
        this.crosshairMargin = config.crosshairMargin || 35;

        this.highlightSelection = config.highlightSelection || false;
        this.defaultItemOpacity = config.defaultItemOpacity || 1.0;

        /** How far the nodes end up being from each other. Smaller (negative) values will result in further distance. */
        this.charge = config.charge || -450;
        this.linkDistance = config.linkDistance || 45;

        // Allows listening to highlight changes caused by user interaction.
        this.selectionChangedListener = null;
        /** Identifier of selected item (hovered or otherwise), or null if nothing is selected. */
        this.selectedIdentifier = null;

        this.svg = d3.select(this.objectSelector).append('svg')
            .attr('width', this.width)
            .attr('height', this.height);

        this.forceLayout = d3.layout.force()
            .size([this.width, this.height])
            .charge(this.charge)
            .linkDistance(this.linkDistance);
    }

    setSourceData(newIdentityGraph, newSelectedUsers) {
        this.identityGraph = newIdentityGraph;
        this.selectedUsers = newSelectedUsers;

        // TODO: don't just re-render, but add/remove the delta to the graph
        this.render();
    }

    setSelectedIdentifier(userIdentifier) {
        this.selectedIdentifier = userIdentifier;

        if (this.selectedIdentifier) {
            this.svg
                .selectAll('circle.proximityGraphNode').classed('selected', false);

            this.svg
                .selectAll('circle.proximityGraphNode')
                .filter(function(d) {
                    return d.identity.identifier === userIdentifier;
                }).classed('selected', true);
        } else {
            this.svg
                .selectAll('circle.proximityGraphNode').classed('selected', false);
        }
    }

    // FIXME this render method does not support incremental updates
    render() {
        this.svg.html('');

        const that = this;
        const maxLinkValue = this._getMaxLinkValue(this.identityGraph);

        var nodeData = this._filterNodes(this.identityGraph.nodes);
        var filteredLinks = this._filterLinks(this.identityGraph.nodes,
                                             this.identityGraph.links, maxLinkValue, this.relativeLinkValueThreshold);
        this.findFromNodeData = function(identifier) {
            return nodeData.find(function(node) {
                return node.identifier == identifier;
            });
        };

        var links = filteredLinks.map(function(currentLink) {
            return {
                source: this.findFromNodeData(currentLink.source.identifier),
                target: this.findFromNodeData(currentLink.target.identifier),
                value: currentLink.value
            };
        }, this);

        const connectionsPerIdentifier = this._createConnectionsPerIdentifierTable(links);
        const connectionCounts = getObjectValues(connectionsPerIdentifier);

        var maxConnectionCount = getMaxValueFromArray(connectionCounts);
        var medianConnectionCount = getMedianValueFromArrayExcludingZeroes(connectionCounts);

        var commitList = filterObjectArray(nodeData, 'commitCount').sort(numberComparator);
        var medianCommitCount = getMedianValueFromArrayExcludingZeroes(commitList);

        var centeredNodeData = null;
        if (this.centeredIdentifier) {
            for (let i = 0; i < nodeData.length; ++i) {
                if (nodeData[i].identifier == this.centeredIdentifier) {
                    centeredNodeData = nodeData[i];
                    break;
                }
            }
        }
        if (centeredNodeData) {
            centeredNodeData.fixed = true;
            centeredNodeData.x = this.width / 2;
            centeredNodeData.y = this.height / 2;
        }

        if (this.drawCrosshair) {
            this.svg.append('line')
                .attr('x1', this.crosshairMargin)
                .attr('y1', this.height / 2)
                .attr('x2', this.width - this.crosshairMargin)
                .attr('y2', this.height / 2)
                .attr('stroke-width', 0.5)
                .attr('stroke', '#dddddd')
                .attr('fill-opacity', 0.5);
            this.svg.append('line')
                .attr('x1', this.width / 2)
                .attr('y1', this.crosshairMargin)
                .attr('x2', this.width / 2)
                .attr('y2', this.height - this.crosshairMargin)
                .attr('stroke-width', 0.5)
                .attr('stroke', '#dddddd')
                .attr('fill-opacity', 0.5);
        }

        this.forceLayout
            .nodes(nodeData)
            .links(links)
            .linkStrength(function(d) {
                return 0.1 + (d.value / maxLinkValue);
            })
            .start();

        var link = this.svg.selectAll('.link')
            .data(links)
            .enter().append('line')
            .attr('class', 'proximityGraphLink')
            .style('stroke-width', function(d) { return 10 * (d.value / maxLinkValue); });

        var nodes = this.svg.selectAll('.node')
            .data(nodeData);

        nodes.enter()
            .append('circle')
                .attr('class', 'proximityGraphNode')
                .attr('r', function(d) {
                    return that._getNodeRadius(d, medianCommitCount);
                })
                .style('fill', function(d) {
                    var connectionCount = that._getConnectionCount(d.index, links);
                    var relativeConnectionCount = connectionCount / medianConnectionCount;
                    if (relativeConnectionCount >= 1) {
                        relativeConnectionCount = connectionCount / maxConnectionCount;
                        return that.mapConnectionsToColor(relativeConnectionCount, that.highConnectionColors);
                    } else {
                        return that.mapConnectionsToColor(relativeConnectionCount, that.lowConnectionColors);
                    }
                })
                .on('mouseover', function(d) {
                    that._updateSelection(d.identity.identifier);
                    if (that.highlightSelection) {
                        d3.select(this).classed('selected', true);
                    }
                })
                .on('mouseout', function() {
                    that._updateSelection(null);
                    d3.select(this).classed('selected', false);
                })
                .call(this.forceLayout.drag);

        nodes
            .classed('selected', function(d) {
                return d.identity.identifier === that.selectedIdentifier;
            })
            .attr('fill-opacity', function(d) {
                if (that.highlightSelection) {
                    return d.identity.identifier === that.selectedIdentifier ? 1 : that.defaultItemOpacity;
                } else {
                    return that.defaultItemOpacity;
                }
            });

        nodes.append('title')
            .attr('pointer-events', 'none')
            .text(function(d) { return d.identity.name; });

        this.forceLayout.on('tick', function() {
            link.attr('x1', function(d) { return d.source.x; })
                .attr('y1', function(d) { return d.source.y; })
                .attr('x2', function(d) { return d.target.x; })
                .attr('y2', function(d) { return d.target.y; });

            nodes.attr('cx', function(d) { return d.x; })
                 .attr('cy', function(d) { return d.y; });
        });
    }

    _getNodeRadius(nodeData, medianCommitCount) {
        if (medianCommitCount > 0) {
            return 3 + 2 * Math.sqrt(nodeData.commitCount / medianCommitCount);
        } else {
            return 3;
        }
    }

    _updateSelection(newSelection) {
        this.selectedIdentifier = newSelection;
        if (this.selectionChangedListener) {
            this.selectionChangedListener(this.selectedIdentifier);
        }
    }

    _getMaxLinkValue(graph) {
        var that = this;
        return graph.links.reduce(function(previousValue, currentLink) {
            if (that.isLinkSelected(graph.nodes, currentLink)) {
                return Math.max(previousValue, currentLink.value);
            } else {
                return previousValue;
            }
        }, -1);
    }

    _filterNodes(nodeData) {
        var that = this;
        return nodeData.filter(function(node) {
            return that.isNodeSelected(node.identifier);
        }, []);
    }

    isNodeSelected(identifier) {
        return this.selectedUsers.isUserSelected(identifier);
    }

    isLinkSelected(nodes, link) {
        return this.isNodeSelected(link.source.identifier)
            && this.isNodeSelected(link.target.identifier);
    }

    /**
     * Filter out the links that are below the given relative percentage, between [0..1].
     * For example, if relativeThreshold is 0.1, all links that have under 10% of the maximum
     * interaction are filtered out.
     */
    _filterLinks(nodes, links, maxLinkValue, relativeThreshold) {
        var that = this;
        var sourceLinks = fromJS(links).toJS();
        return sourceLinks.filter(function(currentLink) {
            return (currentLink.value / maxLinkValue) >= relativeThreshold
                && that.isLinkSelected(nodes, currentLink);
        }, []);
    }

    _getConnectionCount(nodeIndex, links) {
        var connectionCount = 0;
        for (let i = 0; i < links.length; ++i) {
            if (links[i].source.index == nodeIndex || links[i].target.index == nodeIndex) {
                connectionCount += links[i].value;
            }
        }
        return connectionCount;
    }

    _createConnectionsPerIdentifierTable(links) {
        var connectionsPerIdentifier = {};
        for (let i = 0; i < links.length; ++i) {
            var connectionCount = links[i].value;
            var sourceConnections = connectionsPerIdentifier[links[i].source.identifier];
            var targetConnections = connectionsPerIdentifier[links[i].target.identifier];
            connectionsPerIdentifier[links[i].source.identifier] = safeIncrement(sourceConnections, connectionCount);
            connectionsPerIdentifier[links[i].target.identifier] = safeIncrement(targetConnections, connectionCount);
        }
        return connectionsPerIdentifier;
    }

    mapConnectionsToColor(relativeConnectionCount, colors) {
        const colorIndex = Math.min(colors.length - 1, Math.floor(relativeConnectionCount * colors.length));
        return colors[colorIndex];
    }
}