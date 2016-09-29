import 'bootstrap/dist/css/bootstrap.css';
import './style/base.scss';

import React from 'react'; // eslint-disable-line no-unused-vars
import ReactDOM from 'react-dom';
import {Router, Route, hashHistory} from 'react-router';

import GlobalJavascriptLoader from './common/loader/GlobalJavascriptLoader';
import PageFooter from './common/PageFooter';
import SelectedUsers from './common/model/SelectedUsers';

import OverviewPage from './overview/OverviewPage';
import ProfilePage from './profile/ProfilePage';

var jsLoader = new GlobalJavascriptLoader();

function onDatasetOverviewLoaded() {
    const storageKey = window.datasetOverview.hashCode;
    currentSelection.selectedUsers = SelectedUsers.fromLocalStorage(storageKey, window.ids);
    renderPage();
}

function onIdsLoaded() {
    jsLoader.loadJavascriptFile('./data/datasetOverview.js', onDatasetOverviewLoaded);
}

// Ids are stored globally, since they're so frequently used and repeated.
jsLoader.loadJavascriptFile('./data/ids.js', onIdsLoaded);

// The dataset overview is also globally stored
window.datasetOverview = {};

var currentSelection = {
    selectedUsers: {}
};

function onCurrentSelectionChanged(newSelection) {
    currentSelection.selectedUsers = newSelection.selectedUsers;
    currentSelection.selectedUsers.writeToStorage();
}

// Called when preconditions are complete (data is loaded).
function renderPage() {
    ReactDOM.render(
        <div>
            <Router history={hashHistory}>
                <Route path='/' component={OverviewPage}
                    datasetOverview={window.datasetOverview}
                    currentSelection={currentSelection}
                    onCurrentSelectionChanged={onCurrentSelectionChanged}
                />
                <Route path='/profile/:identifier' component={ProfilePage}
                    datasetOverview={window.datasetOverview}
                    currentSelection={currentSelection}
                />
            </Router>
            <PageFooter datasetOverview={window.datasetOverview} />
        </div>,
        document.getElementById('app')
    );
}
