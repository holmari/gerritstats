/**
 * Keeps track of users selected into the analysis.
 */
class SelectedUsers {

    constructor(overviewUserdata, primaryStorageKey) {
        this.users = {};
        this.groupKey = 'selectedUsers';
        this.storageKey = primaryStorageKey + '.' + this.groupKey;

        this.isUserSelected = function(userDataOrIdentifier) {
            var identifier = typeof(userDataOrIdentifier) == 'string'
                ? userDataOrIdentifier
                : userDataOrIdentifier.identifier;
            return this.users[identifier] === '1';
        }

        this.users = this.loadFromLocalStorage();
    }

    setUserSelected(userIdentifier, isUserSelected) {
        if (isUserSelected) {
            this.users[userIdentifier] = '1';
        } else {
            delete this.users[userIdentifier];
        }
    }

    writeToStorage() {
        localStorage.setItem(this.storageKey, JSON.stringify(this.users));
    }

    getSelectedUserCount() {
        return Object.keys(this.users).length;
    }

    getTotalUserCount() {
        return overviewUserdata.length;
    }

    getPrintableUserCount() {
        return this.getSelectedUserCount() + ' / ' + this.getTotalUserCount();
    }

    loadFromLocalStorage() {
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
}
