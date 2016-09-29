import numeral from 'numeral';
import moment from 'moment';

import {PERCENTAGE_FORMAT} from './GerritUserdata';

class MonthlyTimeFormat {

    static formatFloat(value) {
        if (Number.isFinite(value)) {
            return numeral(value).format(PERCENTAGE_FORMAT);
        } else if (Number.isNaN(value)) {
            return 'N/A';
        } else {
            return '\u221e%';
        }
    }

    static getSafeRateOfChange(prevValue, currentValue) {
        if (prevValue == currentValue) {
            return 0;
        } else if (prevValue != 0) {
            var delta = (currentValue / prevValue);
            return delta < 1 ? -(1 - delta) : delta - 1;
        } else {
            return currentValue > 0 ? Number.POSITIVE_INFINITY : Number.NEGATIVE_INFINITY;
        }
    }

    static formatRateOfChange(prevValue, nextValue) {
        return MonthlyTimeFormat.formatFloat(
            MonthlyTimeFormat.getSafeRateOfChange(prevValue, nextValue));
    }

    static monthToQuarter(month) {
        return Math.floor((month - 1) / 3);
    }
}

class YearlyItemList {

    constructor(epochFunction) {
        this.items = [];
        this.epochFunction = epochFunction;

        this.itemsPerMonth = {};
        for (var i = 1; i <= 12; ++i) {
            this.itemsPerMonth[i] = [];
        }
    }

    push(item) {
        var unixEpoch = this.epochFunction(item);
        var month = moment(unixEpoch).month() + 1;
        if (month < 1 || month > 12) {
            throw new Error('Month must be in [1..12] range');
        }
        this.itemsPerMonth[month].push(item);
        this.items.push(item);
    }

    getMonthlyItemCount(month) {
        return this.itemsPerMonth[month].length;
    }

    getMonthOnMonthChange(month) {
        if (month > 1) {
            var itemsInThisMonth = this.itemsPerMonth[month].length;
            var itemsInPrevMonth = this.itemsPerMonth[month - 1].length;
            return MonthlyTimeFormat.getSafeRateOfChange(itemsInPrevMonth, itemsInThisMonth);
        } else {
            return Number.NaN;
        }
    }

    getQuarterOnQuarterChange(month) {
        var quarter = MonthlyTimeFormat.monthToQuarter(month);
        var quarterStartMonth = 1 + (quarter * 3);
        if (quarterStartMonth > 1) {
            var quarterItemCount = this.getQuarterlyItemCount(quarter);
            var prevQuarterItemCount = 0;
            var prevQuarterStartMonth = quarterStartMonth - 3;

            for (var i = prevQuarterStartMonth; i <= prevQuarterStartMonth + 2; ++i) {
                prevQuarterItemCount += this.itemsPerMonth[i].length;
            }
            return MonthlyTimeFormat.getSafeRateOfChange(prevQuarterItemCount, quarterItemCount);
        } else {
            return Number.NaN;
        }
    }

    getDisplayableQuarterOnQuarterChange(month) {
        return MonthlyTimeFormat.formatFloat(this.getQuarterOnQuarterChange(month));
    }

    getDisplayableMonthOnMonthChange(month) {
        return MonthlyTimeFormat.formatFloat(this.getMonthOnMonthChange(month));
    }

    getQuarterlyItemCount(quarter) {
        var quarterStartMonth = 1 + (quarter * 3);
        var quarterItemCount = 0;
        for (var i = quarterStartMonth; i <= quarterStartMonth + 2; ++i) {
            quarterItemCount += this.itemsPerMonth[i].length;
        }
        return quarterItemCount;
    }
}

/**
 * Keeps track of items in a calendar-like order (years & months).
 * epochFunction must return the date of the given item as unix epoch.
 */
export default class DatedList {

    constructor(items, epochFunction) {
        this.itemsPerYear = {};
        this.minDate = Number.MAX_VALUE;
        this.maxDate = Number.MIN_VALUE;
        this.monthsInYear = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
        this.epochFunction = epochFunction;

        for (var i = 0; i < items.length; ++i) {
            this.push(items[i]);
        }
    }

    push(item) {
        var unixEpoch = this.epochFunction(item);
        var year = moment(unixEpoch).year();

        this.minDate = Math.min(this.minDate, unixEpoch);
        this.maxDate = Math.max(this.maxDate, unixEpoch);

        if (!this.itemsPerYear[year]) {
            this.itemsPerYear[year] = new YearlyItemList(this.epochFunction);
        }
        this.itemsPerYear[year].push(item);
    }

    getActiveYears() {
        var years = Object.keys(this.itemsPerYear);
        years.sort(function(l, r) {
            return (l > r) ? -1 : ((l < r) ? 1 : 0);
        });
        return years;
    }

    isDateWithinRange(year, month) {
        if (this.minDate == Number.MAX_VALUE || this.maxDate == Number.MIN_VALUE) {
            return false;
        }
        var startMoment = moment(this.minDate);
        var endMoment = moment(this.maxDate);

        var dateAtStartOfMonth = moment({ 'year': year, 'month': month - 1 }).startOf('month');
        var dateAtEndOfMonth = moment({ 'year': year, 'month': month - 1 }).endOf('month');

        return !this.isInFuture(year, month)
            && startMoment.isBefore(dateAtEndOfMonth)
            && endMoment.isAfter(dateAtStartOfMonth);
    }

    isInFuture(year, month) {
        var timeToTest = moment({ 'year': year, 'month': month - 1});
        var now = moment();
        return timeToTest.isAfter(now);
    }

    getDisplayableMonthOnMonthChange(year, month) {
        if (!this.isDateWithinRange(year, month)) {
            return '';
        }

        var items = this.itemsPerYear[year];
        if (!items) {
            return MonthlyTimeFormat.formatFloat(Number.NaN);
        }

        if (month > 1) {
            return items.getDisplayableMonthOnMonthChange(month);
        }

        var prevYearItems = this.itemsPerYear[year - 1];
        if (!prevYearItems) {
            return MonthlyTimeFormat.formatFloat(Number.NaN);
        } else {
            var itemsForLastMonthOfPrevYear = prevYearItems.getMonthlyItemCount(12);
            var itemsForFirstMonth = items.getMonthlyItemCount(1);
            return MonthlyTimeFormat.formatRateOfChange(itemsForLastMonthOfPrevYear, itemsForFirstMonth);
        }
    }

    getDisplayableQuarterOnQuarterChange(year, month) {
        if (!this.isDateWithinRange(year, month)) {
            return '';
        }
        var items = this.itemsPerYear[year];
        if (!items) {
            return MonthlyTimeFormat.formatFloat(Number.NaN);
        }

        var quarter = MonthlyTimeFormat.monthToQuarter(month);
        if (quarter > 0) {
            return items.getDisplayableQuarterOnQuarterChange(month);
        }

        var prevYearItems = this.itemsPerYear[year - 1];
        if (!prevYearItems) {
            return MonthlyTimeFormat.formatFloat(Number.NaN);
        } else {
            var itemsForLastQuarterOfPrevYear = prevYearItems.getQuarterlyItemCount(3);
            var itemsForFirstQuarter = items.getQuarterlyItemCount(0);
            return MonthlyTimeFormat.formatRateOfChange(itemsForLastQuarterOfPrevYear, itemsForFirstQuarter);
        }
    }

    getPrintableMonthlyItemCount(year, month) {
        if (!this.isDateWithinRange(year, month)) {
            return '';
        }
        var items = this.itemsPerYear[year];
        if (!items) {
            return '0';
        } else {
            return items.getMonthlyItemCount(month).toString();
        }
    }
}