import { useEffect, useMemo, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { getMemberGroups } from '../api/memberGroupApi'
import { useAuth } from '../context/AuthContext'

function Sidebar() {
  const { t } = useTranslation()
  const { currentUser, getAccessToken } = useAuth()
  const [groups, setGroups] = useState([])

  useEffect(() => {
    getMemberGroups(getAccessToken(), currentUser.workspaceId).then(setGroups).catch(() => setGroups([]))
  }, [currentUser.workspaceId, getAccessToken])

  const groupById = useMemo(() => new Map(groups.map(g => [g.id, g])), [groups])
  const visibleIds = useMemo(() => new Set(groups.filter(g => g.showInNavigation).map(g => g.id)), [groups])
  const visibleChildIds = useMemo(() => {
    const ids = new Set()
    groups.forEach(g => g.childGroups?.forEach(c => { if (visibleIds.has(c.groupId)) ids.add(c.groupId) }))
    return ids
  }, [groups, visibleIds])
  const roots = useMemo(() => groups.filter(g => visibleIds.has(g.id) && !visibleChildIds.has(g.id)), [groups, visibleIds, visibleChildIds])

  const renderGroup = (group, depth = 0, visited = new Set()) => {
    if (!group || visited.has(group.id)) return null
    const next = new Set(visited); next.add(group.id)
    const children = (group.childGroups || []).map(c => groupById.get(c.groupId)).filter(g => g && visibleIds.has(g.id))
    return <div key={group.id} className="nav-group-node">
      <NavLink to={`/groups/${group.id}`} className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'} style={{paddingLeft: `${16 + depth * 16}px`}}>{group.name}</NavLink>
      {children.map(child => renderGroup(child, depth + 1, next))}
    </div>
  }

  return <aside className="sidebar">
    <div className="sidebar-logo"><div className="brand-icon">FP</div><div><strong>FamilyPoints</strong><span>Workspace</span></div></div>
    <nav className="sidebar-nav">
      <NavLink to="/" end className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}>{t('navigation.dashboard')}</NavLink>
      <NavLink to="/members" className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}>Учасники</NavLink>
      <NavLink to="/tasks" className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}>{t('navigation.tasks')}</NavLink>
      <NavLink to="/points" className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}>{t('navigation.points')}</NavLink>
      <NavLink to="/rewards" className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}>{t('navigation.rewards')}</NavLink>
      {roots.length > 0 && <div className="sidebar-dynamic-groups">{roots.map(g => renderGroup(g))}</div>}
    </nav>
    <div className="sidebar-bottom"><NavLink to="/settings" className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}>{t('navigation.settings')}</NavLink></div>
  </aside>
}
export default Sidebar
