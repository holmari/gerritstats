/**
 * Renders a simple 2d bar chart.
 */
function FrequencyTable(svgId, record, data) {
    this.xDomain = [record.getFromDate(), record.getToDate()];
    this.margin = {
        top: 20,
        right: 20,
        bottom: 30,
        left: 40
    };
    this.width = 1050 - this.margin.left - this.margin.right;
    this.height = 400 - this.margin.top - this.margin.bottom;

    this.initialize = function() {
        this.x = d3.time.scale()
            .domain(this.xDomain)
            .range([0, this.width]);

        this.y = d3.scale.linear()
            .range([this.height, 0]);

        this.xAxis = d3.svg.axis()
            .scale(this.x)
            .orient("bottom")
            .ticks(d3.time.weeks(this.xDomain[0], this.xDomain[1]).length)
            .tickFormat(d3.time.format("%W"))

        this.yAxis = d3.svg.axis()
            .scale(this.y)
            .orient("left")
            .ticks(10)
            .tickFormat(d3.format("d"))

        this.svg = d3.select(svgId).append("svg")
            .attr("width", this.width + this.margin.left + this.margin.right)
            .attr("height", this.height + this.margin.top + this.margin.bottom)
            .append("g")
            .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");

        this.x.domain(this.xDomain);
        this.y.domain([0, d3.max(data, function(d) { return d.count; })]);

        this.svg.append("g")
          .attr("class", "x commentChartAxis")
          .attr("transform", "translate(0," + this.height + ")")
          .call(this.xAxis);

        this.svg.append("g")
          .attr("class", "y commentChartAxis")
          .call(this.yAxis)
        .append("text")
          .attr("transform", "rotate(-90)")
          .attr("y", 6)
          .attr("dy", ".71em")
          .style("text-anchor", "end")
          .text("Value");

        var totalDaysInDomain = d3.time.days(this.xDomain[0], this.xDomain[1]);
        var barWidth = this.width / totalDaysInDomain.length;
        var graph = this;
        this.svg.selectAll(".bar")
            .data(data)
          .enter().append("rect")
            .attr("class", "commentChartBar")
            .attr("x", function(d) { return graph.x(d.date); })
            .attr("width", barWidth)
            .attr("y", function(d) { return graph.y(d.count); })
            .attr("height", function(d) { return graph.height - graph.y(d.count); })
           .append("svg:title")
            .text(function(d) { return d.date });
    }

    this.initialize();
}