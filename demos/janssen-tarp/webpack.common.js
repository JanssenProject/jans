const path = require('path');
const CopyPlugin = require('copy-webpack-plugin');
const HtmlPlugin = require('html-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const autoprefixer = require('autoprefixer')
const { merge } = require('webpack-merge')

const commonConfig = {
    entry: {
        popup: path.resolve('src/popup/index.tsx'),
        options: path.resolve('src/options/index.tsx'),
        background: path.resolve('src/background/background.ts'),
    },
    module: {
        rules: [
            {
                use: 'ts-loader',
                test: /\.tsx?$/,
                exclude: /node_modules/,
            },
            {
                test: /\.css$/i,
                use: [
                    'style-loader',
                    {
                        loader: 'css-loader',
                        options: {
                            importLoaders: 1,
                        },
                    },
                    {
                        loader: 'postcss-loader', // postcss loader needed for tailwindcss
                        options: {
                            postcssOptions: {
                                ident: 'postcss',
                                plugins: [autoprefixer],
                            },
                        },
                    },
                ],
            },
            {
                type: 'assets/resource',
                test: /\.(png|jpg|jpeg|gif|woff|woff2|tff|eot|svg)$/,
            },
        ]
    },
    resolve: {
        extensions: ['.tsx', '.js', '.ts']
    },
    optimization: {
        splitChunks: {
            chunks: 'all',
        }
    }
}
const assetArr = [{
    from: path.resolve('src/static/icon.png')
},
{
    from: path.resolve('src/static/logo.jpg')
},
{
    from: path.resolve('src/static/tarpDocs1.png')
},
{
    from: path.resolve('src/static/tarpDocs2.png')
},
{
    from: path.resolve('src/static/tarpDocs3.png')
},
{
    from: path.resolve('src/static/tarpDocs4.png')
},
{
    from: path.resolve('src/static/tarpDocs5.png')
},
{
    from: path.resolve('src/static/tarpDocs6.png')
}];

const chromeConfig = merge(commonConfig, {
    "plugins": [
        new CleanWebpackPlugin({
            cleanStaleWebpackAssets: false
        }),
        new CopyPlugin({
            patterns: [...assetArr, { from: path.resolve('src/static/chrome')}].map(obj => ({ ...obj, to: path.resolve('dist/chrome') }))
        }),
        ...getHtmlPlugins([
            'popup',
            'options',
            'newTab'
        ])
    ],
    output: {
        filename: '[name].js',
        path: path.join(__dirname, 'dist/chrome')
    }
})

const firefoxConfig = merge(commonConfig, {
    "plugins": [
        new CleanWebpackPlugin({
            cleanStaleWebpackAssets: false
        }),
        new CopyPlugin({
            patterns: [...assetArr, { from: path.resolve('src/static/firefox')}].map(obj => ({ ...obj, to: path.resolve('dist/firefox') }))
        }),
        ...getHtmlPlugins([
            'popup',
            'options',
            'newTab'
        ])
    ],
    output: {
        filename: '[name].js',
        path: path.join(__dirname, 'dist/firefox')
    }
})

function getHtmlPlugins(chunks) {
    return chunks.map(chunk => new HtmlPlugin({
        title: 'Tarp - Janssen Project',
        filename: `${chunk}.html`,
        chunks: [chunk]
    }))
}

module.exports = { chromeConfig, firefoxConfig };