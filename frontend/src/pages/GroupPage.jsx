import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { getMemberGroups } from '../api/memberGroupApi'
import { useAuth } from '../context/AuthContext'

export default function GroupPage() {
  const { t } = useTranslation()
  const { groupId } = useParams()
  const { currentUser, getAccessToken } = useAuth()
  const [groups, setGroups] = useState([])
  const [tab, setTab] = useState('overview')
  const [error, setError] = useState('')
  useEffect(() => { getMemberGroups(getAccessToken(), currentUser.workspaceId).then(setGroups).catch(e => setError(e.message)) }, [groupId, currentUser.workspaceId, getAccessToken])
  const group = useMemo(() => groups.find(g => g.id === Number(groupId)), [groups, groupId])
  if (error) return <div className="page-error">{error}</div>
  if (!group) return <div className="page-loading">Завантаження…</div>
  return <>
    <div className="page-title"><h1>{group.name}</h1><p>{group.description || 'Робочий простір групи'}</p></div>
    <div className="page-tabs">
      {['overview','tasks','members','points'].map(x => <button key={x} type="button" className={tab === x ? 'page-tab active' : 'page-tab'} onClick={() => setTab(x)}>{{overview:'Огляд',tasks:'Завдання',members:'Учасники',points:'Бали'}[x]}</button>)}
    </div>
    {tab === 'overview' && <div className="dashboard-stats"><div className="stat-card"><span className="stat-label">Учасники</span><strong className="stat-value">{group.members?.length || 0}</strong><span className="stat-description">безпосередньо в групі</span></div><div className="stat-card"><span className="stat-label">Підгрупи</span><strong className="stat-value">{group.childGroups?.length || 0}</strong><span className="stat-description">вкладені групи</span></div><div className="stat-card"><span className="stat-label">Ролі</span><strong className="stat-value">{group.roles?.length || 0}</strong><span className="stat-description">ролей групи</span></div></div>}
    {tab === 'members' && <section className="dashboard-panel"><h2>Учасники</h2>{(group.members || []).map(m => <div className="group-edit-item" key={m.memberId}><span>{m.memberName}</span></div>)}</section>}
    {tab === 'points' && <section className="dashboard-panel"><h2>Баланс групи</h2>{(group.balances || []).map(b => <div className="balance-row" key={b.pointTypeId}><strong>{t(`pointType.${b.code}`, { defaultValue: b.name || b.code })}</strong><span>{b.amount}</span></div>)}</section>}
    {tab === 'tasks' && <section className="dashboard-panel"><h2>Завдання групи</h2><div className="empty-state compact">Фільтр завдань за групою буде підключений у наступному кроці.</div></section>}
  </>
}
