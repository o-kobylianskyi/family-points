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
