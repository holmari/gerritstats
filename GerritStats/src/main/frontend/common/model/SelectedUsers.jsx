import {Set} from 'immutable';

/**
 * Keeps track of users selected into the analysis.
 */
export default class SelectedUsers {

    static _getStorageKey(primaryKey) {
        return primaryKey + '.selectedUsers';
    }

    // Create a users set with all users selected.
    static _selectAllUsers(ids) {
        var users = Set();
        for (let key in ids) {
            users = users.add(ids[key].identifier);
        }
        return users;
    }

    static fromLocalStorage(primaryStorageKey, ids) {
        const key = SelectedUsers._getStorageKey(primaryStorageKey);
        var users = JSON.parse(localStorage.getItem(key));
        if (users === null) {
            users = SelectedUsers._selectAllUsers(ids);
        } else {
            users = Set.of(...users);
        }
        return new SelectedUsers(primaryStorageKey, ids, users);
    }

    constructor(primaryStorageKey, ids, users) {
        this.ids = ids;
        this.users = users;
        this.primaryStorageKey = primaryStorageKey;
    }

    equals(otherSelection) {
        return this.users.equals(otherSelection.users);
    }

    isUserSelected(userDataOrIdentifier) {
        var identifier = typeof(userDataOrIdentifier) == 'string'
            ? userDataOrIdentifier
            : userDataOrIdentifier['identifier'];
        return this.users.has(identifier);
    }

    isAllUsersSelected() {
        return this.getSelectedUserCount() == this.getTotalUserCount();
    }

    toggleSelection(userIdentifier) {
        const isUserSelected = this.isUserSelected(userIdentifier);
        return this.setUserSelected(userIdentifier, !isUserSelected);
    }

    setUserSelected(userIdentifier, isUserSelected) {
        var newUsers = Set();
        if (isUserSelected) {
            newUsers = this.users.add(userIdentifier);
        } else {
            newUsers = this.users.delete(userIdentifier);
        }
        return new SelectedUsers(this.primaryStorageKey, this.ids, newUsers);
    }

    selectAll() {
        const newUsers = SelectedUsers._selectAllUsers(this.ids);
        return new SelectedUsers(this.primaryStorageKey, this.ids, newUsers);
    }

    selectNone() {
        return new SelectedUsers(this.primaryStorageKey, this.ids, Set());
    }

    writeToStorage() {
        const key = SelectedUsers._getStorageKey(this.primaryStorageKey);
        localStorage.setItem(key, JSON.stringify(this.users));
    }

    getSelectedUserCount() {
        return this.users.size;
    }

    getTotalUserCount() {
        return Object.keys(this.ids).length;
    }
}
