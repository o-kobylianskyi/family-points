import test from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync,readdirSync} from 'node:fs'
import {join} from 'node:path'
import {fileURLToPath} from 'node:url'

const root=fileURLToPath(new URL('../src/',import.meta.url))
function sources(dir){
  return readdirSync(dir,{withFileTypes:true}).flatMap(entry=>{
    const path=join(dir,entry.name)
    if(entry.isDirectory()) return sources(path)
    return /\.jsx?$/.test(entry.name)?[path]:[]
  })
}
test('Settings regression: no boolean variable followed by parenthesized expression without semicolon',()=>{
  const pattern=/\b(?:let|const|var)\s+[A-Za-z_$][\w$]*\s*=\s*(?:true|false)\s*(?:\/\/[^\n]*)?\r?\n\s*\(/g
  for(const path of sources(root)){
    const source=readFileSync(path,'utf8')
    assert.equal(pattern.test(source),false,`Potential automatic semicolon insertion boolean-call in ${path}`)
    pattern.lastIndex=0
  }
})
test('Settings currency loader does not invoke the alive boolean as a function',()=>{
  const source=readFileSync(new URL('../src/components/CurrencyFormsSettings.jsx',import.meta.url),'utf8')
  assert.match(source,/let alive = true;\s*\n\s*\(canManage \? getAllPointTypes/)
})
