import './ReviewCommentsPerDayView.scss';

import React from 'react';

import D3BaseComponent from '../../common/charts/D3BaseComponent';
import GerritUserdata from '../../common/model/GerritUserdata';
import SelectedUsers from '../../common/model/SelectedUsers';

import FrequencyTable from './FrequencyTable';

/**
  * Processes the data into the following format:
  * [{date: "2015-06-19", "count": 1},
  *  {date: "2015-08-10", "count": 1},
  *  {date: "2015-08-14", "count": 1},
  *  {date: "2015-08-15", "count": 3},
  *   ...
  * ];
*/
function groupReviewCommentsByDate(comments) {
    var frequencies = comments.reduce(function (previousValue, currentValue) {
        var date = currentValue.date;
        if (typeof previousValue[date] == 'undefined') {
            previousValue[date] = 1;
        } else {
            previousValue[date] += 1;
        }
        return previousValue;
    }, {});

    var data = Object.keys(frequencies).map(function(date) {
        return {
            date: new Date(date),
            count: frequencies[date]
        };
    });
    data.sort(function(left, right) {
        return (left.date <  right.date) ? -1 : 1;
    });
    return data;
}

export default class ReviewCommentsPerDayView extends D3BaseComponent {
    constructor(props) {
        super(props);
    }

    componentDidMount() {
        const commentGroupingByDate = groupReviewCommentsByDate(this.props.userdata.getReviewCommentDates());
        new FrequencyTable(this.refs.graphContent, this.props.userdata, commentGroupingByDate, {
            height: 550,
        });
    }
}

ReviewCommentsPerDayView.displayName = 'ReviewCommentsPerDayView';

ReviewCommentsPerDayView.propTypes = {
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    userdata: React.PropTypes.instanceOf(GerritUserdata).isRequired,
};
