import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const langs = ['uk', 'de', 'en', 'ru']
const locales = Object.fromEntries(langs.map(lang => [
  lang, JSON.parse(fs.readFileSync(path.join(root, 'src/i18n/locales', lang + '.json'), 'utf8')),
]))

function flatten(obj, prefix = '') {
  return Object.entries(obj).flatMap(([key, value]) => {
    const dotted = prefix ? prefix + '.' + key : key
    return value !== null && typeof value === 'object' && !Array.isArray(value)
      ? flatten(value, dotted)
      : [dotted]
  })
}

const keys = Object.fromEntries(langs.map(lang => [lang, new Set(flatten(locales[lang]))]))
const union = new Set(langs.flatMap(lang => [...keys[lang]]))
const languageSpecificPlural = /_(zero|one|two|few|many|other)$/
let failed = false

for (const lang of langs) {
  const missing = [...union].filter(key => !keys[lang].has(key) &&
    // Languages have different plural categories: i18next fallback handles them.
    !languageSpecificPlural.test(key))
  if (missing.length) {
    failed = true
    console.error(lang + ': missing translation keys: ' + missing.join(', '))
  }
}
console.log('Locale keys checked: ' + langs.map(lang => lang + '=' + keys[lang].size).join(', '))

function listJsx(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    const location = path.join(dir, entry.name)
    if (entry.isDirectory()) return listJsx(location)
    return entry.name.endsWith('.jsx') ? [location] : []
  })
}

// Advisory only: this inventories legacy hardcoded Cyrillic, including task names,
// API messages and comments. Review the actual JSX context before replacing strings.
// Keys parity above is the strict CI gate.
const outstanding = []
for (const file of listJsx(path.join(root, 'src'))) {
  const lines = fs.readFileSync(file, 'utf8').split('\n')
  const rows = lines.flatMap((line, index) =>
    /[А-Яа-яІіЇїЄєҐґ]/.test(line) &&
    !/^\s*\/\//.test(line) ? [index + 1] : [])
  if (rows.length) outstanding.push({ file: path.relative(root, file), count: rows.length, sample: rows.slice(0, 5) })
}
if (outstanding.length) {
  console.log('Untranslated-literal audit candidates (nonblocking; review before changing):')
  for (const item of outstanding.sort((a, b) => b.count - a.count)) {
    console.log('  ' + item.file + ': ' + item.count + ' candidate lines; e.g. ' + item.sample.join(', '))
  }
}
if (failed) process.exitCode = 1
