export function formatPrintableDuration(durationMsec) {
    var durationInSecs = parseInt(durationMsec / 1000);
    var days = parseInt(durationInSecs / (60 * 60 * 24));
    var hours = parseInt(durationInSecs / (60 * 60) % 24);
    var minutes = parseInt(durationInSecs / (60) % 60);

    var daysPart = days > 0 ? `${days}d ` : '';
    var hoursPart = hours > 0 ? `${hours}h ` : '';
    var minPart = minutes > 0 ? `${minutes}min` : '';
    if (!days && !hours && minutes == 0 && durationInSecs > 0) {
        minPart = '< 1min';
    }

    if (durationInSecs > 0) {
        return daysPart + hoursPart + minPart;
    } else {
        return '\u2013';
    }
}