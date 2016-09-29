import './NumberPanel.scss';

import classnames from 'classnames';
import React from 'react';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';

import {DECIMAL_PRECISION} from '../../common/model/GerritUserdata';

export default class NumberPanel extends React.Component {
    constructor(props) {
        super(props);
    }

    getPrintableNumber(value) {
        if (Number.isNaN(value)) {
            return '\u2013';
        } else if (!Number.isFinite(value)) {
            return '\u221e';
        } else if (!Number.isInteger(value)) {
            return value.toFixed(DECIMAL_PRECISION);
        } else {
            return value;
        }
    }

    render() {
        const tooltip = (
            <Tooltip id="tooltip">{this.props.tooltip}</Tooltip>
        );

        var value = this.props.value;
        if (typeof this.props.value === 'number') {
            value = this.getPrintableNumber(value);
        }

        const className = classnames(
            'numberPanel',
            {'wide': this.props.size == 'wide'},
            {'xWide': this.props.size == 'xWide'}
        );
        return (
            <OverlayTrigger placement='bottom' overlay={tooltip}>
                <div className={className} data-toggle='tooltip'>
                <div className='value'>{value}</div>
                    <div>&nbsp;<div className='title'>{this.props.title}</div></div>
                </div>
            </OverlayTrigger>
        );
    }
}

NumberPanel.displayName = 'NumberPanel';

NumberPanel.defaultProps = {
    size: 'normal'
};

NumberPanel.propTypes = {
    title: React.PropTypes.string.isRequired,
    value: React.PropTypes.any.isRequired,
    tooltip: React.PropTypes.string,
    size: React.PropTypes.oneOf([
        'normal', 'wide', 'xWide'
    ]).isRequired,
};