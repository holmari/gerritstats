import '../../style/charts.scss';

import * as d3 from 'd3';

export default class ReviewersAndApprovalsChart {

    constructor(svgId, reviewerData, config) {
        this.margin = config.margin || {
            top: 20,
            right: 20,
            bottom: 60,
            left: 60
        };
        this.width = config.width || (480 - this.margin.left - this.margin.right);
        this.height = config.height || (480 - this.margin.top - this.margin.bottom);

        this.colors = d3.scale.category10();

        /** Allows listening to highlight changes caused by user interaction. */
        this.onSelectionChangedListener = config.selectionChangedListener;
        /** The selected (hovered or otherwise) item, or null if nothing is selected. */
        this.selectedReviewer = null;

        this.reviewerData = reviewerData;

        // axes size: max of added reviewer count.
        var tickCount = 0;
        this.maxValue = 0;
        if (reviewerData.length > 0) {
            tickCount = this.maxValue = reviewerData.reduce(
                function(previousValue, currentReviewerData) {
                    return Math.max(previousValue,
                                    Math.max(currentReviewerData.approvalData.addedAsReviewerCount,
                                             currentReviewerData.approvalData.approvalCount));
                }, 0);
        }

        // both axes share same domain
        this.domain = [0, tickCount];
        this.xScale = d3.scale.linear()
            .domain(this.domain)
            .range([0, this.width]);

        this.yScale = d3.scale.linear()
            .domain(this.domain)
            .range([this.height, 0]);

        this.xAxis = d3.svg.axis()
            .scale(this.xScale)
            .orient('bottom');

        this.yAxis = d3.svg.axis()
            .scale(this.yScale)
            .orient('left');

        this.svg = d3.select(svgId)
            .append('svg')
              .attr('width', this.width + this.margin.left + this.margin.right)
              .attr('height', this.height + this.margin.top + this.margin.bottom)
            .append('g')
              .attr('transform', 'translate(' + this.margin.left + ',' + this.margin.top + ')');

        // diagonal
        this.svg.append('line')
             .attr('x1', 0)
             .attr('y1', this.height)
             .attr('x2', this.width)
             .attr('y2', 0)
             .attr('stroke-width', 0.5)
             .attr('stroke', '#dddddd')
             .attr('fill-opacity', 0.5);

        this.svg.append('g')
            .attr('class', 'x chartAxis')
            .attr('transform', 'translate(0, ' + this.height + ')')
            .append('text')
                .attr('transform', 'translate(' + (this.width / 2) + ', 0)')
                .attr('class', 'chartAxisLabel')
                .attr('x', 0)
                .attr('y', this.margin.bottom - 6)
                .style('text-anchor', 'middle')
                .text('Number of times added as reviewer');

        this.svg.append('g')
            .attr('class', 'y chartAxis')
            .append('text')
                .attr('class', 'chartAxisLabel')
                .attr('transform', 'rotate(90) translate(' + (this.height / 2) + ', 0)')
                .attr('x', 0)
                .attr('y', this.margin.left - 6)
                .style('text-anchor', 'middle')
                .text('Number of approvals');

        this.svg.append('g')
            .attr('class', 'reviewerApprovals');
    }

    // FIXME: this re-renders the entire thing!
    setSelectedItemByIdentifier(userIdentifier) {
        this.selectedReviewer = userIdentifier;
        this.render();
    }

    updateSelection(newSelection) {
        var previousSelection = this.selectedReviewer;
        this.selectedReviewer = newSelection;
        if (this.onSelectionChangedListener) {
            this.onSelectionChangedListener(this.selectedReviewer, previousSelection);
        }
    }

    renderPoints() {
        var g = this.svg.select('g.reviewerApprovals');

        var points = g.selectAll('circle.reviewerApproval')
                      .data(this.reviewerData);

        var graph = this;
        points.enter()
            .append('circle')
                .attr('class', 'reviewerApproval')
                .attr('cx', function(d) { return graph.xScale(d.approvalData.addedAsReviewerCount); })
                .attr('cy', function(d) { return graph.yScale(d.approvalData.approvalCount); })
                .attr('r', function(d) { return 5 + (d.approvalData.addedAsReviewerCount / graph.maxValue) * 30; })
                .attr('fill', function(d) { return graph.colors(d.identity['email']); })
                .attr('stroke', 'rgba(0,0,0, .05)')
                .on('mouseover', function(d) {
                    graph.updateSelection(d.identity['identifier']);
                    graph.render();
                })
                .on('mouseout', function() {
                    graph.updateSelection(null);
                    graph.render();
                })
            .append('svg:title')
                .text(function(d) { return d.identity['name']; });

        points
            .classed('selected', function(d) {
                return graph.selectedReviewer && d.identity['identifier'] === graph.selectedReviewer;
            })
            .attr('fill-opacity', function(d) {
                if (graph.selectedReviewer) {
                    return d.identity['identifier'] === graph.selectedReviewer ? 1 : 0.3;
                } else {
                    return 0.3;
                }
            });
    }

    render() {
        this.svg.select('g.x.chartAxis')
            .call(this.xAxis);

        this.svg.select('g.y.chartAxis')
            .call(this.yAxis);

        this.renderPoints();
    }
}
