import React from 'react';

export default class ClearFloat extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const clearStyle = {
            clear: 'both'
        };
        return (
            <div style={clearStyle}></div>
        );
    }
}

ClearFloat.displayName = 'ClearFloat';
