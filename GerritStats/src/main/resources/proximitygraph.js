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

    // d3
    this.svg = null;

    this.create = function() {
        this.svg = d3.select(objectSelector).append('svg')
            .attr('width', this.width)
            .attr('height', this.height);

        this.forceLayout = d3.layout.force()
            .size([this.width, this.height])
            .charge(-450)
            .linkDistance(45);

        this.render();
    }

    this.updateSize = function() {
        this.width = $(objectSelector).width();
        this.height = $(objectSelector).height();
        this.svg.attr("width", this.width).attr("height", this.height);
        this.forceLayout.size([this.width, this.height]).resume();
    }

    this.render = function() {
        this.svg.html('');

        // TODO; when filtering users, the diagram links are not constructed correctly
        // because the identity graph has indices instead of references to the nodes.
        //
        // The graph's links must be constructed in a different way, so that they contain
        // *references* to nodes, instead of indices.

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

        nodes.enter().append('circle')
            .attr('class', 'proximityGraphNode')
            .attr('r', function(d) {
                return 3 + 2 * Math.sqrt(d.commitCount / medianCommitCount);
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
            .call(this.forceLayout.drag);

        nodes.append('title')
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

    this.create(identityGraph);
}