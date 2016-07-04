/**
 * Creates the identity graph node list.
 * @param whitelistedIdentifiers Optional argument containing the identifies that should be
 *                               included. Other identities are excluded.
 */
function createIdentityGraph(userData, whitelistedIdentifiers) {
    // deep copy the data; d3 modifies it
    var identityGraphNodes = jQuery.extend(true, [], userData);
    if (whitelistedIdentifiers) {
        for (var i = 0; i < identityGraphNodes.length; ++i) {
            if (!whitelistedIdentifiers.has(identityGraphNodes[i].identifier)) {
                identityGraphNodes.splice(i, 1);
                --i;
            }
        }
    }

    var identityGraph = {
        "nodes": identityGraphNodes,
        "links": []
    }

    for (var i = 0; i < identityGraphNodes.length; ++i) {
        var item = identityGraphNodes[i];

        for (var j = 0; j < item.myReviewerList.length; ++j) {
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

function getIndexOfIdentity(identity, userData) {
    for (var i = 0; i < userData.length; ++i) {
        var user = userData[i];
        if ((user.identity.email !== undefined && user.identity.email == identity.email)
         || (user.identity.username !== undefined && user.identity.username == identity.username)) {
            return i;
        }
    }
    return -1;
}

/**
 * Creates a force-directed graph that illustrates team dynamics
 * and how many cross-reviews are done.
 */
function ProximityGraph(identityGraph, selectedUsers, objectSelector) {
    this.width = 1200;
    this.height = 800;

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

    // Source data
    this.identityGraph = identityGraph;

    /** Filter out all the connections that are below the given threshold */
    this.relativeLinkValueThreshold = 0.1;

    /** If set, this item will be locked in to center of the view. */
    this.centeredIdentifier = null;
    this.centeredItemRadius = 15;

    this.drawCrosshair = false;
    this.crosshairMargin = 35;

    // Allows listening to highlight changes caused by user interaction.
    this.selectionChangedListener = null;
    /** Identifier of selected item (hovered or otherwise), or null if nothing is selected. */
    this.selectedIdentifier = null;

    this.highlightSelection = false;
    this.defaultItemOpacity = 1.0;

    /** How far the nodes end up being from each other. Smaller (negative) values will result in further distance. */
    this.charge = -450;
    this.linkDistance = 45;
    // d3
    this.svg = null;

    this.create = function() {
        this.svg = d3.select(objectSelector).append('svg')
            .attr('width', this.width)
            .attr('height', this.height);

        this.forceLayout = d3.layout.force()
            .size([this.width, this.height])
            .charge(this.charge)
            .linkDistance(this.linkDistance);

        this.render();
    }

    this.updateSize = function() {
        this.width = $(objectSelector).width();
        this.height = $(objectSelector).height();
        this.svg.attr("width", this.width).attr("height", this.height);
        this.forceLayout.size([this.width, this.height]).resume();
    }

    this.getNodeRadius = function(nodeData, medianCommitCount) {
        return 3 + 2 * Math.sqrt(nodeData.commitCount / medianCommitCount);
    }

    this.setSelectedIdentifier = function(userIdentifier) {
        this.selectedIdentifier = userIdentifier;
        this.render();
    };

    this.updateSelection = function(newSelection) {
        var previousSelection = this.selectedIdentifier;
        this.selectedIdentifier = newSelection;
        if (this.selectionChangedListener) {
            this.selectionChangedListener(this.selectedIdentifier, previousSelection);
        }
    };

    this.render = function() {
        this.svg.html('');

        var that = this;
        var maxLinkValue = this.getMaxLinkValue(this.identityGraph);

        var nodeData = this.filterNodes(this.identityGraph.nodes);
        var filteredLinks = this.filterLinks(this.identityGraph.nodes,
                                             this.identityGraph.links, maxLinkValue, this.relativeLinkValueThreshold);
        this.findFromNodeData = function(identifier) {
            return nodeData.find(function(node) {
                return node.identifier == identifier;
            });
        };

        var links = filteredLinks.map(function(currentLink, index, array) {
            return {
                source: this.findFromNodeData(currentLink.source.identifier),
                target: this.findFromNodeData(currentLink.target.identifier),
                value: currentLink.value
            }
        }, this);

        var connectionsPerIdentifier = this.createConnectionsPerIdentifierTable(links);
        var connectionCounts = $.map(connectionsPerIdentifier, function(value, key) {
            return value;
        });
        var maxConnectionCount = this.getMaxValueFromArray(connectionCounts);
        var medianConnectionCount = this.getMedianValueFromArrayExcludingZeroes(connectionCounts);

        var commitList = this.filterObjectArray(nodeData, 'commitCount').sort(this.numberComparator);
        var medianCommitCount = this.getMedianValueFromArrayExcludingZeroes(commitList);
        var maxCommitCount = this.getMaxValueFromArray(commitList);

        var centeredNodeData = null;
        if (this.centeredIdentifier) {
            for (var i = 0; i < nodeData.length; ++i) {
                if (nodeData[i].identifier == this.centeredIdentifier) {
                    centeredNodeData = nodeData[i];
                    break;
                }
            }
        }
        if (centeredNodeData) {
            nodeData[i].fixed = true;
            nodeData[i].x = this.width / 2;
            nodeData[i].y = this.height / 2;
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

        var link = this.svg.selectAll(".link")
            .data(links)
            .enter().append("line")
            .attr("class", "proximityGraphLink")
            .style("stroke-width", function(d) { return 10 * (d.value / maxLinkValue); });

        var nodes = this.svg.selectAll('.node')
            .data(nodeData);

        nodes.enter()
            .append('circle')
                .attr('class', 'proximityGraphNode')
                .attr('r', function(d) {
                    return that.getNodeRadius(d, medianCommitCount);
                })
                .style('stroke', function(d) {
                    if (that.highlightSelection && d.identity.identifier == that.selectedIdentifier) {
                        return '#303138';
                    } else {
                        return '#ffffff';
                    }
                })
                .style('fill', function(d) {
                    var connectionCount = that.getConnectionCount(d.index, links);
                    var relativeConnectionCount = connectionCount / medianConnectionCount;
                    if (relativeConnectionCount >= 1) {
                        relativeConnectionCount = connectionCount / maxConnectionCount;
                        return that.mapConnectionsToColor(relativeConnectionCount, that.highConnectionColors);
                    } else {
                        return that.mapConnectionsToColor(relativeConnectionCount, that.lowConnectionColors);
                    }
                })
                .attr('fill-opacity', function(d) {
                    if (that.highlightSelection) {
                        return d.identity.identifier === that.selectedIdentifier ? 1 : that.defaultItemOpacity;
                    } else {
                        return that.defaultItemOpacity;
                    }
                })
                .on('mouseout', function(d) {
                    that.updateSelection(null);
                    d3.select(this).style('fill-opacity', that.defaultItemOpacity);
                })
                .on('mouseover', function(d) {
                    that.updateSelection(d.identity.identifier);
                    if (that.highlightSelection) {
                        d3.select(this).style('fill-opacity', 1);
                    }
                })
                .call(this.forceLayout.drag);

        nodes.append('title')
            .attr("pointer-events", 'none')
            .text(function(d) { return d.identity.name; });

        this.forceLayout.on('tick', function() {
            link.attr('x1', function(d) { return d.source.x; })
                .attr('y1', function(d) { return d.source.y; })
                .attr('x2', function(d) { return d.target.x; })
                .attr('y2', function(d) { return d.target.y; })

            nodes.attr('cx', function(d) { return d.x; })
                 .attr('cy', function(d) { return d.y; });
        });
    }

    this.getMaxLinkValue = function(graph) {
        var that = this;
        return graph.links.reduce(function(previousValue, currentLink, index, links) {
            if (that.isLinkSelected(graph.nodes, currentLink)) {
                return Math.max(previousValue, currentLink.value);
            } else {
                return previousValue;
            }
        }, -1);
    }

    this.filterNodes = function(nodeData) {
        var that = this;
        return nodeData.filter(function(node, index, array) {
            return that.isNodeSelected(node.identifier);
        }, []);
    }

    this.isNodeSelected = function(identifier) {
        return selectedUsers.isUserSelected(identifier);
    }

    this.isLinkSelected = function(nodes, link) {
        return this.isNodeSelected(link.source.identifier)
            && this.isNodeSelected(link.target.identifier);
    }

    /**
     * Filter out the links that are below the given relative percentage, between [0..1].
     * For example, if relativeThreshold is 0.1, all links that have under 10% of the maximum
     * interaction are filtered out.
     */
    this.filterLinks = function(nodes, links, maxLinkValue, relativeThreshold) {
        var that = this;
        var sourceLinks = jQuery.extend(true, [], links);
        return sourceLinks.filter(function(currentLink, index, array) {
            return (currentLink.value / maxLinkValue) >= relativeThreshold
                && that.isLinkSelected(nodes, currentLink);
        }, []);
    }

    this.getConnectionCount = function(nodeIndex, links) {
        var connectionCount = 0;
        for (var i = 0; i < links.length; ++i) {
            if (links[i].source.index == nodeIndex || links[i].target.index == nodeIndex) {
                connectionCount += links[i].value;
            }
        }
        return connectionCount;
    }

    this.isVarDefined = function(variable) {
        return typeof variable !== 'undefined';
    }

    this.safeIncrement = function(initialValue, toAdd) {
        return !this.isVarDefined(initialValue) ? toAdd : initialValue + toAdd;
    }

    this.createConnectionsPerIdentifierTable = function(links) {
        var connectionsPerIdentifier = {};
        for (var i = 0; i < links.length; ++i) {
            var connectionCount = links[i].value;
            var sourceConnections = connectionsPerIdentifier[links[i].source.identifier];
            var targetConnections = connectionsPerIdentifier[links[i].target.identifier];
            connectionsPerIdentifier[links[i].source.identifier] = this.safeIncrement(sourceConnections, connectionCount);
            connectionsPerIdentifier[links[i].target.identifier] = this.safeIncrement(targetConnections, connectionCount);
        }
        return connectionsPerIdentifier;
    }

    this.numberComparator = function(left, right) {
        return left - right;
    }

    this.filterObjectArray = function(objectList, key) {
        return objectList.map(function(currentValue, index, array) {
            return currentValue[key];
        }, []);
    }

    this.getMaxValueFromArray = function(list) {
        return list.reduce(function(previousValue, currentValue, index, array) {
            return Math.max(previousValue, currentValue);
        }, -1);
    }

    this.getMedianValueFromArrayExcludingZeroes = function(list) {
        var sortedList = list.slice().filter(function(currentValue, index, array) {
            return currentValue !== undefined;
        })
        .sort(this.numberComparator);
        var lastZero = sortedList.lastIndexOf(0);
        if (lastZero == -1) {
            lastZero = 0;
        }
        return sortedList[lastZero + Math.floor((sortedList.length - lastZero) / 2)];
    }

    this.mapConnectionsToColor = function(relativeConnectionCount, colors) {
        var colorIndex = Math.min(colors.length - 1, Math.floor(relativeConnectionCount * colors.length));
        return colors[colorIndex];
    }
}