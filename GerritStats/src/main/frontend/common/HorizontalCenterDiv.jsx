import './HorizontalCenterDiv.scss';

import React from 'react';

export default class HorizontalCenterDiv extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className='horizontalCenterDiv'>
                {this.props.children}
            </div>
        );
    }
}

HorizontalCenterDiv.displayName = 'HorizontalCenterDiv';
