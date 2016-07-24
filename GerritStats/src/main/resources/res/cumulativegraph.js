/**
 * Renders a simple 2d line chart, showing user's review statistics over time.
 *
 * Because review comments don't have timestamps, the dates are normalized
 * between the user record's from/to dates. This can cause small inaccuracies.
 * Also, the JSON data provided by Gerrit simply
 * doesn't have any timestamps for comments, so they're estimated based on
 * patch set creation date.
 */
function CumulativeGraph(svgId, userRecord) {
    this.xDomain = userRecord.getFromDate() && userRecord.getToDate()
                 ? [userRecord.getFromDate(), userRecord.getToDate()]
                 : [];
    this.margin = {
        top: 20,
        right: 20,
        bottom: 60,
        left: 60
    };

    this.width = 1050 - this.margin.left - this.margin.right;
    this.height = 500 - this.margin.top - this.margin.bottom;

    this.userRecord = userRecord;

    // The 'step-after' interpolation mode makes the most sense, as
    // it reflect the nature of user actions; they occur on particular moments,
    // and not in between (as in linear interpolation).
    this.interpolationMode = 'step-after';

    this.cumulativeCommitsPath = null;
    this.writtenCommentsPath = null;
    this.receivedCommentsPath = null;

    this.isValid = function() {
        return (this.xDomain.length == 2);
    }

    this.getCumulativeCommitData = function() {
        var that = this;
        var cumulativeCommits = [];
        cumulativeCommits.push({
            'date': this.xDomain[0],
            'count': 0
        });

        var sortedCommits = userRecord.commits.slice().sort(function(l, r) {
            return (l.createdOnDate < r.createdOnDate) ? -1 : (r.createdOnDate < l.createdOnDate ? 1 : 0);
        });
        sortedCommits.forEach(function(commit) {
            cumulativeCommits.push({
                'date': Math.max(that.xDomain[0],
                                 Math.min(that.xDomain[1], commit.createdOnDate)),
                'count': cumulativeCommits.length + 1
            });
        });
        cumulativeCommits.push({
            'date': this.xDomain[1],
            'count': cumulativeCommits.length
        });
        return cumulativeCommits;
    }

    this.getCumulativeWrittenCommentData = function() {
        var that = this;
        var cumulativeWrittenComments = [];
        cumulativeWrittenComments.push({
            'date': this.xDomain[0],
            'count': 0
        });

        var comments = [];
        userRecord.commentsWritten.forEach(function(commentItem) {
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
                'date': Math.max(that.xDomain[0],
                                 Math.min(that.xDomain[1], commentItem.patchSetTimestamp)),
                'count': commentCount
            });
        });

        cumulativeWrittenComments.push({
            'date': this.xDomain[1],
            'count': commentCount
        });

        return cumulativeWrittenComments;
    }

    this.getCumulativeReceivedCommentData = function() {
        var that = this;
        var cumulativeReceivedComments = [];
        cumulativeReceivedComments.push({
            'date': this.xDomain[0],
            'count': 0
        });

        var sortedComments = userRecord.commentsReceived.slice().sort(function(l, r) {
            return (l.commit.createdOnDate < r.commit.createdOnDate)
                ? -1 : (r.commit.createdOnDate < l.commit.createdOnDate ? 1 : 0);
        });
        var commentCount = 0;
        sortedComments.forEach(function(commentItem) {
            commentCount += commentItem.commentsByUser.length;
            cumulativeReceivedComments.push({
                'date': Math.max(that.xDomain[0],
                                 Math.min(that.xDomain[1], commentItem.commit.createdOnDate)),
                'count': commentCount
            });
        });
        cumulativeReceivedComments.push({
            'date': this.xDomain[1],
            'count': commentCount
        });
        return cumulativeReceivedComments;
    }

    this.createLegend = function() {
        var that = this;
        var ordinal = d3.scale.ordinal()
            .domain(['Commits',
                     'Comments written',
                     'Comments received'
                    ])
            .range(['rgb(255, 0, 0)', 'rgb(0, 0, 255)', 'rgb(0, 255, 0)']);

        this.svg.append('g')
            .attr('class', 'chartLegend')
            .attr('transform', 'translate(20,20)');

        var legendOrdinal = d3.legend.color()
            .shape('rect')
            .shapePadding(10)
            .classPrefix('__') // prevent incorrect fonts caused by bootstrap's label class
            .scale(ordinal)
            .on("cellclick", function(d) {
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

        this.svg.select('.chartLegend')
                .call(legendOrdinal);
    }

    this.initialize = function() {
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

        var cumulativeCommitData = this.getCumulativeCommitData();
        var receivedCommentData = this.getCumulativeReceivedCommentData();
        var writtenCommentData = this.getCumulativeWrittenCommentData();

        this.x.domain(this.xDomain);
        this.y.domain([0, d3.max([cumulativeCommitData[cumulativeCommitData.length - 1].count,
                                  writtenCommentData[writtenCommentData.length - 1].count,
                                  receivedCommentData[receivedCommentData.length - 1].count])]);

        this.svg = d3.select(svgId)
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
               .datum(cumulativeCommitData)
               .attr('class', 'lineCommits')
               .attr('d', cumulativeCommitsLine);

            this.writtenCommentsPath = this.svg.append('path')
               .datum(writtenCommentData)
               .attr('class', 'lineWrittenComments')
               .attr('d', cumulativeWrittenCommentsLine);

            this.receivedCommentsPath = this.svg.append('path')
               .datum(receivedCommentData)
               .attr('class', 'lineReceivedComments')
               .attr('d', cumulativeReceivedCommentsLine);

            this.createLegend();
        }

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