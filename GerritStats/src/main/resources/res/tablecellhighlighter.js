/**
 * Highlights top / bottom entries of the table cells.
 */
function TableCellHighlighter(overviewUserdata, usersInAnalysis, dataKey) {
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
        if (!usersInAnalysis.isUserSelected(element.identity.identifier)) {
            return true;
        }
        return false;
    }

    /**
     * Returns a sorted, unique list of values from a given array, for the given key.
     * Also removes all users not included in analysis.
     */
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
            return this.colorsTopEntries[colorIndex];
        }

        var leastBottomItemIndex = this.sortedData.length >= this.colorsBottomEntries.length
                                 ? this.sortedData.length - this.colorsBottomEntries.length
                                 : this.sortedData.length - 1;
        var leastBottomItemValue = this.sortedData[leastBottomItemIndex];
        if (this.highlightNegativeEntries && value <= leastBottomItemValue) {
            var idx = this.sortedData.findIndex(indexFinder(value));
            var colorIndex = this.sortedData.length - 1 - idx;
            return this.colorsBottomEntries[colorIndex];
        }

        return '';
    }

    this.highlightAscending = function(value) {
        var topRangeCutoffIndex = Math.min(this.colorsTopEntries.length - 1, this.sortedData.length - 1);
        var leastTopItemValue = this.sortedData[topRangeCutoffIndex];
        if (this.highlightPositiveEntries && value <= leastTopItemValue) {
            var colorIndex = this.sortedData.findIndex(indexFinder(value));
            return this.colorsTopEntries[colorIndex];
        }

        var leastBottomItemIndex = this.sortedData.length >= this.colorsBottomEntries.length
                                 ? this.sortedData.length - this.colorsBottomEntries.length
                                 : this.sortedData.length - 1;
        var leastBottomItemValue = this.sortedData[leastBottomItemIndex];
        if (this.highlightNegativeEntries && value >= leastBottomItemValue) {
            var idx = this.sortedData.findIndex(indexFinder(value));
            var colorIndex = this.sortedData.length - 1 - idx;
            return this.colorsBottomEntries[colorIndex];
        }

        return '';
    }

    this.getHighlightColor = function(object) {
        if (!this.sortedData || this.needsUpdate) {
            this.sortedData = this.sortUniqueValues();
            this.needsUpdate = false;
        }

        if (!this.sortedData.length || this.isFiltered(object, dataKey)) {
            return '';
        }

        var value = object[dataKey];
        return this.useAscendingSort ? this.highlightAscending(value) : this.highlightDescending(value);
    }

    this.setNeedsUpdate = function() {
        this.needsUpdate = true;
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