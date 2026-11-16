import test from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import {validateSetupCatalog, selectSetupMatching} from '../src/utils/setupCatalogSelection.js'

// The same bundled source that Backend imports. No speculative or duplicated
// 142-entry fixture in frontend. This is a pure, non-destructive contract test.
const catalog = JSON.parse(readFileSync(new URL('../../src/main/resources/templates/catalog-v1.json',import.meta.url),'utf8'))
test('V49 accepts the authoritative 142-template / six-pack catalog with real memberships', () => {
  assert.strictEqual(validateSetupCatalog(catalog),catalog)
  assert.equal(catalog.templates.length,142)
  assert.equal(catalog.templates.filter(t=>t.kind==='TASK').length,110)
  assert.equal(catalog.templates.filter(t=>t.kind==='REWARD').length,32)
  assert.equal(catalog.packs.length,6)
  const packed = new Set(catalog.packs.flatMap(p=>p.templateCodes))
  assert.equal(packed.size,99)
  assert.equal(catalog.templates.filter(t=>!packed.has(t.code)).length,43)
})
test('V49 overlapping packs and category selections deduplicate deterministically', () => {
  const codes = catalog.templates.map(t=>t.code)
  let selected = []
  for(const pack of catalog.packs) selected = selectSetupMatching(selected,pack.templateCodes,true,codes)
  assert.equal(selected.length,99)
  assert.equal(new Set(selected).size,99)
  const outside = codes.filter(code=>!selected.includes(code))
  selected = selectSetupMatching(selected,outside,true,codes)
  assert.equal(selected.length,142)
  selected = selectSetupMatching(selected,catalog.packs[0].templateCodes,false,codes)
  assert.equal(selected.length,142-catalog.packs[0].templateCodes.length)
  assert.equal(new Set(selected).size,selected.length)
})
test('V49 never accepts invalid or foreign codes from malformed catalog or selection',()=>{
  for(const invalid of [
    {...catalog,templates:[...catalog.templates,catalog.templates[0]]},
    {...catalog,templates:[...catalog.templates,{code:'bad',kind:'FOREIGN'}]},
    {...catalog,packs:[...catalog.packs,{code:'foreign',templateCodes:['FOREIGN']}]},
    {...catalog,packs:[...catalog.packs,catalog.packs[0]]},
    {...catalog,packs:[{...catalog.packs[0],templateCodes:['x','x']},...catalog.packs.slice(1)]},
  ]) assert.throws(()=>validateSetupCatalog(invalid))
  assert.throws(()=>selectSetupMatching([],['FOREIGN'],true,catalog.templates.map(t=>t.code)))
})
