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

test('Safety plan remount scope includes username fallback used by loader identity',()=>{
  const source=readFileSync(new URL('../src/components/FamilyDataSafetyPlan.jsx',import.meta.url),'utf8')
  const expected='currentUser?.accountId ?? currentUser?.userId ?? currentUser?.id ?? currentUser?.username ?? null'
  assert.ok(source.includes('const account = '+expected),
    'Backup wrapper identity must include username-only fallback')
  assert.ok(source.includes("const key = [account, currentUser?.workspaceId, canManage ? 'manage' : 'read-only'].join(':')"),
    'The component must remount when user, family or permission changes')
  assert.ok(source.includes('const accountScope = '+expected),
    'The backup request scope must use the same account identity as the wrapper key')
})

test('V49 remount identity exactly matches pending finalize marker account scope',()=>{
  const source=readFileSync(new URL('../src/components/InitialFamilySetupReview.jsx',import.meta.url),'utf8')
  const identity='currentUser?.accountId ?? currentUser?.userId ?? currentUser?.id ?? currentUser?.workspaceMemberId ?? currentUser?.username'
  assert.ok(source.includes('const owner = '+identity),
    'React component remount owner must use the same account identity as V49 pending markers')
  assert.ok(source.includes('const accountId = '+identity),
    'V49 pending marker account identity must remain aligned with React wrapper')
})

test('Safety plan does not fetch or display backup metadata without authorized account scope',()=>{
  const source=readFileSync(new URL('../src/components/FamilyDataSafetyPlan.jsx',import.meta.url),'utf8')
  assert.match(source,/if \(!workspaceId \|\| !accountScope \|\| !canManage\) return/)
  assert.match(source,/if \(!canManage \|\| !accountScope \|\| !workspaceId \|\| !selectedSnapshot\) return/)
  assert.match(source,/if \(!canManage \|\| !accountScope \|\| !workspaceId\) return null/)
})
