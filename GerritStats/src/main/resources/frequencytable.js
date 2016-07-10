/**
 * Renders a simple 2d bar chart.
 */
function FrequencyTable(svgId, record, data) {
    this.xDomain = [record.getFromDate(), record.getToDate()];
    this.margin = {
        top: 20,
        right: 20,
        bottom: 60,
        left: 60
    };

    this.width = 1050 - this.margin.left - this.margin.right;
    this.height = 500 - this.margin.top - this.margin.bottom;

    this.colors = d3.scale.linear().domain([0, 10]).range(['#ffc400', '#02b221']);
    // The range is intentionally different; it seems quite rare to have high daily averages
    // even for most prolific reviewers.
    this.averageLineColors = d3.scale.linear().domain([0, 5]).range(['#ffc400', '#02b221']);

    this.initialize = function() {
        var that = this;

        this.x = d3.time.scale().range([0, this.width]);
        this.y = d3.scale.linear().range([this.height, 0]);

        this.xAxis = d3.svg.axis()
                        .scale(this.x)
                        .orient('bottom'),
        this.yAxis = d3.svg.axis()
                        .scale(this.y)
                        .orient('left');
        this.x.domain(this.xDomain);
        this.y.domain([0, d3.max(data, function(d) { return d.count; })]);

        this.svg = d3.select(svgId)
            .append('svg')
                .attr('width', this.width + this.margin.left + this.margin.right)
                .attr('height', this.height + this.margin.top + this.margin.bottom)
            .append('g')
                .attr('transform', 'translate(' + this.margin.left + ',' + this.margin.top + ')');

        var msecsInDay = 1000 * 60 * 60 * 24;
        var daysInDomain = Math.round((this.xDomain[1].getTime() - this.xDomain[0].getTime()) / msecsInDay);
        var weeksInDomain = daysInDomain / 7;
        daysInDomain = Math.max(1, Math.round(daysInDomain * (5 / 7)));

        this.averageReviewsPerDay = data.reduce(function(prevValue, currentValue) {
            return prevValue + currentValue.count;
        }, 0) / daysInDomain;

        this.svg.append('g')
          .attr('class', 'y chartAxis')
          .append('text')
              .attr('class', 'chartAxisLabel')
              .attr('transform', 'rotate(90) translate(' + (this.height / 2) + ', 0)')
              .attr('x', 0)
              .attr('y', this.margin.left - 6)
              .style('text-anchor', 'middle')
              .text('Comments per day');

        var totalDaysInDomain = d3.time.days(this.xDomain[0], this.xDomain[1]);
        var barWidth = Math.max((this.width - this.margin.left - this.margin.top) / totalDaysInDomain.length, 1);
        this.svg.selectAll('.bar')
            .data(data).enter()
            .append('rect')
              .attr('class', 'commentChartBar')
              .attr('x', function(d) { return that.x(d.date); })
              .attr('width', barWidth)
              .attr('y', function(d) { return that.y(d.count); })
              .attr('height', function(d) { return that.height - that.y(d.count); })
              .attr('fill', function(d) { return that.colors(d.count); })
           .append('svg:title')
              .text(function(d) { return d.date });

        this.averageGuide = this.svg.append('line')
            .attr('class', 'verticalGuideline')
            .attr('x1', 0)
            .attr('y1', this.y(this.averageReviewsPerDay))
            .attr('x2', this.width)
            .attr('y2', this.y(this.averageReviewsPerDay))
            .style('stroke', this.averageLineColors(this.averageReviewsPerDay))
            .style("stroke-dasharray", ("7, 7"));

       this.helpTextLabel = this.svg.append('text')
            .attr('class', 'chartGuidelineText')
            .attr('x', this.width)
            .attr('y', this.y(this.averageReviewsPerDay) - 7)
            .style('text-anchor', 'end')
            .style('fill-opacity', 0.75)
            .style('font-size', '12px')
            .text('Avg (excl. weekends): ' + this.averageReviewsPerDay.toFixed(1));

        this.svg.append('g')
            .attr('class', 'x chartAxis')
            .attr('transform', 'translate(0,' + this.height + ')')
            .call(this.xAxis);

        this.svg.append('g')
            .attr('class', 'y chartAxis')
            .call(this.yAxis);
    }

    this.initialize();
}