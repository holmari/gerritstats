/**
 * Highlights top / bottom entries of the table cells.
 */
function TableCellHighlighter(overviewUserdata, dataKey) {
    // best to worst
    this.colorsTopEntries = ['#7fffc4', '#c9ffc7', '#e9fac8', '#faf4c8', '#ffedc9']
    // worst to best
    this.colorsBottomEntries = ['#ff90d5', '#ffaadf', '#ffacce', '#ffbdcd', '#ffcecc']

    this.useAscendingSort = false;

    this.ignoreZeroes = false;

    this.sortedData = null;

    /** Returns a sorted, unique list of values from a given array, for the given key. */
    function sortUniqueValues(arrayToFilter, key) {
        var descendingSorter = function(l, r) {
            return r[key] - l[key];
        };
        var ascendingSorter = function(l, r) {
            return l[key] - r[key];
        };
        var sortedArray = arrayToFilter.concat().sort(
            this.useAscendingSort ? ascendingSorter : descendingSorter);

        var filteredArray = [];
        sortedArray.forEach(function(element) {
            if (filteredArray.length == 0 || element[key] != filteredArray[filteredArray.length - 1]) {
                filteredArray.push(element[key]);
            }
        });
        return filteredArray;
    }

    function indexFinder(value) {
        return function(element) {
            return element == value;
        }
    }

    this.highlightDescending = function(value) {
        var topRangeCutoffIndex = Math.min(this.colorsTopEntries.length - 1, this.sortedData.length - 1);
        if (value >= this.sortedData[topRangeCutoffIndex]) {
            var colorIndex = this.sortedData.findIndex(indexFinder(value));
            return 'style=background:' + this.colorsTopEntries[colorIndex];
        } else if (this.sortedData.length >= this.colorsBottomEntries.length
            && value <= this.sortedData[this.sortedData.length - this.colorsBottomEntries.length])
        {
            var idx = this.sortedData.findIndex(indexFinder(value));
            var colorIndex = this.sortedData.length - 1 - idx;
            return 'style=background:' + this.colorsBottomEntries[colorIndex];
        } else {
            return '';
        }
    }

    this.highlightAscending = function(value) {
        var topRangeCutoffIndex = Math.min(this.colorsBottomEntries.length - 1, this.sortedData.length - 1);
        if (value >= this.sortedData[topRangeCutoffIndex]) {
            var colorIndex = this.sortedData.findIndex(indexFinder(value));
            return 'style=background:' + this.colorsBottomEntries[colorIndex];
        } else if (this.sortedData.length >= this.colorsTopEntries.length
            && value <= this.sortedData[this.sortedData.length - this.colorsTopEntries.length])
        {
            var idx = this.sortedData.findIndex(indexFinder(value));
            var colorIndex = this.sortedData.length - 1 - idx;
            return 'style=background:' + this.colorsTopEntries[colorIndex];
        } else {
            return '';
        }
    }

    this.highlight = function(object) {
        if (!this.sortedData) {
            this.sortedData = sortUniqueValues(overviewUserdata, dataKey);
        }

        var value = object[dataKey];
        if (this.sortedData.length == 0 || (this.ignoreZeroes && value == 0)) {
            return '';
        }

        return this.useAscendingSort ? this.highlightAscending(value) : this.highlightDescending(value);
    }

    this.setIsAscending = function(valueToSet) {
        this.useAscendingSort = valueToSet;
        return this;
    }

    this.setIgnoreZeroes = function(valueToSet) {
        this.ignoreZeroes = valueToSet;
        return this;
    }
}