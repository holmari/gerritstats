function ReviewersAndApprovalsGraph(svgId, reviewerData) {
    this.margin = {
        top: 20,
        right: 20,
        bottom: 40,
        left: 40
    };
    this.width = 690 - this.margin.left - this.margin.right;
    this.height = 600 - this.margin.top - this.margin.bottom;

    this.selectedReviewer = null;
    this.colors = d3.scale.category10();

    this.initialize = function() {
        // axes size: max of added reviewer count.
        var tickCount = 0;
        this.maxValue = 0;
        if (reviewerData.length > 0) {
            tickCount = this.maxValue = reviewerData.reduce(
                function(previousValue, currentReviewerData, index, array) {
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

        this.svg = d3.select(svgId).append('svg')
            .attr('width', this.width + this.margin.left + this.margin.right)
            .attr('height', this.height + this.margin.top + this.margin.bottom)
            .append("g")
            .attr('transform', 'translate(' + this.margin.left + ',' + this.margin.top + ')');

        this.svg.append('g')
            .attr('class', 'x reviewerApprovalChartAxis')
            .attr('transform', 'translate(0, ' + this.height + ')')
            .append('text')
                .attr('transform', 'translate(' + (this.width / 2) + ', 0)')
                .attr('class', 'gia-axisLabel')
                .attr('x', 0)
                .attr('y', this.margin.bottom)
                .style('text-anchor', 'middle')
                .text('Number of times added as reviewer');

        this.svg.append('g')
            .attr('class', 'y reviewerApprovalChartAxis')
            .append('text')
                .attr('class', 'gia-axisLabel')
                .attr('transform', 'rotate(90) translate(' + (this.height / 2) + ' 0)')
                .attr('x', 0)
                .attr('y', this.margin.left)
                .style('text-anchor', 'middle')
                .text('Number of approvals');

        this.svg.append('g')
            .attr('class', 'x reviewerApprovalChartAxis')
            .attr('transform', 'translate(0,' + this.height + ')');

        this.svg.append('g')
            .attr('class', 'y reviewerApprovalChartAxis');

        this.svg.append('g')
            .attr('class', 'reviewerApprovals')

        var diagonal = this.svg.append("line")
                             .attr('x1', 0)
                             .attr('y1', this.height)
                             .attr('x2', this.width)
                             .attr('y2', 0)
                             .attr('stroke-width', 0.5)
                             .attr('stroke', '#dddddd')
                             .attr('fill-opacity', 0.5);
    };

    this.renderPoints = function() {
        var g = this.svg.select('g.reviewerApprovals');

        points = g.selectAll('circle.reviewerApproval')
                    .data(reviewerData);
        var graph = this;
        points.enter()
            .append('circle')
                .attr('class', 'reviewerApproval')
                .attr('cx', function(d) { return graph.xScale(d.approvalData.addedAsReviewerCount); })
                .attr('cy', function(d) { return graph.yScale(d.approvalData.approvalCount); })
                .attr('r', function(d) { return 5 + (d.approvalData.addedAsReviewerCount / graph.maxValue) * 30; })
                .attr('fill', function(d) { return graph.colors(d.identity.email); })
                .attr('stroke', 'rgba(0,0,0, .05)')
                .on('mouseover', function(d) { graph.selectedReviewer = d; graph.render(); })
                .on('mouseout', function(d) { graph.selectedReviewer = null; graph.render(); })
            .append('svg:title')
                .text(function(d) { return d.identity.name; });

        points
            .classed('selected', function(d) { return graph.selectedReviewer && d === graph.selectedReviewer; })
            .attr('fill-opacity', function(d) {
                if (graph.selectedReviewer) {
                    return d === graph.selectedReviewer ? 1 : 0.1;
                } else {
                    return 0.3;
                }
            });
    };

    this.render = function() {
        this.svg.select('g.x.reviewerApprovalChartAxis')
            .call(this.xAxis);

        this.svg.select('g.y.reviewerApprovalChartAxis')
            .call(this.yAxis);

        this.renderPoints();
    };

    this.initialize();
}
