import './PageHeader.scss';

import React from 'react';
import {Link} from 'react-router';

import DatasetOverviewWidget from './DatasetOverviewWidget';
import SelectedUsers from '../model/SelectedUsers';

export default class PageHeader extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedUsers: this.props.selectedUsers
        };
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            selectedUsers: nextProps.selectedUsers
        });
    }

    renderMainTitle() {
        return this.props.mainTitle;
    }

    renderBackButton() {
        if (this.props.showBackButton) {
            return (
                <Link to='/'><img src={require('./img/ic_back.png')} /></Link>
            );
        }
    }

    render() {
        return (
            <header>
                <DatasetOverviewWidget
                    datasetOverview={this.props.datasetOverview}
                    selectedUsers={this.state.selectedUsers}
                />
                {this.renderBackButton()}
                <div style={{display:'inline-block'}}>
                    <h1 className='pageTitle'>{this.renderMainTitle()}</h1>
                    <div className="subtitleH1">{this.props.subtitle}</div>
                </div>
            </header>
        );
    }
}

PageHeader.displayName = 'PageHeader';

PageHeader.defaultProps = {
    datasetOverview: {
        filenames: []
    },
    mainTitle: 'GerritStats',
    subtitle: null,
    showBackButton: false,
};

PageHeader.propTypes = {
    datasetOverview: React.PropTypes.shape({
        filenames: React.PropTypes.array.isRequired
    }),
    selectedUsers: React.PropTypes.instanceOf(SelectedUsers).isRequired,
    mainTitle: React.PropTypes.string,
    subtitle: React.PropTypes.string,
    showBackButton: React.PropTypes.bool,
};