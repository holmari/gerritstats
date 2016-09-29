import moment from 'moment';

import * as d3 from 'd3';
import legend from 'd3-svg-legend/no-extend';
import {getDefaultXAxisTimeFormat} from '../../common/charts/D3Utils';

/*
 * Creates a dynamic vertical guide that appears when user moves the mouse,
 * and the related components.
 */
class VerticalGuide {

    constructor(graph) {
        this.graph = graph;
        this.svg = graph.svg;

        this.legendGroupWidth = 190;
        this.legendGroupHeight = 120;
        this.legendGroupMargin = 10;

        this.render();
    }

    render() {
        this.guideLine = this.svg.append('line')
            .attr('class', 'chartGuideline')
            .attr('x1', 100)
            .attr('y1', 0)
            .attr('x2', 100)
            .attr('y2', this.graph.height)
            .style('visibility', 'hidden');

        this.dynamicLegendGroup = this.svg.append('g')
            .style('visibility', 'hidden');

        this.dynamicLegendGroup.append('rect')
            .attr('class', 'chartDynamicLegendBox')
            .attr('width', this.legendGroupWidth)
            .attr('height', this.legendGroupHeight);

        this.legendValueTime = this.dynamicLegendGroup.append('text')
            .attr('class', 'chartDynamicLegendTitle')
            .attr('x', 10)
            .attr('y', this.graph.margin.top);

        var legendWrapper = this.dynamicLegendGroup.append('g')
            .attr('transform', 'translate(' + -10 + ',' + 20 + ')');

        this.graph.createLegend(legendWrapper);

        var legendValueXPos = 145;
        this.legendValueCommitCount = this.dynamicLegendGroup.append('text')
            .attr('class', 'chartLegend')
            .attr('x', legendValueXPos)
            .attr('y', this.graph.margin.top + 34);

        this.legendValueCommentsWritten = this.dynamicLegendGroup.append('text')
            .attr('class', 'chartLegend')
            .attr('x', legendValueXPos)
            .attr('y', this.graph.margin.top + 58);

        this.legendValueCommentsReceived = this.dynamicLegendGroup.append('text')
            .attr('class', 'chartLegend')
            .attr('x', legendValueXPos)
            .attr('y', this.graph.margin.top + 83);

        this.focusCircleGroup = this.svg.append('g')
            .attr('class', 'focusCircleGroup')
            .style('visibility', 'hidden');

        this.focusCircleCommitCount = this.focusCircleGroup.append('circle')
                .attr('class', 'focusCircleCommitCount')
                .attr('r', 5);

        this.focusCircleCommentsWritten = this.focusCircleGroup.append('circle')
                .attr('class', 'focusCircleCommentsWritten')
                .attr('r', 5);

        this.focusCircleCommentsReceived = this.focusCircleGroup.append('circle')
                .attr('class', 'focusCircleCommentsReceived')
                .attr('r', 5);

        this.dateBisector = d3.bisector(function(d) { return d.date; }).left;
    }

    setVisibility(isVisible) {
        var visibility = isVisible ? 'visible' : 'hidden';
        this.guideLine.style('visibility', visibility);
        this.dynamicLegendGroup.style('visibility', visibility);
        this.focusCircleGroup.style('visibility', visibility);

        // Hide the legend from the graph, it can get on the way and it's visually duplicated
        // while the guide is visible
        this.graph.legend.style('visibility', !isVisible ? 'visible' : 'hidden');
    }

    getClosestValueToDate(dataArray, referenceDate) {
        var index = this.dateBisector(dataArray, referenceDate, 1);
        var value = 0;
        if (dataArray[index].date - referenceDate > dataArray[index - 1].date - referenceDate) {
            value = dataArray[index - 1].count;
        } else {
            value = dataArray[index].count;
        }
        return value;
    }

    updatePosition(mousePos) {
        var xPos = mousePos[0];
        this.guideLine.attr('x1', xPos)
                      .attr('x2', xPos);

        var legendGroupX = xPos + this.legendGroupMargin;
        if (xPos + this.legendGroupWidth >= this.graph.width) {
            legendGroupX = xPos - this.legendGroupMargin - this.legendGroupWidth;
        }
        this.dynamicLegendGroup.attr('transform', 'translate(' + legendGroupX + ','
                                                               + this.legendGroupMargin + ')');

        var dateAtX = Math.max(this.graph.xDomain[0], Math.min(this.graph.x.invert(xPos), this.graph.xDomain[1]));
        var commitCount = this.getClosestValueToDate(this.graph.cumulativeCommitData, dateAtX);
        var commentsWrittenCount = this.getClosestValueToDate(this.graph.writtenCommentData, dateAtX);
        var commentsReceivedCount = this.getClosestValueToDate(this.graph.receivedCommentData, dateAtX);

        this.legendValueTime.text(moment(dateAtX).format('YYYY-MM-DD'));
        this.legendValueCommitCount.text(commitCount);
        this.legendValueCommentsWritten.text(commentsWrittenCount);
        this.legendValueCommentsReceived.text(commentsReceivedCount);

        this.focusCircleCommitCount
            .attr('cx', xPos)
            .attr('cy', this.graph.y(commitCount));

        this.focusCircleCommentsWritten
            .attr('cx', xPos)
            .attr('cy', this.graph.y(commentsWrittenCount));

        this.focusCircleCommentsReceived
            .attr('cx', xPos)
            .attr('cy', this.graph.y(commentsReceivedCount));
    }
}

/**
 * Renders a simple 2d line chart, showing user's review statistics over time.
 *
 * Because review comments don't have timestamps, the dates are normalized
 * between the user record's from/to dates. This can cause small inaccuracies.
 * Also, the JSON data provided by Gerrit simply
 * doesn't have any timestamps for comments, so they're estimated based on
 * patch set creation date.
 */
export default class CumulativeChart {

    constructor(svgId, userRecord, config) {
        this.svgId = svgId;
        this.xDomain = userRecord.getFromDate() && userRecord.getToDate()
                     ? [userRecord.getFromDate(), userRecord.getToDate()]
                     : [];

        this.margin = config['margin'] || {
            top: 20,
            right: 20,
            bottom: 60,
            left: 60
        };

        this.width = config['width'] || (1050 - this.margin.left - this.margin.right);
        this.height = config['height'] || (500 - this.margin.top - this.margin.bottom);

        this.userRecord = userRecord;

        // The 'step-after' interpolation mode makes the most sense, as
        // it reflect the nature of user actions; they occur on particular moments,
        // and not in between (as in linear interpolation).
        this.interpolationMode = 'step-after';

        this.cumulativeCommitsPath = null;
        this.writtenCommentsPath = null;
        this.receivedCommentsPath = null;

        this.selectionGuide = null;

        this.render();
    }

    isValid() {
        return (this.xDomain.length == 2);
    }

    parseCumulativeCommitData() {
        var that = this;
        var cumulativeCommits = [];
        cumulativeCommits.push({
            date: this.xDomain[0],
            count: 0
        });

        var sortedCommits = this.userRecord.getCommits().sort(function(l, r) {
            return (l.createdOnDate < r.createdOnDate) ? -1 : (r.createdOnDate < l.createdOnDate ? 1 : 0);
        });
        sortedCommits.forEach(function(commit) {
            cumulativeCommits.push({
                date: Math.max(that.xDomain[0],
                                 Math.min(that.xDomain[1], commit.createdOnDate)),
                count: cumulativeCommits.length + 1
            });
        });
        cumulativeCommits.push({
            date: this.xDomain[1],
            count: cumulativeCommits.length
        });
        return cumulativeCommits;
    }

    parseCumulativeWrittenCommentData() {
        var that = this;
        var cumulativeWrittenComments = [];
        cumulativeWrittenComments.push({
            date: this.xDomain[0],
            count: 0
        });

        var comments = [];
        this.userRecord.getCommentsWritten().forEach(function(commentItem) {
            comments = comments.concat(commentItem.commentsByUser);
        });
        comments.sort(function(l, r) {
            return (l.patchSetTimestamp < r.patchSetTimestamp)
                ? -1 : (r.patchSetTimestamp < l.patchSetTimestamp ? 1 : 0);
        });

        var commentCount = 0;
        comments.forEach(function(commentItem) {
            commentCount += 1;
            cumulativeWrittenComments.push({
                date: Math.max(that.xDomain[0],
                                 Math.min(that.xDomain[1], commentItem.patchSetTimestamp)),
                count: commentCount,
            });
        });

        cumulativeWrittenComments.push({
            date: this.xDomain[1],
            count: commentCount,
        });

        return cumulativeWrittenComments;
    }

    parseCumulativeReceivedCommentData() {
        var that = this;
        var cumulativeReceivedComments = [];
        cumulativeReceivedComments.push({
            date: this.xDomain[0],
            count: 0,
        });

        var sortedComments = this.userRecord.getCommentsReceived().slice().sort(function(l, r) {
            return (l.commit.createdOnDate < r.commit.createdOnDate)
                ? -1 : (r.commit.createdOnDate < l.commit.createdOnDate ? 1 : 0);
        });
        var commentCount = 0;
        sortedComments.forEach(function(commentItem) {
            commentCount += commentItem.commentsByUser.length;
            cumulativeReceivedComments.push({
                date: Math.max(that.xDomain[0],
                                 Math.min(that.xDomain[1], commentItem.commit.createdOnDate)),
                count: commentCount
            });
        });
        cumulativeReceivedComments.push({
            date: this.xDomain[1],
            count: commentCount,
        });
        return cumulativeReceivedComments;
    }

    createLegend(parentSvgItem) {
        var that = this;
        var legendScale = d3.scale.ordinal()
            .domain(['Commits',
                     'Comments written',
                     'Comments received'
                    ])
            .range(['rgb(74, 145, 255)', 'rgb(163, 204, 39)', 'rgb(255, 130, 112)']);

        this.legend = parentSvgItem.append('g')
            .attr('class', 'chartLegend')
            .attr('transform', 'translate(20,20)');

        var legendColor = legend.color()
            .shape('rect')
            .shapePadding(10)
            .classPrefix('__') // prevent incorrect fonts caused by bootstrap's label class
            .scale(legendScale)
            .on('cellclick', function(d) {
                var pathToToggle = null;
                switch (d) {
                case 'Commits':
                    pathToToggle = that.cumulativeCommitsPath;
                    break;
                case 'Comments written':
                    pathToToggle = that.writtenCommentsPath;
                    break;
                case 'Comments received':
                    pathToToggle = that.receivedCommentsPath;
                    break;
                default:
                    throw new Error('Unknown switch case ' + d);
                }
                var isSelected = pathToToggle.style('opacity') != 1;
                pathToToggle.style('opacity', isSelected ? 1 : 0);
                this.style.opacity = isSelected ? 1 : 0.3;
            });

        this.legend.call(legendColor);
    }

    render() {
        var that = this;

        this.x = d3.time.scale().range([0, this.width]);
        this.y = d3.scale.linear().range([this.height, 0]);

        this.xAxis = d3.svg.axis()
                        .scale(this.x)
                        .orient('bottom')
                        .tickFormat(getDefaultXAxisTimeFormat());
        this.yAxis = d3.svg.axis()
                        .scale(this.y)
                        .orient('left');

        this.cumulativeCommitData = this.parseCumulativeCommitData();
        this.receivedCommentData = this.parseCumulativeReceivedCommentData();
        this.writtenCommentData = this.parseCumulativeWrittenCommentData();

        this.x.domain(this.xDomain);
        this.y.domain([0, d3.max([this.cumulativeCommitData[this.cumulativeCommitData.length - 1].count,
                                  this.writtenCommentData[this.writtenCommentData.length - 1].count,
                                  this.receivedCommentData[this.receivedCommentData.length - 1].count])]);

        this.svg = d3.select(this.svgId)
            .append('svg')
                .attr('width', this.width + this.margin.left + this.margin.right)
                .attr('height', this.height + this.margin.top + this.margin.bottom)
            .append('g')
                .attr('transform', 'translate(' + this.margin.left + ',' + this.margin.top + ')');

        this.svg.append('g')
          .attr('class', 'y chartAxis')
          .append('text')
              .attr('class', 'chartAxisLabel')
              .attr('transform', 'rotate(90) translate(' + (this.height / 2) + ', 0)')
              .attr('x', 0)
              .attr('y', this.margin.left - 6)
              .style('text-anchor', 'middle')
              .text('Actions');

        if (this.isValid()) {
            var cumulativeCommitsLine = d3.svg.line()
                .x(function(d) { return that.x(d.date); })
                .y(function(d) { return that.y(d.count); })
                .interpolate(this.interpolationMode);

            var cumulativeWrittenCommentsLine = d3.svg.line()
                .x(function(d) { return that.x(d.date); })
                .y(function(d) { return that.y(d.count); })
                .interpolate(this.interpolationMode);

            var cumulativeReceivedCommentsLine = d3.svg.line()
                .x(function(d) { return that.x(d.date); })
                .y(function(d) { return that.y(d.count); })
                .interpolate(this.interpolationMode);

            this.cumulativeCommitsPath = this.svg.append('path')
               .datum(this.cumulativeCommitData)
               .attr('class', 'lineCommits')
               .attr('d', cumulativeCommitsLine);

            this.writtenCommentsPath = this.svg.append('path')
               .datum(this.writtenCommentData)
               .attr('class', 'lineWrittenComments')
               .attr('d', cumulativeWrittenCommentsLine);

            this.receivedCommentsPath = this.svg.append('path')
               .datum(this.receivedCommentData)
               .attr('class', 'lineReceivedComments')
               .attr('d', cumulativeReceivedCommentsLine);

            this.verticalGuide = new VerticalGuide(this);

            this.svg.append('rect')
                .attr('width', this.width + this.margin.left + this.margin.right)
                .attr('height', this.height + this.margin.top + this.margin.bottom)
                .attr('fill', 'transparent')
                .on('mouseover', function() {
                    that.verticalGuide.setVisibility(true);
                    that.verticalGuide.updatePosition(d3.mouse(this));
                })
                .on('mouseout', function() {
                    that.verticalGuide.setVisibility(false);
                })
                .on('mousemove', function() {
                    that.verticalGuide.updatePosition(d3.mouse(this));
                });
            this.createLegend(this.svg);
        }

        this.svg.append('g')
            .attr('class', 'x chartAxis')
            .attr('transform', 'translate(0,' + this.height + ')')
            .call(this.xAxis);

        this.svg.append('g')
            .attr('class', 'y chartAxis')
            .call(this.yAxis);
    }
}