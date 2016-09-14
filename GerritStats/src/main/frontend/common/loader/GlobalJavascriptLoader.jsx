/**
 * Loads a Javascript file and injects it as <script> tag into global scope.
 */
export default class GlobalJavascriptLoader {
    constructor() {
    }

    loadJavascriptFile(filename, onLoadCallback) {
        var element = document.createElement('script');
        element.src = filename;
        element.onload = onLoadCallback;
        document.body.appendChild(element);
    }
}