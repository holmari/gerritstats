/**
 * Keeps track of users selected into the analysis.
 */
function SelectedUsers(overviewUserdata) {

    this.users = {};
    this.storageKey = 'selectedUsers';

    this.isUserSelected = function(userDataOrIdentifier) {
        var identifier = typeof(userDataOrIdentifier) == 'string'
            ? userDataOrIdentifier
            : userDataOrIdentifier.identifier;
        return this.users[identifier] === '1';
    }

    this.setUserSelected = function(userIdentifier, isUserSelected) {
        if (isUserSelected) {
            this.users[userIdentifier] = '1';
        } else {
            delete this.users[userIdentifier];
        }
    }

    this.writeToStorage = function() {
        localStorage.setItem(this.storageKey, JSON.stringify(this.users));
    }

    this.getSelectedUserCount = function() {
        return Object.keys(this.users).length;
    }

    this.getTotalUserCount = function() {
        return overviewUserdata.length;
    }

    this.getPrintableUserCount = function() {
        return this.getSelectedUserCount() + ' / ' + this.getTotalUserCount();
    }

    this.loadFromLocalStorage = function() {
        var users = JSON.parse(localStorage.getItem(this.storageKey));
        if (users === null) {
            users = {};
            // select all by default
            overviewUserdata.forEach(function(item) {
                users[item.identifier] = '1';
            });
        }
        return users;
    }

    this.initialize = function(overviewUserdata) {
        this.users = this.loadFromLocalStorage();
    }

    this.initialize(overviewUserdata);
}
