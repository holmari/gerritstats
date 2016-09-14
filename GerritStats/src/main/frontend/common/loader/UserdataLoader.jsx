import GlobalJavascriptLoader from './GlobalJavascriptLoader';

/**
 * Loads the userdata .js file for a given user.
 *
 * The data is returned in the callback as an immutable JS object.
 *
 */
export default class UserdataLoader extends GlobalJavascriptLoader {
    constructor() {
        super();
        // This object is global so that data loading is compatible
        // with the previous implementation.
        window.userdata = [];
    }

    /**
     * Loads the userdata .js file for the given user's unique identifier.
     */
    load(userIdentifier, onLoadCallback) {
        this.loadJavascriptFile(`data/users/${userIdentifier}.js`, () =>
            onLoadCallback(window.userdata[userIdentifier])
        );
    }
}