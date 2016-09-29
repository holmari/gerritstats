import * as d3 from 'd3';

/**
 * Returns a default time formatting function for time, often used x axes of graphs.
 */
export function getDefaultXAxisTimeFormat() {
    return d3.time.format.multi([
        ['.%L', (d) => d.getMilliseconds() ],
        [':%S', (d) => d.getSeconds() ],
        ['%H:%M', (d) => d.getMinutes() ],
        ['%H:%M', (d) => d.getHours() ],
        ['%a %d', (d) => d.getDay() && d.getDate() != 1 ],
        ['%b %d', (d) => d.getDate() != 1 ],
        ['%b', (d) => d.getMonth() ],
        ['%Y', () => true ]
    ]);
}