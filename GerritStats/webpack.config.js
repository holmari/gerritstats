var path = require('path');

module.exports = {
    entry: './src/main/frontend/index.jsx',
    output: {
        path: 'out-html',
        filename: 'bundle.js',
    },
    module: {
        loaders: [
            {
                test: /\.jsx?$/,
                exclude: /node_modules/,
                loader: 'babel',
            },
            {
                test: /\.s?css$/,
                loaders: ['style', 'css', 'sass'],
            },
            {
                test: /\.html/,
                loader: 'html',
            },
            {
                test: /\.(woff|woff2)(\?v=\d+\.\d+\.\d+)?$/,
                loader: 'url?limit=10000&mimetype=application/font-woff'
            },
            {
                test: /\.ttf(\?v=\d+\.\d+\.\d+)?$/,
                loader: 'url?limit=10000&mimetype=application/octet-stream'
            },
            {
                test: /\.eot(\?v=\d+\.\d+\.\d+)?$/,
                loader: 'file'
            },
            {
                test: /\.svg(\?v=\d+\.\d+\.\d+)?$/,
                loader: 'url?limit=10000&mimetype=image/svg+xml'
            },
            {
              test: /\.(png|jpg)$/,
              loader: 'url?limit=8192'
            }
        ],
    },
    resolve: {
        extensions: ['', '.js', '.jsx']
    }
};
