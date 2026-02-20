const { merge } = require('webpack-merge')
const common = require('./webpack.common.js')

module.exports = [merge(common.chromeConfig, {
    mode: 'production',
}), merge(common.firefoxConfig, {
    mode: 'production',
})]