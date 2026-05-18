module.exports = {
  parser: require('postcss-comment'),
  plugins: {
    'postcss-import': {},
    'autoprefixer': {
      remove: false
    },
    '@dcloudio/vue-cli-plugin-uni/packages/postcss': {}
  }
}
