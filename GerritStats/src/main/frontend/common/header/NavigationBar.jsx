import './NavigationBar.scss';

import {List} from 'immutable';
import React from 'react';
import {Nav, NavItem} from 'react-bootstrap';

export default class NavigationBar extends React.Component {
    constructor(props) {
        super(props);
    }

    renderElement(element) {
        const key = element.get('key');
        const displayName = element.get('displayName');

        return (
            <NavItem key={key} eventKey={key}>{displayName}</NavItem>
        );
    }

    renderElements() {
        if (!this.props.elements) {
            return;
        }

        var renderedElements = [];
        this.props.elements.forEach(element =>
            renderedElements.push(this.renderElement(element))
        );
        return renderedElements;
    }

    render() {
        return (
            <nav>
                <Nav bsStyle='pills' onSelect={this.props.onSelectedListener}>
                    {this.renderElements()}
                </Nav>
            </nav>
        );
    }
}

NavigationBar.displayName = 'NavigationBar';

NavigationBar.propTypes = {
    elements: React.PropTypes.instanceOf(List).isRequired,
    onSelectedListener: React.PropTypes.func.isRequired
};