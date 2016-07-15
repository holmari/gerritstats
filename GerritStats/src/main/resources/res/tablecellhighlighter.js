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
    this.highlightNegativeEntries = true;
    this.highlightPositiveEntries = true;

    this.sortedData = null;
    this.ignoreFunction = null;

    this.isFiltered = function(element, key) {
        if (this.ignoreZeroes && !element[key]) {
            return true;
        }
        if (this.ignoreFunction && this.ignoreFunction(element, key)) {
            return true;
        }
        return false;
    }

    /** Returns a sorted, unique list of values from a given array, for the given key. */
    this.sortUniqueValues = function() {
        var descendingSorter = function(l, r) {
            return r[dataKey] - l[dataKey];
        };
        var ascendingSorter = function(l, r) {
            return l[dataKey] - r[dataKey];
        };
        var sortedArray = overviewUserdata.concat().sort(
            this.useAscendingSort ? ascendingSorter : descendingSorter);

        var that = this;
        var filteredArray = [];
        sortedArray.forEach(function(element) {
            if ((filteredArray.length == 0 || element[dataKey] != filteredArray[filteredArray.length - 1])
                && !that.isFiltered(element, dataKey)) {
                filteredArray.push(element[dataKey]);
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
        if (this.highlightPositiveEntries && value >= this.sortedData[topRangeCutoffIndex]) {
            var colorIndex = this.sortedData.findIndex(indexFinder(value));
            return 'style=background:' + this.colorsTopEntries[colorIndex];
        }

        var leastBottomItemIndex = this.sortedData.length >= this.colorsBottomEntries.length
                                 ? this.sortedData.length - this.colorsBottomEntries.length
                                 : this.sortedData.length - 1;
        var leastBottomItemValue = this.sortedData[leastBottomItemIndex];
        if (this.highlightNegativeEntries && value <= leastBottomItemValue) {
            var idx = this.sortedData.findIndex(indexFinder(value));
            var colorIndex = this.sortedData.length - 1 - idx;
            return 'style=background:' + this.colorsBottomEntries[colorIndex];
        }

        return '';
    }

    this.highlightAscending = function(value) {
        var topRangeCutoffIndex = Math.min(this.colorsTopEntries.length - 1, this.sortedData.length - 1);
        var leastTopItemValue = this.sortedData[topRangeCutoffIndex];
        if (this.highlightPositiveEntries && value <= leastTopItemValue) {
            var colorIndex = this.sortedData.findIndex(indexFinder(value));
            return 'style=background:' + this.colorsTopEntries[colorIndex];
        }

        var leastBottomItemIndex = this.sortedData.length >= this.colorsBottomEntries.length
                                 ? this.sortedData.length - this.colorsBottomEntries.length
                                 : this.sortedData.length - 1;
        var leastBottomItemValue = this.sortedData[leastBottomItemIndex];
        if (this.highlightNegativeEntries && value >= leastBottomItemValue) {
            var idx = this.sortedData.findIndex(indexFinder(value));
            var colorIndex = this.sortedData.length - 1 - idx;
            return 'style=background:' + this.colorsBottomEntries[colorIndex];
        }

        return '';
    }

    this.highlight = function(object) {
        if (!this.sortedData) {
            this.sortedData = this.sortUniqueValues();
        }

        var value = object[dataKey];
        if (this.sortedData.length == 0 || (this.ignoreZeroes && value == 0)) {
            return '';
        }
        if (this.ignoreFunction && this.ignoreFunction(object, dataKey)) {
            return '';
        }

        return this.useAscendingSort ? this.highlightAscending(value) : this.highlightDescending(value);
    }

    this.setIsAscending = function(valueToSet) {
        this.useAscendingSort = valueToSet;
        return this;
    }

    this.setHighlightNegativeEntries = function(valueToSet) {
        this.highlightNegativeEntries = valueToSet;
        return this;
    }

    this.setHighlightPositiveEntries = function(valueToSet) {
        this.highlightPositiveEntries = valueToSet;
        return this;
    }

    this.setIgnoreZeroes = function(valueToSet) {
        this.ignoreZeroes = valueToSet;
        return this;
    }

    /**
     * Sets the function called to check whether a value should be
     * ignored. Two arguments are passed: element and key.
     * Return true from the function to ignore the element.
     */
    this.setIgnoreFunction = function(ignoreFunction) {
        this.ignoreFunction = ignoreFunction;
        return this;
    }
}