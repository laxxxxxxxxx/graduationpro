const fs = require('fs')
const path = require('path')

const patchFile = (filePath, replacements, label) => {
  if (!fs.existsSync(filePath)) {
    console.warn(`[${label}] file not found, skipped`)
    return
  }

  let source = fs.readFileSync(filePath, 'utf8')
  let changed = false

  replacements.forEach(({ original, patched }) => {
    if (!source.includes(original)) {
      return
    }

    source = source.split(original).join(patched)
    changed = true
  })

  if (changed) {
    fs.writeFileSync(filePath, source)
  }
}

const original = "return code + `\\nexport { render, staticRenderFns, recyclableRender, components }`"
const patched = "return code + `\\nvar recyclableRender = typeof recyclableRender === 'undefined' ? null : recyclableRender\\nvar components = typeof components === 'undefined' ? null : components\\nexport { render, staticRenderFns, recyclableRender, components }`"

patchFile(
  path.resolve(__dirname, '../node_modules/@dcloudio/vue-cli-plugin-uni/packages/vue-loader/lib/loaders/templateLoader.js'),
  [{ original, patched }],
  'patch-uni-template-loader'
)

patchFile(
  path.resolve(__dirname, '../node_modules/postcss-urlrewrite/lib/urlrewrite.js'),
  [
    { original: 'util.isArray', patched: 'Array.isArray' },
    { original: '!util.isRegExp( rule.from )', patched: '!(rule.from instanceof RegExp)' }
  ],
  'patch-postcss-urlrewrite'
)
