import { useEffect, useMemo, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { getMemberGroups } from '../api/memberGroupApi'
import { useAuth } from '../context/AuthContext'
import SoftFamilyIcon from './SoftFamilyIcon'

function Sidebar() {
  const { t, i18n } = useTranslation()
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
    <div className="sidebar-logo"><div className="brand-icon"><img className="fp-brand-reference" src="/assets/soft-family/reference/brand-symbol.webp" alt="" onError={e => { e.currentTarget.hidden = true }} /><SoftFamilyIcon name="family" size={42} /></div><div><strong>FamilyPoints</strong><span>{t('workspaceUi.brandSubtitle')}</span></div></div>
    <nav className="sidebar-nav">
      <NavLink to="/" end className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}><SoftFamilyIcon name="home" size={22} /><span>{t('navigation.dashboard')}</span></NavLink>
      <NavLink to="/tasks" className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}><SoftFamilyIcon name="check" size={22} /><span>{t('navigation.tasks')}</span></NavLink>
      <NavLink to="/calendar" className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}><SoftFamilyIcon name="check" size={22} /><span>{({uk:'Календар',de:'Kalender',en:'Calendar',ru:'Календарь'})[i18n.language.split('-')[0]] || 'Calendar'}</span></NavLink>
      <NavLink to="/points" className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}><SoftFamilyIcon name="points" size={22} /><span>{t('navigation.points')}</span></NavLink>
      <NavLink to="/rewards" className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}><SoftFamilyIcon name="gift" size={22} /><span>{t('navigation.rewards')}</span></NavLink>
      {roots.length > 0 && <div className="sidebar-dynamic-groups">{roots.map(g => renderGroup(g))}</div>}
    </nav>
    <div className="fp-sidebar-promo" aria-hidden="true"><span>Сімейні завдання, бали та мотивація в одному просторі!</span><img src="/assets/soft-family/reference/sidebar-family.webp" alt=""
        onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = '/assets/soft-family/family-illustration.svg' }} /></div>
    <div className="sidebar-bottom"><NavLink to="/download" className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}><SoftFamilyIcon name="gift" size={22} /><span>{t('androidDownload.nav')}</span></NavLink><NavLink to="/members" className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}><SoftFamilyIcon name="family" size={22} /><span>{t('workspaceUi.members')}</span></NavLink><NavLink to="/settings" className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}><SoftFamilyIcon name="settings" size={22} /><span>{t('navigation.settings')}</span></NavLink></div>
  </aside>
}
export default Sidebar
