import './PageFooter.scss';

import ClearFloat from './ClearFloat';

import React from 'react';
import moment from 'moment';

export default class PageFooter extends React.Component {
    constructor(props) {
        super(props);
    }

    renderTimestamp(timestamp) {
        if (!timestamp) {
            return '\u2013';
        } else {
            return moment(timestamp).format('YYYY-MM-DD hh:mm:ss');
        }
    }

    render() {
        return (
            <footer>
                <div className="footerContent">
                    <div className="footerLeft">
                        <a href="https://github.com/holmari/gerritstats">Github</a>
                    </div>
                    <div className="footerRight">
                        Generated on {this.renderTimestamp(this.props.datasetOverview['generatedDate'])}
                    </div>
                    <ClearFloat />
                </div>
            </footer>
        );
    }
}

PageFooter.displayName = 'PageFooter';

PageFooter.defaultProps = {
    datasetOverview: {
        generatedDate: 0
    }
};
