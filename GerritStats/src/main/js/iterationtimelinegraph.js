class IterationTimelineGraph {

    constructor(svgId, userData) {
        this.margin = {
            top: 20,
            right: 20,
            bottom: 20,
            left: 20
        };
        this.width = 685 - this.margin.left - this.margin.right;
        this.height = 480 - this.margin.top - this.margin.bottom;

        this.colors = d3.scale.category10();

        /** Allows listening to highlight changes caused by user interaction. */
        this.selectionChangedListener = null;
        /** The selected (hovered or otherwise) item, or null if nothing is selected. */
        this.selectedCommitUrl = null;

        /** The patch set count threshold after which the commits are considered as iterative. */
        this.highPatchSetCountThreshold = 5;

        this.commitData = userData.getDatedCommitsWithHighPatchSetCount();

        this.defaultItemOpacity = 0.3;

        this.maxValue = 0;

        // min date is the beginning of the year of the data set
        this.minDate = this.commitData.reduce(function(prevValue, currentValue) {
            return Math.min(prevValue, currentValue.commit.createdOnDate);
        }, new Date().getTime());
        this.minDate = moment(this.minDate).startOf('year');

        // max date is the end of the year of the data set
        this.maxDate = this.commitData.reduce(function(prevValue, currentValue) {
            return Math.max(prevValue, currentValue.commit.createdOnDate);
        }, new Date().getTime());
        this.maxDate = moment(this.maxDate).endOf('year');

        this.domain = [this.minDate, this.maxDate];
        this.xScale = d3.time.scale()
            .domain(this.domain)
            .range([0, this.width]);

        this.xAxis = d3.svg.axis()
            .scale(this.xScale)
            .orient('bottom')
            .tickFormat(getDefaultXAxisTimeFormat());

        this.svg = d3.select(svgId).append('svg')
            .attr('width', this.width + this.margin.left + this.margin.right)
            .attr('height', this.height + this.margin.top + this.margin.bottom)
            .append('g')
                .attr('transform', 'translate(' + this.margin.left + ',' + this.margin.top + ')');

        // timeline
        this.svg.append('line')
             .attr('x1', 0)
             .attr('y1', this.height / 2)
             .attr('x2', this.width)
             .attr('y2', this.height / 2)
             .attr('stroke-width', 0.5)
             .attr('stroke', '#dddddd')
             .attr('fill-opacity', 0.5);

        this.verticalGuide = this.svg.append('line')
             .attr('class', 'iterativeChartGuideline')
             .attr('x1', 100)
             .attr('y1', this.height / 2)
             .attr('x2', 100)
             .attr('y2', this.height)
             .style("stroke-dasharray", ("7, 7"))
             .style('visibility', 'hidden');

        this.helpTextLabel = this.svg.append('text')
             .attr('class', 'chartGuidelineText')
             .attr('x', 100)
             .attr('y', this.height / 2)
             .style('visibility', 'hidden');

        this.svg.append('g')
             .attr('class', 'x iterativeCommitChartAxis')
             .attr('transform', 'translate(0, ' + this.height + ')');

        this.svg.append('g')
            .attr('class', 'iterativeCommits')
    };

    setSelectedCommitUrl(commitUrl) {
        this.selectedCommitUrl = commitUrl;

        if (this.selectedCommitUrl) {
            var that = this;
            var matchingItems = this.svg.select('g.iterativeCommits')
                .selectAll('circle.iterativeCommitBubble')
                .filter(function(d, i) {
                    return d.commit.url === commitUrl;
                }).classed('selected', true);
            this.updateTooltip(matchingItems[0][0]);
        } else {
            this.svg.select('g.iterativeCommits')
                    .selectAll('circle.iterativeCommitBubble').classed('selected', false);
            this.updateTooltip(null);
        }
    };

    updateSelection(newSelection) {
        var previousSelection = this.selectedCommitUrl;
        this.selectedCommitUrl = newSelection;
        if (this.selectionChangedListener) {
            this.selectionChangedListener(this.selectedCommitUrl, previousSelection);
        }
    };

    updateTooltip(selectedObject) {
        if (this.selectedCommitUrl) {
            this.verticalGuide.style('visibility', 'visible');
            this.helpTextLabel.style('visibility', 'visible');
            var selection = d3.select(selectedObject);
            var xPos = Math.floor(selection.attr('cx'));
            var centerY = parseInt(selection.attr('cy'));
            var yPos = centerY + parseInt(selection.attr('r')) + 7;
            this.verticalGuide.style('stroke', selection.attr('fill'));
            this.verticalGuide.attr('x1', xPos)
                              .attr('x2', xPos)
                              .attr('y1', yPos);
            this.helpTextLabel.attr('x', xPos + 15);
            this.helpTextLabel.attr('y', centerY + (this.height / 4));
            this.helpTextLabel.text(moment(selection.data()[0].date).format('LLLL'));
        } else {
            this.verticalGuide.style('visibility', 'hidden');
            this.helpTextLabel.style('visibility', 'hidden');
        }
    }

    renderPoints() {
        var g = this.svg.select('g.iterativeCommits');

        var points = g.selectAll('circle.iterativeCommitBubble')
                      .data(this.commitData);

        var graph = this;
        points.enter()
            .append('circle')
                .attr('class', 'iterativeCommitBubble')
                .attr('cx', function(d) { return graph.xScale(d.commit.createdOnDate); })
                .attr('cy', function(d) { return graph.height / 2 })
                .attr('r', function(d) { return graph.highPatchSetCountThreshold + 1.5 * d.count; })
                .attr('fill', function(d) { return graph.colors(d.count); })
                .attr('stroke', 'rgba(0,0,0, .05)')
                .on('mouseover', function(d) {
                    graph.updateSelection(d.commit.url);
                    graph.updateTooltip(this);
                    d3.select(this).classed('selected', true);
                })
                .on('mouseout', function(d) {
                    graph.updateSelection(null);
                    graph.updateTooltip(this);
                    d3.select(this).classed('selected', false);
                })
            .append('svg:title')
                .text(function(d) { return d.commit.subject; });

        points
            .classed('selected', function(d) {
                return (d.commit.url === graph.selectedCommitUrl);
            })
            .attr('fill-opacity', function(d) {
                if (graph.selectedCommitUrl) {
                    return d.commit.url === graph.selectedCommitUrl ? 1 : graph.defaultItemOpacity;
                } else {
                    return graph.defaultItemOpacity;
                }
            });
    };

    render() {
        this.svg.select('g.x.iterativeCommitChartAxis')
            .call(this.xAxis);

        this.renderPoints();
    };
}
