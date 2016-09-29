import './Panel.scss';

import classnames from 'classnames';
import React from 'react';

export default class Panel extends React.Component {
    constructor(props) {
        super(props);
    }

    getClassNames() {
        return classnames(
            'panel',
            {'thirdWidth': this.props.size == 'third'},
            {'twoThirdsWidth': this.props.size == 'twoThirds'},
            {'halfWidth': this.props.size == 'half'},
            {'fullWidth': this.props.size == 'full'},
            {'flexWidth': this.props.size == 'flex'},
            {'fourthWidth': this.props.size == 'fourth'},
            {'threeFourthsWidth': this.props.size == 'threeFourths'}
        );
    }

    render() {
        const className = this.getClassNames();
        return (
            <div className={className}>
                <h2>{this.props.title}</h2>
                {this.props.children}
            </div>
        );
    }
}

Panel.displayName = 'Panel';

Panel.defaultProps = {
    size: 'normal'
};

Panel.propTypes = {
    title: React.PropTypes.string.isRequired,
    size: React.PropTypes.oneOf([
        'normal', 'third', 'twoThirds',
        'half', 'full', 'flex',
        'fourth', 'threeFourths'
    ]).isRequired,
};