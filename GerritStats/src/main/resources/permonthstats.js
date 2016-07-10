/**
 * Renders a table of monthly statistics.
 */
function PerMonthStats(parentId, userData) {

    this.addPerMonthStatsYearHeader = function(year) {
        $(parentId).append('<tr><th colspan="13" class="monthlyCommitYearTitle">' + year + '</th></tr>\
            <tr><th></th>\
                <th>Jan</th>\
                <th>Feb</th>\
                <th>Mar</th>\
                <th>Apr</th>\
                <th>May</th>\
                <th>Jun</th>\
                <th>Jul</th>\
                <th>Aug</th>\
                <th>Sep</th>\
                <th>Oct</th>\
                <th>Nov</th>\
                <th>Dec</th>\
            </tr>\
            <tr class="commitsSection">\
                <th>Commits</th>\
                <!-- content is added here -->\
            </tr>\
            <tr class="commentsSection">\
                <th>Comments</th>\
                <!-- content is added here -->\
            </tr>\
            <tr class="commitsMoMSection">\
                <th>Commits MoM %</th>\
                <!-- content is added here -->\
            </tr>\
            <tr class="commitsQoQSection">\
                <th>Commits QoQ %</th>\
                <!-- content is added here -->\
            </tr>\
            <tr class="commentsMoMSection">\
                <th>Comments MoM %</th>\
                <!-- content is added here -->\
            </tr>\
            <tr class="commentsQoQSection">\
                <th>Comments QoQ %</th>\
                <!-- content is added here -->\
            </tr>\
        ');
    }

    this.addPerMonthStats = function(record) {
        var commitTable = record.datedCommitTable;
        var commentTable = record.datedCommentTable;

        var years = record.datedCommitTable.getActiveYears();
        var perMonthStats = this;
        years.forEach(function(year) {
            perMonthStats.addPerMonthStatsYearHeader(year);
            for (var month = 1; month <= 12; ++month) {
                var commitCount = commitTable.getPrintableMonthlyItemCount(year, month);
                var commitsMoMChange = commitTable.getDisplayableMonthOnMonthChange(year, month);
                var commitsQoQChange = commitTable.getDisplayableQuarterOnQuarterChange(year, month);

                var commentCount = commentTable.getPrintableMonthlyItemCount(year, month);
                var commentsMoMChange = commentTable.getDisplayableMonthOnMonthChange(year, month);
                var commentsQoQChange = commentTable.getDisplayableQuarterOnQuarterChange(year, month);

                $(parentId + ' .commitsSection').last().append('<td>' + commitCount + '</td>');
                $(parentId + ' .commentsSection').last().append('<td>' + commentCount + '</td>');
                $(parentId + ' .commitsMoMSection').last().append('<td>' + commitsMoMChange + '</td>');
                $(parentId + ' .commitsQoQSection').last().append('<td>' + commitsQoQChange + '</td>');
                $(parentId + ' .commentsMoMSection').last().append('<td>' + commentsMoMChange + '</td>');
                $(parentId + ' .commentsQoQSection').last().append('<td>' + commentsQoQChange + '</td>');
            }
        });
    }

    this.addPerMonthStats(userData);
}