import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  updateMemberGroup, addGroupMember, removeGroupMember, addChildGroup, removeChildGroup,
  addGroupRole, updateGroupRole, updateGroupMemberRoles,
  getGroupPermissions, addGroupPermission, removeGroupPermission,
  getRoleSets, createRoleSet, updateRoleSet, deleteRoleSet,
} from '../api/memberGroupApi'

const permissionOptions=['TASK_VIEW','TASK_CREATE','TASK_ASSIGN','TASK_MANAGE','TASK_APPROVE','MEMBER_VIEW','MEMBER_MANAGE','GROUP_VIEW','GROUP_MANAGE','POINT_VIEW','POINT_AWARD','POINT_SPEND','SUBGROUP_MANAGE']

export default function GroupEditor({group,groups,members,token,workspaceId,onChanged,onClose}){
 const { t } = useTranslation()
 const [name,setName]=useState(group.name),[description,setDescription]=useState(group.description||''),[showInNavigation,setShowInNavigation]=useState(Boolean(group.showInNavigation))
 const [memberId,setMemberId]=useState(''),[childId,setChildId]=useState(''),[roleSets,setRoleSets]=useState([]),[permissionGrants,setPermissionGrants]=useState([])
 const [showSetManager,setShowSetManager]=useState(false),[newSet,setNewSet]=useState({name:'',description:''}),[editingSet,setEditingSet]=useState(null)
 const [roleEditor,setRoleEditor]=useState(null),[memberRoleEditor,setMemberRoleEditor]=useState(null),[error,setError]=useState(''),[saving,setSaving]=useState(false)

 const reloadAux=async()=>{const [s,g]=await Promise.all([getRoleSets(token,workspaceId),getGroupPermissions(token,workspaceId,group.id)]);setRoleSets(s);setPermissionGrants(g)}
 useEffect(()=>{reloadAux().catch(e=>setError(e.message))},[token,workspaceId,group.id])
 useEffect(()=>{const k=e=>e.key==='Escape'&&(roleEditor?setRoleEditor(null):memberRoleEditor?setMemberRoleEditor(null):showSetManager?setShowSetManager(false):onClose());window.addEventListener('keydown',k);return()=>window.removeEventListener('keydown',k)},[onClose,roleEditor,memberRoleEditor,showSetManager])
 const run=async action=>{try{setError('');await action();await onChanged();await reloadAux();return true}catch(e){setError(e.message||'Помилка збереження');return false}}
 const availableMembers=useMemo(()=>members.filter(m=>!group.members.some(x=>x.memberId===m.id)),[members,group])
 const availableGroups=useMemo(()=>groups.filter(g=>g.id!==group.id&&!group.childGroups.some(x=>x.groupId===g.id)),[groups,group])
 const groupedRoles=useMemo(()=>{const rows=[...roleSets.map(s=>({id:String(s.id),name:s.name,roles:group.roles.filter(r=>r.roleSetId===s.id)})),{id:'none',name:'Без набору',roles:group.roles.filter(r=>!r.roleSetId)}];return rows.filter(x=>x.roles.length)},[roleSets,group.roles])
 const grantsFor=id=>permissionGrants.filter(g=>g.roleId===id)
 const roleName=id=>group.roles.find(r=>r.id===id)?.name||`#${id}`

 async function saveMain(){if(!name.trim())return setError('Назва групи не може бути порожньою.');try{setSaving(true);setError('');await updateMemberGroup(token,workspaceId,group.id,{name:name.trim(),description:description.trim()||null,showInNavigation});await onChanged();onClose()}catch(e){setError(e.message)}finally{setSaving(false)}}
 const openNewRole=()=>setRoleEditor({id:null,name:'',description:'',roleSetId:'',permissions:{}})
 const openRole=role=>{
   const permissions={}
   grantsFor(role.id).forEach(g=>permissions[g.permission]=g.scope)
   setError('')
   setRoleEditor({
     id:role.id,
     name:role.name||'',
     description:role.description||'',
     roleSetId:role.roleSetId||'',
     permissions
   })
 }
 const saveRole=async(event)=>{
   event?.preventDefault?.()
   const sourceRole=roleEditor?.id ? group.roles.find(r=>r.id===roleEditor.id) : null
   const effectiveName=(roleEditor?.name||sourceRole?.name||'').trim()
   if(!effectiveName){
     setError('Вкажіть назву ролі')
     return
   }
   setSaving(true)
   const ok=await run(async()=>{
     let roleId=roleEditor.id
     if(roleId) await updateGroupRole(token,workspaceId,group.id,roleId,{name:effectiveName,description:roleEditor.description||null,roleSetId:roleEditor.roleSetId?Number(roleEditor.roleSetId):null})
     else {
       const updated=await addGroupRole(token,workspaceId,group.id,{name:effectiveName,description:roleEditor.description||null,roleSetId:roleEditor.roleSetId?Number(roleEditor.roleSetId):null})
       roleId=updated.roles.find(r=>r.name===roleEditor.name.trim())?.id
       if(!roleId) throw new Error('Не вдалося визначити ID створеної ролі')
     }
     const existing=grantsFor(roleId)
     for(const grant of existing){
       if(!roleEditor.permissions[grant.permission]||roleEditor.permissions[grant.permission]!==grant.scope){
         await removeGroupPermission(token,workspaceId,group.id,grant.id)
       }
     }
     for(const [permission,scope] of Object.entries(roleEditor.permissions)){
       if(scope&&!existing.some(g=>g.permission===permission&&g.scope===scope)){
         await addGroupPermission(token,workspaceId,group.id,roleId,{permission,scope})
       }
     }
   })
   setSaving(false)
   if(ok)setRoleEditor(null)
 }
 const saveMemberRoles=async()=>{
   setSaving(true)
   const ok=await run(()=>updateGroupMemberRoles(token,workspaceId,group.id,memberRoleEditor.memberId,[...memberRoleEditor.roleIds]))
   setSaving(false)
   if(ok)setMemberRoleEditor(null)
 }

 return <div className="modal-backdrop"><div className="modal-card group-editor" role="dialog" aria-modal="true">
  <div className="group-editor-header"><h2>Редагування групи</h2><button type="button" className="group-editor-close-x" onClick={onClose}>×</button></div>
  <div className="group-editor-scroll">
    {error&&<div className="page-error group-editor-error">{error}</div>}
    <div className="group-editor-grid">
      <section className="group-editor-panel">
        <h3>Основні дані</h3>
        <label className="form-field"><span>Назва</span><input value={name} onChange={e=>setName(e.target.value)}/></label>
        <label className="form-field"><span>Опис</span><textarea value={description} onChange={e=>setDescription(e.target.value)}/></label>
        <label className="group-navigation-toggle"><input type="checkbox" checked={showInNavigation} onChange={e=>setShowInNavigation(e.target.checked)}/><span>Показувати в навігації</span></label>
      </section>

      <section className="group-editor-panel">
        <div className="role-section-heading"><h3>Особи та їх ролі</h3></div>
        <div className="group-edit-row"><select value={memberId} onChange={e=>setMemberId(e.target.value)}><option value="">Оберіть особу…</option>{availableMembers.map(m=><option key={m.id} value={m.id}>{m.name}</option>)}</select><button type="button" disabled={!memberId} onClick={()=>run(async()=>{await addGroupMember(token,workspaceId,group.id,{memberId:Number(memberId),roleIds:[]});setMemberId('')})}>Додати</button></div>
        <div className="group-editor-list">
          {group.members.map(m=><div className="role-member-card" key={m.memberId}><div className="group-edit-item"><strong>{m.memberName}</strong><span className="group-edit-actions"><button type="button" onClick={()=>setMemberRoleEditor({memberId:m.memberId,memberName:m.memberName,roleIds:new Set(m.roleIds||[])})}>Вибрати ролі</button><button type="button" onClick={()=>run(()=>removeGroupMember(token,workspaceId,group.id,m.memberId))}>×</button></span></div><div className="role-chip-list">{(m.roleIds||[]).length?(m.roleIds||[]).map(id=><span className="role-chip" key={id}>{roleName(id)}</span>):<span className="role-muted">Ролі не призначені</span>}</div></div>)}
        </div>
      </section>

      <section className="group-editor-panel">
        <h3>Підгрупи</h3>
        <div className="group-edit-row"><select value={childId} onChange={e=>setChildId(e.target.value)}><option value="">Оберіть групу…</option>{availableGroups.map(g=><option key={g.id} value={g.id}>{g.name}</option>)}</select><button type="button" disabled={!childId} onClick={()=>run(async()=>{await addChildGroup(token,workspaceId,group.id,Number(childId));setChildId('')})}>Додати</button></div>
        <div className="group-editor-list">
          {group.childGroups.map(c=><div className="group-edit-item" key={c.groupId}><span>↳ {c.groupName}</span><button type="button" onClick={()=>run(()=>removeChildGroup(token,workspaceId,group.id,c.groupId))}>×</button></div>)}
        </div>

        <div className="role-section-heading"><h3>Ролі</h3><button type="button" onClick={()=>setShowSetManager(true)}>Керування наборами</button></div>
        <div className="group-editor-list">
          {groupedRoles.length?groupedRoles.map(set=><div className="role-set-block" key={set.id}><div className="role-set-title">{set.name}</div>{set.roles.map(r=><button type="button" className="role-list-row" key={r.id} onClick={()=>openRole(r)}><span><strong>{r.name}</strong>{r.description&&<small>{r.description}</small>}</span><span>Редагувати ›</span></button>)}</div>):<div className="role-muted">У цій групі ще немає ролей.</div>}
        </div>
        <button type="button" className="secondary-button role-add-button" onClick={openNewRole}>+ Додати роль</button>
      </section>
    </div>
  </div>

  <div className="modal-actions group-editor-actions"><button type="button" className="secondary-button" onClick={onClose}>Скасувати</button><button type="button" className="add-member-button" onClick={saveMain} disabled={saving||!name.trim()}>{saving?'Збереження…':'Зберегти'}</button></div>

  {showSetManager&&<div className="nested-modal-backdrop"><div className="nested-modal-card">
    <div className="group-editor-header"><h3>Набори ролей</h3><button type="button" className="group-editor-close-x" onClick={()=>setShowSetManager(false)}>×</button></div>
    <div className="role-set-create"><input placeholder="Назва набору" value={newSet.name} onChange={e=>setNewSet({...newSet,name:e.target.value})}/><input placeholder="Опис (необов'язково)" value={newSet.description} onChange={e=>setNewSet({...newSet,description:e.target.value})}/><button type="button" disabled={!newSet.name.trim()} onClick={()=>run(async()=>{await createRoleSet(token,workspaceId,{name:newSet.name.trim(),description:newSet.description||null});setNewSet({name:'',description:''})})}>+ Створити</button></div>
    {roleSets.map(s=><div className="role-set-manage-row" key={s.id}>{editingSet?.id===s.id?<><input value={editingSet.name} onChange={e=>setEditingSet({...editingSet,name:e.target.value})}/><input value={editingSet.description||''} onChange={e=>setEditingSet({...editingSet,description:e.target.value})}/><button type="button" onClick={()=>run(async()=>{await updateRoleSet(token,workspaceId,s.id,{name:editingSet.name.trim(),description:editingSet.description||null});setEditingSet(null)})}>Зберегти</button></>:<><span><strong>{s.name}</strong><small>{s.description||'Без опису'}</small></span><span className="group-edit-actions"><button type="button" onClick={()=>setEditingSet({...s})}>Редагувати</button><button type="button" className="danger-link" onClick={()=>window.confirm(`Видалити набір "${s.name}"? Ролі залишаться без набору.`)&&run(()=>deleteRoleSet(token,workspaceId,s.id))}>Видалити</button></span></>}</div>)}
  </div></div>}

  {roleEditor&&<div className="nested-modal-backdrop"><form className="nested-modal-card role-editor-modal" onSubmit={saveRole}>
    <div className="group-editor-header"><h3>{roleEditor.id?'Редагування ролі':'Нова роль'}</h3><button type="button" className="group-editor-close-x" onClick={()=>setRoleEditor(null)}>×</button></div>
    {error&&<div className="page-error" style={{marginTop:'12px'}}>{error}</div>}
    <label className="form-field"><span>Назва</span><input value={roleEditor.name} onChange={e=>setRoleEditor({...roleEditor,name:e.target.value})}/></label>
    <label className="form-field"><span>Набір ролей</span><select value={roleEditor.roleSetId||''} onChange={e=>setRoleEditor({...roleEditor,roleSetId:e.target.value})}><option value="">Без набору</option>{roleSets.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}</select></label>
    <label className="form-field"><span>Опис</span><textarea value={roleEditor.description||''} onChange={e=>setRoleEditor({...roleEditor,description:e.target.value})}/></label>
    <h4>{t('rolePermissions.title')}</h4><div className="role-permissions">{permissionOptions.map(p=>{const scope=roleEditor.permissions[p];return <div className="role-permission-row" key={p}><label><input type="checkbox" checked={Boolean(scope)} onChange={e=>{const next={...roleEditor.permissions};e.target.checked?next[p]='GROUP':delete next[p];setRoleEditor({...roleEditor,permissions:next})}}/><span>{t(`rolePermissions.permissions.${p}`,{defaultValue:p})}</span></label>{scope&&<select value={scope} onChange={e=>setRoleEditor({...roleEditor,permissions:{...roleEditor.permissions,[p]:e.target.value}})}><option value="GROUP">{t('rolePermissions.scopes.GROUP')}</option><option value="GROUP_SUBTREE">{t('rolePermissions.scopes.GROUP_SUBTREE')}</option></select>}</div>})}</div>
    <div className="modal-actions"><button type="button" className="secondary-button" onClick={()=>setRoleEditor(null)}>Скасувати</button><button type="submit" className="add-member-button" disabled={saving}>{saving?'Збереження…':'Зберегти'}</button></div>
  </form></div>}

  {memberRoleEditor&&<div className="nested-modal-backdrop"><div className="nested-modal-card">
    <div className="group-editor-header"><h3>Ролі: {memberRoleEditor.memberName}</h3><button type="button" className="group-editor-close-x" onClick={()=>setMemberRoleEditor(null)}>×</button></div>
    {error&&<div className="page-error" style={{marginTop:'12px'}}>{error}</div>}
    {groupedRoles.map(set=><div className="role-set-block" key={set.id}><div className="role-set-title">{set.name}</div>{set.roles.map(r=><label className="role-choice-row" key={r.id}><input type="checkbox" checked={memberRoleEditor.roleIds.has(r.id)} onChange={()=>{const ids=new Set(memberRoleEditor.roleIds);ids.has(r.id)?ids.delete(r.id):ids.add(r.id);setMemberRoleEditor({...memberRoleEditor,roleIds:ids})}}/><span>{r.name}</span></label>)}</div>)}
    {!group.roles.length&&<div className="role-muted">Спочатку створіть хоча б одну роль.</div>}
    <div className="modal-actions"><button type="button" className="secondary-button" onClick={()=>setMemberRoleEditor(null)}>Скасувати</button><button type="button" className="add-member-button" disabled={saving} onClick={saveMemberRoles}>{saving?'Збереження…':'Зберегти ролі'}</button></div>
  </div></div>}
 </div></div>
}
