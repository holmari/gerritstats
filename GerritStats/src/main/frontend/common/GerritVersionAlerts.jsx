import React from 'react';

import {Alert} from 'react-bootstrap';

// show this alert just once for the whole app; it'll come back on refresh.
var alertShown = false;

export default class GerritVersionAlerts extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            alertVisible: !this.isGerritVersionAtLeast(2, 9) && !alertShown
        };
    }

    // Version 3.1 must pass the "atLeast" test against 2.9 even tho 1 < 9.
    isGerritVersionAtLeast(major, minor) {
        var gerritVersion = this.props.datasetOverview['gerritVersion'] || {};
        return gerritVersion['major'] > major || 
            (gerritVersion['major'] == major && gerritVersion['minor'] >= minor);
    }

    isGerritVersionUnknown() {
        var gerritVersion = this.props.datasetOverview['gerritVersion'] || {};
        return gerritVersion['major'] === -1
            && gerritVersion['minor'] === -1
            && gerritVersion['patch'] === -1;
    }

    getPrintableGerritVersion() {
        var gerritVersion = this.props.datasetOverview['gerritVersion'] || {};
        return gerritVersion['major'] + '.'
             + gerritVersion['minor'] + '.'
             + gerritVersion['patch'];
    }

    handleAlertDismiss() {
        alertShown = true;
        this.setState({
            alertVisible: false
        });
    }

    render() {
        if (!this.state.alertVisible) {
            return null;
        }

        if (this.isGerritVersionUnknown()) {
            return (
                <Alert bsStyle="warning" onDismiss={this.handleAlertDismiss.bind(this)}>
                    <strong>Unknown Gerrit version warning</strong>:
                    Some or all of this data has been generated with data from an unknown Gerrit version.
                    Some data is not included in the sources.
                    Rerun GerritDownloader and GerritStats to get rid of this message.
                </Alert>
            );
        } else if (!this.isGerritVersionAtLeast(2, 9)) {
            const gerritVersion = this.getPrintableGerritVersion();
            return (
                <Alert bsStyle="warning" onDismiss={this.handleAlertDismiss.bind(this)}>
                    <strong>Old Gerrit version warning</strong>:
                    Some or all of this data has been generated with data from an old Gerrit version ({gerritVersion}).
                    Versions prior to 2.9 do not provide enough information on who was added as a reviewer,
                    so much of the data will be incorrect.
                    Update Gerrit to get better statistics!
                </Alert>
            );
        } else {
            return null;
        }
    }
}

GerritVersionAlerts.displayName = 'GerritVersionAlerts';

GerritVersionAlerts.defaultProps = {
    datasetOverview: {}
};