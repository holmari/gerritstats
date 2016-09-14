import './PanelContainer.scss';

import React from 'react';

export default class PanelContainer extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className='panelContainer'>
                {this.props.children}
            </div>
        );
    }
}

PanelContainer.displayName = 'PanelContainer';
