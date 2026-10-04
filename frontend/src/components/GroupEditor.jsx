import { useEffect, useMemo, useState } from 'react'
import {
  updateMemberGroup, addGroupMember, removeGroupMember, addChildGroup, removeChildGroup,
  addGroupRole, updateGroupRole, updateGroupMemberRoles,
  getGroupPermissions, addGroupPermission, removeGroupPermission,
  getRoleSets, createRoleSet, updateRoleSet,
} from '../api/memberGroupApi'

const permissionOptions = [
  'TASK_VIEW','TASK_CREATE','TASK_ASSIGN','TASK_MANAGE','TASK_APPROVE',
  'MEMBER_VIEW','MEMBER_MANAGE','GROUP_VIEW','GROUP_MANAGE',
  'POINT_VIEW','POINT_AWARD','POINT_SPEND','SUBGROUP_MANAGE',
]

export default function GroupEditor({ group, groups, members, token, workspaceId, onChanged, onClose }) {
  const [name,setName]=useState(group.name)
  const [description,setDescription]=useState(group.description||'')
  const [showInNavigation,setShowInNavigation]=useState(Boolean(group.showInNavigation))
  const [memberId,setMemberId]=useState('')
  const [childId,setChildId]=useState('')
  const [roleSets,setRoleSets]=useState([])
  const [newSetName,setNewSetName]=useState('')
  const [editingSet,setEditingSet]=useState(null)
  const [editingRole,setEditingRole]=useState(null)
  const [newRole,setNewRole]=useState({name:'',description:'',roleSetId:''})
  const [permissionGrants,setPermissionGrants]=useState([])
  const [error,setError]=useState('')
  const [saving,setSaving]=useState(false)

  const reloadAux=async()=>{
    const [sets,grants]=await Promise.all([
      getRoleSets(token,workspaceId),
      getGroupPermissions(token,workspaceId,group.id),
    ])
    setRoleSets(sets); setPermissionGrants(grants)
  }
  useEffect(()=>{reloadAux().catch(e=>setError(e.message))},[token,workspaceId,group.id])
  useEffect(()=>{const k=e=>{if(e.key==='Escape')onClose()};window.addEventListener('keydown',k);return()=>window.removeEventListener('keydown',k)},[onClose])

  const run=async action=>{try{setError('');await action();await onChanged();await reloadAux()}catch(e){setError(e.message)}}
  const availableMembers=useMemo(()=>members.filter(m=>!group.members.some(x=>x.memberId===m.id)),[members,group])
  const availableGroups=useMemo(()=>groups.filter(g=>g.id!==group.id&&!group.childGroups.some(x=>x.groupId===g.id)),[groups,group])
  const grantsFor=roleId=>permissionGrants.filter(g=>g.roleId===roleId)

  async function saveMain(){
    if(!name.trim())return setError('Назва групи не може бути порожньою.')
    try{setSaving(true);setError('');await updateMemberGroup(token,workspaceId,group.id,{name:name.trim(),description:description.trim()||null,showInNavigation});await onChanged();onClose()}
    catch(e){setError(e.message)}finally{setSaving(false)}
  }

  const toggleMemberRole=(member,roleId)=>{
    const ids=new Set(member.roleIds||[])
    ids.has(roleId)?ids.delete(roleId):ids.add(roleId)
    return run(()=>updateGroupMemberRoles(token,workspaceId,group.id,member.memberId,[...ids]))
  }

  return <div className="modal-backdrop"><div className="modal-card group-editor" role="dialog" aria-modal="true">
    <div className="group-editor-header"><h2>Редагування групи</h2><button type="button" className="group-editor-close-x" onClick={onClose}>×</button></div>
    {error&&<div className="page-error">{error}</div>}

    <label className="form-field"><span>Назва</span><input value={name} onChange={e=>setName(e.target.value)}/></label>
    <label className="form-field"><span>Опис</span><textarea value={description} onChange={e=>setDescription(e.target.value)}/></label>
    <label className="group-navigation-toggle"><input type="checkbox" checked={showInNavigation} onChange={e=>setShowInNavigation(e.target.checked)}/><span>Показувати в навігації</span></label>

    <h3>Особи та їх ролі</h3>
    <div className="group-edit-row">
      <select value={memberId} onChange={e=>setMemberId(e.target.value)}><option value="">Оберіть особу…</option>{availableMembers.map(m=><option key={m.id} value={m.id}>{m.name}</option>)}</select>
      <button type="button" disabled={!memberId} onClick={()=>run(async()=>{await addGroupMember(token,workspaceId,group.id,{memberId:Number(memberId),roleIds:[]});setMemberId('')})}>Додати</button>
    </div>
    {group.members.map(member=><div className="role-member-card" key={member.memberId}>
      <div className="group-edit-item"><strong>{member.memberName}</strong><button type="button" onClick={()=>run(()=>removeGroupMember(token,workspaceId,group.id,member.memberId))}>×</button></div>
      <div className="group-tags">{group.roles.map(role=><label key={role.id} className="role-check"><input type="checkbox" checked={(member.roleIds||[]).includes(role.id)} onChange={()=>toggleMemberRole(member,role.id)}/>{role.name}</label>)}</div>
    </div>)}

    <h3>Підгрупи</h3>
    <div className="group-edit-row"><select value={childId} onChange={e=>setChildId(e.target.value)}><option value="">Оберіть групу…</option>{availableGroups.map(g=><option key={g.id} value={g.id}>{g.name}</option>)}</select><button type="button" disabled={!childId} onClick={()=>run(async()=>{await addChildGroup(token,workspaceId,group.id,Number(childId));setChildId('')})}>Додати</button></div>
    {group.childGroups.map(c=><div className="group-edit-item" key={c.groupId}><span>↳ {c.groupName}</span><button type="button" onClick={()=>run(()=>removeChildGroup(token,workspaceId,group.id,c.groupId))}>×</button></div>)}

    <h3>Набори ролей</h3>
    <div className="group-edit-row"><input value={newSetName} onChange={e=>setNewSetName(e.target.value)} placeholder="Напр. Керівництво"/><button type="button" disabled={!newSetName.trim()} onClick={()=>run(async()=>{await createRoleSet(token,workspaceId,{name:newSetName.trim(),description:null});setNewSetName('')})}>+ Набір</button></div>
    {roleSets.map(set=><div className="group-edit-item" key={set.id}>
      {editingSet?.id===set.id?<><input value={editingSet.name} onChange={e=>setEditingSet({...editingSet,name:e.target.value})}/><button type="button" onClick={()=>run(async()=>{await updateRoleSet(token,workspaceId,set.id,{name:editingSet.name,description:editingSet.description||null});setEditingSet(null)})}>Зберегти</button></>:<><span><strong>{set.name}</strong>{set.description?' · '+set.description:''}</span><button type="button" onClick={()=>setEditingSet({...set})}>Редагувати</button></>}
    </div>)}

    <h3>Ролі групи</h3>
    <div className="role-editor-box">
      <div className="group-edit-row"><input value={newRole.name} onChange={e=>setNewRole({...newRole,name:e.target.value})} placeholder="Назва ролі"/><select value={newRole.roleSetId} onChange={e=>setNewRole({...newRole,roleSetId:e.target.value})}><option value="">Без набору</option>{roleSets.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}</select><button type="button" disabled={!newRole.name.trim()} onClick={()=>run(async()=>{const updated=await addGroupRole(token,workspaceId,group.id,{name:newRole.name.trim(),description:newRole.description||null});const created=updated.roles.find(r=>r.name===newRole.name.trim());if(created&&newRole.roleSetId)await updateGroupRole(token,workspaceId,group.id,created.id,{name:created.name,description:created.description,roleSetId:Number(newRole.roleSetId)});setNewRole({name:'',description:'',roleSetId:''})})}>+ Роль</button></div>
    </div>

    {group.roles.map(role=><div className="role-manager-card" key={role.id}>
      {editingRole?.id===role.id?<>
        <div className="group-edit-row"><input value={editingRole.name} onChange={e=>setEditingRole({...editingRole,name:e.target.value})}/><select value={editingRole.roleSetId||''} onChange={e=>setEditingRole({...editingRole,roleSetId:e.target.value})}><option value="">Без набору</option>{roleSets.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}</select></div>
        <textarea value={editingRole.description||''} onChange={e=>setEditingRole({...editingRole,description:e.target.value})}/>
        <div className="modal-actions"><button type="button" onClick={()=>setEditingRole(null)}>Скасувати</button><button type="button" onClick={()=>run(async()=>{await updateGroupRole(token,workspaceId,group.id,role.id,{name:editingRole.name,description:editingRole.description||null,roleSetId:editingRole.roleSetId?Number(editingRole.roleSetId):null});setEditingRole(null)})}>Зберегти роль</button></div>
      </>:<div className="group-edit-item"><span><strong>{role.name}</strong>{role.roleSetId?' · '+(roleSets.find(s=>s.id===role.roleSetId)?.name||'Набір'):''}{role.description?' · '+role.description:''}</span><button type="button" onClick={()=>setEditingRole({...role})}>Редагувати</button></div>}

      <div className="role-permissions">
        {permissionOptions.map(p=>{
          const grant=grantsFor(role.id).find(g=>g.permission===p)
          return <div className="role-permission-row" key={p}><label><input type="checkbox" checked={Boolean(grant)} onChange={()=>grant?run(()=>removeGroupPermission(token,workspaceId,group.id,grant.id)):run(()=>addGroupPermission(token,workspaceId,group.id,role.id,{permission:p,scope:'GROUP'}))}/>{p}</label>
          {grant&&<select value={grant.scope} onChange={e=>run(async()=>{await removeGroupPermission(token,workspaceId,group.id,grant.id);await addGroupPermission(token,workspaceId,group.id,role.id,{permission:p,scope:e.target.value})})}><option value="GROUP">Ця група</option><option value="GROUP_SUBTREE">Група + підгрупи</option></select>}</div>
        })}
      </div>
    </div>)}

    <div className="modal-actions group-editor-actions"><button type="button" className="secondary-button" onClick={onClose}>Скасувати</button><button type="button" className="add-member-button" onClick={saveMain} disabled={saving||!name.trim()}>{saving?'Збереження…':'Зберегти'}</button></div>
  </div></div>
}
