import { apiRequest } from './client'
async function h(request,m){const r=await request;if(!r.ok){let x=m;try{const e=await r.json();if(e.message)x=e.message}catch{}throw new Error(x)}if(r.status===204)return null;return r.json()}
const p=(f)=>`/workspaces/${f}/member-groups`
export const getMemberGroups=(t,f)=>h(apiRequest(p(f),{token:t}),'Failed to load member groups')
export const createMemberGroup=(t,f,b)=>h(apiRequest(p(f),{token:t,method:'POST',body:b}),'Failed to create member group')
export const updateMemberGroup=(t,f,g,b)=>h(apiRequest(`${p(f)}/${g}`,{token:t,method:'PUT',body:b}),'Failed to update member group')
export const deleteMemberGroup=(t,f,g)=>h(apiRequest(`${p(f)}/${g}`,{token:t,method:'DELETE'}),'Failed to delete member group')
export const addGroupRole=(t,f,g,b)=>h(apiRequest(`${p(f)}/${g}/roles`,{token:t,method:'POST',body:b}),'Failed to add group role')
export const addGroupMember=(t,f,g,b)=>h(apiRequest(`${p(f)}/${g}/members`,{token:t,method:'PUT',body:b}),'Failed to add group member')
export const removeGroupMember=(t,f,g,m)=>h(apiRequest(`${p(f)}/${g}/members/${m}`,{token:t,method:'DELETE'}),'Failed to remove group member')
export const addChildGroup=(t,f,g,c)=>h(apiRequest(`${p(f)}/${g}/children`,{token:t,method:'PUT',body:{childGroupId:c}}),'Failed to add child group')
export const removeChildGroup=(t,f,g,c)=>h(apiRequest(`${p(f)}/${g}/children/${c}`,{token:t,method:'DELETE'}),'Failed to remove child group')
export const getResolvedActors=(t,f,g)=>h(apiRequest(`${p(f)}/${g}/resolved-actors`,{token:t}),'Failed to resolve group actors')
export const addGroupPoints=(t,f,g,b)=>h(apiRequest(`${p(f)}/${g}/points`,{token:t,method:'POST',body:b}),'Failed to change group balance')

export const getGroupPermissions=(t,f,g)=>h(apiRequest(`${p(f)}/${g}/permissions`,{token:t}),'Failed to load group permissions')
export const addGroupPermission=(t,f,g,r,b)=>h(apiRequest(`${p(f)}/${g}/roles/${r}/permissions`,{token:t,method:'POST',body:b}),'Failed to add group permission')
export const removeGroupPermission=(t,f,g,id)=>h(apiRequest(`${p(f)}/${g}/permissions/${id}`,{token:t,method:'DELETE'}),'Failed to remove group permission')

export const updateGroupRole=(t,f,g,r,b)=>h(apiRequest(`${p(f)}/${g}/roles/${r}`,{token:t,method:'PUT',body:b}),'Failed to update group role')
export const updateGroupMemberRoles=(t,f,g,m,roleIds)=>h(apiRequest(`${p(f)}/${g}/members/${m}/roles`,{token:t,method:'PUT',body:roleIds}),'Failed to update member roles')
const rs=(f)=>`/workspaces/${f}/role-sets`
export const getRoleSets=(t,f)=>h(apiRequest(rs(f),{token:t}),'Failed to load role sets')
export const createRoleSet=(t,f,b)=>h(apiRequest(rs(f),{token:t,method:'POST',body:b}),'Failed to create role set')
export const updateRoleSet=(t,f,id,b)=>h(apiRequest(`${rs(f)}/${id}`,{token:t,method:'PUT',body:b}),'Failed to update role set')
export const deleteRoleSet=(t,f,id)=>h(apiRequest(`${rs(f)}/${id}`,{token:t,method:'DELETE'}),'Failed to delete role set')
