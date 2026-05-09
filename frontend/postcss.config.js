module.exports = {
  parser: require('postcss-comment'),
  plugins: [
    require('autoprefixer')()
  ]
}
