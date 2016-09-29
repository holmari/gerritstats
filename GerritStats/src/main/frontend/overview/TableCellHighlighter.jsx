/**
 * Highlights top / bottom entries of the table cells.
 */
export default class TableCellHighlighter {
    constructor(overviewUserdata, selectedUsers, dataKey) {
        this.overviewUserdata = overviewUserdata;

        // best to worst
        this.colorsTopEntries = ['#beffec', '#eeffed', '#f9feee', '#fefcee', '#fffaee'];
        // worst to best
        this.colorsBottomEntries = ['#ffcdf3', '#ffdff6', '#ffe0f0', '#ffc2d4', '#fff0ef'];

        this.useAscendingSort = false;
        this.ignoreZeroes = false;
        this.highlightNegativeEntries = true;
        this.highlightPositiveEntries = true;

        this.sortedData = null;
        this.ignoreFunction = null;
        this.dataKey = dataKey;

        this.setSelectedUsers(selectedUsers);
    }

    _isFiltered(element, key) {
        if (this.ignoreZeroes && !element[key]) {
            return true;
        }
        if (this.ignoreFunction && this.ignoreFunction(element, key)) {
            return true;
        }
        if (!this.selectedUsers.isUserSelected(element.identity['identifier'])) {
            return true;
        }
        return false;
    }

    /**
     * Returns a sorted, unique list of values from a given array, for the given key.
     * Also removes all users not included in analysis.
     */
    sortUniqueValues() {
        var descendingSorter = function(l, r) {
            return r[this.dataKey] - l[this.dataKey];
        }.bind(this);
        var ascendingSorter = function(l, r) {
            return l[this.dataKey] - r[this.dataKey];
        }.bind(this);
        var sortedArray = this.overviewUserdata.concat().sort(
            this.useAscendingSort ? ascendingSorter : descendingSorter);

        var filteredArray = [];
        sortedArray.forEach(function(element) {
            if ((filteredArray.length == 0 || element[this.dataKey] != filteredArray[filteredArray.length - 1])
                && !this._isFiltered(element, this.dataKey)) {
                filteredArray.push(element[this.dataKey]);
            }
        }.bind(this));
        return filteredArray;
    }

    _indexFinder(value) {
        return function(element) {
            return element == value;
        };
    }

    highlightDescending(value) {
        var topRangeCutoffIndex = Math.min(this.colorsTopEntries.length - 1, this.sortedData.length - 1);
        if (this.highlightPositiveEntries && value >= this.sortedData[topRangeCutoffIndex]) {
            let colorIndex = this.sortedData.findIndex(this._indexFinder(value));
            return this.colorsTopEntries[colorIndex];
        }

        var leastBottomItemIndex = this.sortedData.length >= this.colorsBottomEntries.length
                                 ? this.sortedData.length - this.colorsBottomEntries.length
                                 : this.sortedData.length - 1;
        var leastBottomItemValue = this.sortedData[leastBottomItemIndex];
        if (this.highlightNegativeEntries && value <= leastBottomItemValue) {
            var idx = this.sortedData.findIndex(this._indexFinder(value));
            let colorIndex = this.sortedData.length - 1 - idx;
            return this.colorsBottomEntries[colorIndex];
        }

        return '';
    }

    highlightAscending(value) {
        var topRangeCutoffIndex = Math.min(this.colorsTopEntries.length - 1, this.sortedData.length - 1);
        var leastTopItemValue = this.sortedData[topRangeCutoffIndex];
        if (this.highlightPositiveEntries && value <= leastTopItemValue) {
            let colorIndex = this.sortedData.findIndex(this._indexFinder(value));
            return this.colorsTopEntries[colorIndex];
        }

        var leastBottomItemIndex = this.sortedData.length >= this.colorsBottomEntries.length
                                 ? this.sortedData.length - this.colorsBottomEntries.length
                                 : this.sortedData.length - 1;
        var leastBottomItemValue = this.sortedData[leastBottomItemIndex];
        if (this.highlightNegativeEntries && value >= leastBottomItemValue) {
            let idx = this.sortedData.findIndex(this._indexFinder(value));
            let colorIndex = this.sortedData.length - 1 - idx;
            return this.colorsBottomEntries[colorIndex];
        }

        return '';
    }

    getHighlightColor(objectOrIndex) {
        var object = objectOrIndex;
        if (typeof objectOrIndex === 'number') {
            object = this.overviewUserdata[objectOrIndex];
        }

        if (!this.sortedData || this.needsUpdate) {
            this.sortedData = this.sortUniqueValues();
            this.needsUpdate = false;
        }

        if (!this.sortedData.length || this._isFiltered(object, this.dataKey)) {
            return '';
        }

        var value = object[this.dataKey];
        return this.useAscendingSort ? this.highlightAscending(value) : this.highlightDescending(value);
    }

    setOverviewUserdata(overviewUserdata) {
        this.overviewUserdata = overviewUserdata;
    }

    setSelectedUsers(selectedUsers) {
        this.selectedUsers = selectedUsers;
        this.needsUpdate = true;
    }

    setIsAscending(valueToSet) {
        this.useAscendingSort = valueToSet;
        return this;
    }

    setHighlightNegativeEntries(valueToSet) {
        this.highlightNegativeEntries = valueToSet;
        return this;
    }

    setHighlightPositiveEntries(valueToSet) {
        this.highlightPositiveEntries = valueToSet;
        return this;
    }

    setIgnoreZeroes(valueToSet) {
        this.ignoreZeroes = valueToSet;
        return this;
    }

    /**
     * Sets the function called to check whether a value should be
     * ignored. Two arguments are passed: element and key.
     * Return true from the function to ignore the element.
     */
    setIgnoreFunction(ignoreFunction) {
        this.ignoreFunction = ignoreFunction;
        return this;
    }
}