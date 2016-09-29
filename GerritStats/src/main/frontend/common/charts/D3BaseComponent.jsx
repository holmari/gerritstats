import './D3BaseComponent.scss';

import React from 'react';

/**
 * A trivial mount point for d3 graphs. Do the following:
 *  1) pass this.refs.graphContent to d3.select()
 *  2) create the d3 rendering in componentDidMount()
 */
export default class D3BaseComponent extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className={this.props.className} ref='graphContent' />
        );
    }
}

D3BaseComponent.defaultProps = {
    className: 'graphPanelContent'
};