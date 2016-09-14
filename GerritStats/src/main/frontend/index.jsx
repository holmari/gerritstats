import 'bootstrap/dist/css/bootstrap.css';
import './style/base.scss';

import React from 'react';
import ReactDOM from 'react-dom';
import {Router, Route, hashHistory} from 'react-router'

import GlobalJavascriptLoader from './common/loader/GlobalJavascriptLoader';
import PageFooter from './common/PageFooter';
import SelectedUsers from './common/model/SelectedUsers';

import ids from './data/ids';
import OverviewPage from './overview/OverviewPage';
import ProfilePage from './profile/ProfilePage';

// Ids are stored globally, since they're so frequently used and repeated.
window.ids = ids;

// The dataset overview is also globally stored
window.datasetOverview = {};

var currentSelection = {
    selectedUsers: {}
};

var jsLoader = new GlobalJavascriptLoader();
function onDatasetOverviewLoaded() {
    const storageKey = window.datasetOverview.hashCode;
    currentSelection.selectedUsers = SelectedUsers.fromLocalStorage(storageKey, ids);
    renderPage();
}
jsLoader.loadJavascriptFile('./data/datasetOverview.js', onDatasetOverviewLoaded);

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
                    datasetOverview={datasetOverview}
                    currentSelection={currentSelection}
                    onCurrentSelectionChanged={onCurrentSelectionChanged}
                />
                <Route path='/profile/:identifier' component={ProfilePage}
                    datasetOverview={datasetOverview}
                    currentSelection={currentSelection}
                />
            </Router>
            <PageFooter datasetOverview={datasetOverview} />
        </div>,
        document.getElementById('app')
    );
}
