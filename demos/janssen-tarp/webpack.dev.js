const { merge } = require('webpack-merge')
const common = require('./webpack.common.js')

module.exports = [merge(common.chromeConfig, {
    mode: 'development',
    devtool: 'cheap-module-source-map',
}), merge(common.firefoxConfig, {
    mode: 'development',
    devtool: 'cheap-module-source-map',
})]