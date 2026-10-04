import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  updateMemberGroup, addGroupMember, removeGroupMember, addChildGroup, removeChildGroup,
  addGroupRole, updateGroupRole, updateGroupMemberRoles,
  getGroupPermissions, addGroupPermission, removeGroupPermission,
  getRoleSets, createRoleSet, updateRoleSet, deleteRoleSet,
} from '../api/memberGroupApi'
import {
  getRoleDefinitions, createRoleDefinition, updateRoleDefinition, deleteRoleDefinition
} from '../api/roleCatalogApi'

const permissionGroups=[
 {key:'tasks',permissions:['TASK_VIEW','TASK_CREATE','TASK_ASSIGN','TASK_MANAGE','TASK_APPROVE']},
 {key:'members',permissions:['MEMBER_VIEW','MEMBER_MANAGE']},
 {key:'groups',permissions:['GROUP_VIEW','GROUP_MANAGE','SUBGROUP_MANAGE']},
 {key:'points',permissions:['POINT_VIEW','POINT_AWARD','POINT_SPEND']},
]

export default function GroupEditor({group,groups,members,token,workspaceId,onChanged,onClose}){
 const { t } = useTranslation()
 const [name,setName]=useState(group.name),[description,setDescription]=useState(group.description||''),[showInNavigation,setShowInNavigation]=useState(Boolean(group.showInNavigation))
 const [memberId,setMemberId]=useState(''),[childId,setChildId]=useState(''),[roleSets,setRoleSets]=useState([]),[roleDefinitions,setRoleDefinitions]=useState([]),[permissionGrants,setPermissionGrants]=useState([])
 const [showSetManager,setShowSetManager]=useState(false),[showNewSetForm,setShowNewSetForm]=useState(false),[newSet,setNewSet]=useState({name:'',description:''}),[editingSet,setEditingSet]=useState(null),[catalogRoleEditor,setCatalogRoleEditor]=useState(null)
 const [roleEditor,setRoleEditor]=useState(null),[memberRoleEditor,setMemberRoleEditor]=useState(null),[error,setError]=useState(''),[saving,setSaving]=useState(false)

 const reloadAux=async()=>{const [s,d,g]=await Promise.all([getRoleSets(token,workspaceId),getRoleDefinitions(token,workspaceId),getGroupPermissions(token,workspaceId,group.id)]);setRoleSets(s);setRoleDefinitions(d);setPermissionGrants(g)}
 useEffect(()=>{reloadAux().catch(e=>setError(e.message))},[token,workspaceId,group.id])
 useEffect(()=>{const k=e=>e.key==='Escape'&&(roleEditor?setRoleEditor(null):memberRoleEditor?setMemberRoleEditor(null):showSetManager?setShowSetManager(false):onClose());window.addEventListener('keydown',k);return()=>window.removeEventListener('keydown',k)},[onClose,roleEditor,memberRoleEditor,showSetManager])
 const run=async action=>{try{setError('');await action();await onChanged();await reloadAux();return true}catch(e){setError(e.message||'Помилка збереження');return false}}
 const availableMembers=useMemo(()=>members.filter(m=>!group.members.some(x=>x.memberId===m.id)),[members,group])
 const availableGroups=useMemo(()=>groups.filter(g=>g.id!==group.id&&!group.childGroups.some(x=>x.groupId===g.id)),[groups,group])
 const groupedRoles=useMemo(()=>{const rows=[...roleSets.map(s=>({id:String(s.id),name:s.name,roles:group.roles.filter(r=>r.roleSetId===s.id)})),{id:'none',name:'Без набору',roles:group.roles.filter(r=>!r.roleSetId)}];return rows.filter(x=>x.roles.length)},[roleSets,group.roles])
 const grantsFor=id=>permissionGrants.filter(g=>g.roleId===id)
 const roleName=id=>group.roles.find(r=>r.id===id)?.name||`#${id}`
 const roleSetDisplayName=s=>s.systemCode?t(`roles.roleSets.${s.systemCode}`,{defaultValue:s.name}):s.name
 const roleSetDisplayDescription=s=>s.systemCode?t(`roles.roleSetDescriptions.${s.systemCode}`,{defaultValue:s.description||''}):(s.description||'')
 const catalogRoleDisplayName=r=>r.systemCode?t(`roles.system.${r.systemCode}`,{defaultValue:r.name}):r.name
 const rolesForSet=setId=>roleDefinitions.filter(r=>r.roleSetId===setId&&r.active)

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

 const openNewCatalogRole=roleSetId=>setCatalogRoleEditor({id:null,roleSetId,name:'',description:'',systemDefault:false})
 const openCatalogRole=role=>setCatalogRoleEditor({...role})
 const saveCatalogRole=async()=>{
   const roleName=(catalogRoleEditor?.name||'').trim()
   if(!roleName){setError(t('roles.roleNameRequired'));return}
   setSaving(true)
   const body={
     name:roleName,
     description:catalogRoleEditor.description?.trim()||null,
     roleSetId:catalogRoleEditor.roleSetId,
     visibility:'SHARED',
     ownerContextType:'WORKSPACE',
     ownerContextId:null
   }
   const ok=await run(async()=>{
     if(catalogRoleEditor.id) await updateRoleDefinition(token,workspaceId,catalogRoleEditor.id,body)
     else await createRoleDefinition(token,workspaceId,body)
   })
   setSaving(false)
   if(ok)setCatalogRoleEditor(null)
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

  {showSetManager&&<div className="nested-modal-backdrop"><div className="nested-modal-card role-set-manager-modal">
    <div className="group-editor-header">
      <div><h3>{t('roles.managerTitle')}</h3><p className="role-muted">{t('roles.managerDescription')}</p></div>
      <button type="button" className="group-editor-close-x" onClick={()=>{setShowSetManager(false);setShowNewSetForm(false);setEditingSet(null)}}>×</button>
    </div>

    <div className="role-set-manager-primary-actions">
      <button type="button" className="primary-button" onClick={()=>openNewCatalogRole(null)}>+ {t('roles.addStandaloneRole')}</button>
      {!showNewSetForm
        ? <button type="button" className="secondary-button" onClick={()=>setShowNewSetForm(true)}>+ {t('roles.addSet')}</button>
      : <div className="role-set-create role-set-create-expanded">
          <input placeholder={t('roles.setNamePlaceholder')} value={newSet.name} onChange={e=>setNewSet({...newSet,name:e.target.value})}/>
          <input placeholder={t('roles.setDescriptionPlaceholder')} value={newSet.description} onChange={e=>setNewSet({...newSet,description:e.target.value})}/>
          <div className="role-set-create-actions">
            <button type="button" className="secondary-button" onClick={()=>{setShowNewSetForm(false);setNewSet({name:'',description:''})}}>{t('common.cancel')}</button>
            <button type="button" className="primary-button" disabled={!newSet.name.trim()} onClick={()=>run(async()=>{await createRoleSet(token,workspaceId,{name:newSet.name.trim(),description:newSet.description||null});setNewSet({name:'',description:''});setShowNewSetForm(false)})}>{t('roles.createSet')}</button>
          </div>
        </div>}
    </div>

    <div className="role-set-manager-list">
      {roleSets.map(s=>{
        const setRoles=rolesForSet(s.id)
        return <section className={`role-set-manage-card ${s.systemDefault?'system-role-set':''}`} key={s.id}>
          <div className="role-set-manage-header">
            {editingSet?.id===s.id&&!s.systemDefault
              ? <>
                  <div className="role-set-edit-fields">
                    <input value={editingSet.name} onChange={e=>setEditingSet({...editingSet,name:e.target.value})}/>
                    <input value={editingSet.description||''} onChange={e=>setEditingSet({...editingSet,description:e.target.value})}/>
                  </div>
                  <span className="group-edit-actions">
                    <button type="button" className="secondary-button" onClick={()=>setEditingSet(null)}>{t('common.cancel')}</button>
                    <button type="button" className="primary-button" disabled={!editingSet.name.trim()} onClick={()=>run(async()=>{await updateRoleSet(token,workspaceId,s.id,{name:editingSet.name.trim(),description:editingSet.description||null});setEditingSet(null)})}>{t('roles.save')}</button>
                  </span>
                </>
              : <>
                  <span className="role-set-info">
                    <span className="role-set-name-line"><strong>{roleSetDisplayName(s)}</strong>{s.systemDefault&&<span className="role-system-badge">{t('roles.systemBadge')}</span>}</span>
                    <small>{roleSetDisplayDescription(s)||t('roles.noDescription')}</small>
                  </span>
                  {!s.systemDefault&&<span className="group-edit-actions">
                    <button type="button" onClick={()=>setEditingSet({...s})}>{t('common.edit')}</button>
                    <button type="button" className="danger-link" onClick={()=>window.confirm(t('roles.deleteSetConfirm'))&&run(()=>deleteRoleSet(token,workspaceId,s.id))}>{t('roles.delete')}</button>
                  </span>}
                </>}
          </div>
          <div className="role-set-contents">
            <div className="role-set-contents-title">
              <strong>{t('roles.rolesInSet')}</strong>
              {!s.systemDefault&&<button type="button" className="role-inline-add" onClick={()=>openNewCatalogRole(s.id)}>+ {t('roles.addRole')}</button>}
            </div>
            {setRoles.length
              ? <div className="role-set-definition-list">{setRoles.map(r=><div className="role-set-definition-row" key={r.id}>
                  <span><strong>{catalogRoleDisplayName(r)}</strong>{r.description&&<small>{r.description}</small>}</span>
                  {r.systemDefault
                    ? <span className="role-system-badge">{t('roles.systemBadge')}</span>
                    : <span className="group-edit-actions">
                        <button type="button" onClick={()=>openCatalogRole(r)}>{t('common.edit')}</button>
                        <button type="button" className="danger-link" onClick={()=>window.confirm(t('roles.deleteRoleConfirm'))&&run(()=>deleteRoleDefinition(token,workspaceId,r.id))}>{t('roles.delete')}</button>
                      </span>}
                </div>)}</div>
              : <div className="role-muted">{t('roles.emptySet')}</div>}
          </div>
        </section>
      })}
    </div>
  </div></div>}

  {catalogRoleEditor&&<div className="nested-modal-backdrop"><div className="nested-modal-card role-catalog-role-modal">
    <div className="group-editor-header">
      <h3>{catalogRoleEditor.id?t('roles.editRole'):t('roles.newRole')}</h3>
      <button type="button" className="group-editor-close-x" onClick={()=>setCatalogRoleEditor(null)}>×</button>
    </div>
    {error&&<div className="page-error" style={{marginTop:'12px'}}>{error}</div>}
    <label className="form-field"><span>{t('roles.name')}</span><input value={catalogRoleEditor.name||''} onChange={e=>setCatalogRoleEditor({...catalogRoleEditor,name:e.target.value})}/></label>
    <label className="form-field"><span>{t('roles.roleSet')}</span>
      <select value={catalogRoleEditor.roleSetId||''} onChange={e=>setCatalogRoleEditor({...catalogRoleEditor,roleSetId:e.target.value?Number(e.target.value):null})}>
        <option value="">{t('roles.withoutSet')}</option>
        {roleSets.map(s=><option key={s.id} value={s.id}>{roleSetDisplayName(s)}</option>)}
      </select>
    </label>
    <label className="form-field"><span>{t('roles.descriptionField')}</span><textarea value={catalogRoleEditor.description||''} onChange={e=>setCatalogRoleEditor({...catalogRoleEditor,description:e.target.value})}/></label>
    <div className="modal-actions">
      <button type="button" className="secondary-button" onClick={()=>setCatalogRoleEditor(null)}>{t('common.cancel')}</button>
      <button type="button" className="primary-button" disabled={saving||!catalogRoleEditor.name?.trim()} onClick={saveCatalogRole}>{saving?t('common.saving'):t('roles.save')}</button>
    </div>
  </div></div>}

  {roleEditor&&<div className="nested-modal-backdrop"><form className="nested-modal-card role-editor-modal" onSubmit={saveRole}>
    <div className="group-editor-header"><h3>{roleEditor.id?'Редагування ролі':'Нова роль'}</h3><button type="button" className="group-editor-close-x" onClick={()=>setRoleEditor(null)}>×</button></div>
    {error&&<div className="page-error" style={{marginTop:'12px'}}>{error}</div>}
    <label className="form-field"><span>Назва</span><input value={roleEditor.name} onChange={e=>setRoleEditor({...roleEditor,name:e.target.value})}/></label>
    <label className="form-field"><span>Набір ролей</span><select value={roleEditor.roleSetId||''} onChange={e=>setRoleEditor({...roleEditor,roleSetId:e.target.value})}><option value="">Без набору</option>{roleSets.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}</select></label>
    <label className="form-field"><span>Опис</span><textarea value={roleEditor.description||''} onChange={e=>setRoleEditor({...roleEditor,description:e.target.value})}/></label>
    <h4>{t('rolePermissions.title')}</h4>
    <div className="role-permission-groups">
      {permissionGroups.map(group=><section className="role-permission-group" key={group.key}>
        <h5>{t(`rolePermissions.groups.${group.key}`)}</h5>
        <div className="role-permissions">
          {group.permissions.map(p=>{const scope=roleEditor.permissions[p];return <div className="role-permission-row" key={p}>
            <label className="role-permission-check">
              <input type="checkbox" checked={Boolean(scope)} onChange={e=>{const next={...roleEditor.permissions};e.target.checked?next[p]='GROUP':delete next[p];setRoleEditor({...roleEditor,permissions:next})}}/>
              <span>{t(`rolePermissions.permissions.${p}`,{defaultValue:p})}</span>
            </label>
            {scope&&<select value={scope} onChange={e=>setRoleEditor({...roleEditor,permissions:{...roleEditor.permissions,[p]:e.target.value}})}><option value="GROUP">{t('rolePermissions.scopes.GROUP')}</option><option value="GROUP_SUBTREE">{t('rolePermissions.scopes.GROUP_SUBTREE')}</option></select>}
          </div>})}
        </div>
      </section>)}
    </div>
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
