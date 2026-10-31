import { useCurrencyQuantity } from '../utils/useCurrencyQuantity'
import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { getMemberGroups } from '../api/memberGroupApi'
import { useAuth } from '../context/AuthContext'
import EntityMedia from '../components/EntityMedia'

export default function GroupPage() {
  const { t } = useTranslation()
  const { groupId } = useParams()
  const { currentUser, getAccessToken } = useAuth()
  const currency = useCurrencyQuantity(getAccessToken(), currentUser.workspaceId)
  const [groups, setGroups] = useState([])
  const [tab, setTab] = useState('overview')
  const [error, setError] = useState('')
  useEffect(() => { getMemberGroups(getAccessToken(), currentUser.workspaceId).then(setGroups).catch(e => setError(e.message)) }, [groupId, currentUser.workspaceId, getAccessToken])
  const group = useMemo(() => groups.find(g => g.id === Number(groupId)), [groups, groupId])
  if (error) return <div className="page-error">{error}</div>
  if (!group) return <div className="page-loading">{t("common.loading")}</div>
  return <>
    <div className="page-title"><h1>{group.name}</h1><p>{group.description || t('groupPageUi.description')}</p></div>
    <EntityMedia type="MEMBER_GROUP" targetId={group.id} slot="ICON" canEdit={currentUser.permissions?.includes("MANAGE_GROUPS")||currentUser.permissions?.includes("ADMIN_OVERRIDE")} />
    <div className="page-tabs">
      {['overview','tasks','members','points'].map(x => <button key={x} type="button" className={tab === x ? 'page-tab active' : 'page-tab'} onClick={() => setTab(x)}>{t(`groupPageUi.${x}`)}</button>)}
    </div>
    {tab === 'overview' && <div className="dashboard-stats"><div className="stat-card"><span className="stat-label">{t('groupPageUi.members')}</span><strong className="stat-value">{group.members?.length || 0}</strong><span className="stat-description">{t('groupPageUi.direct')}</span></div><div className="stat-card"><span className="stat-label">{t('groupPageUi.subgroups')}</span><strong className="stat-value">{group.childGroups?.length || 0}</strong><span className="stat-description">{t('groupPageUi.nested')}</span></div><div className="stat-card"><span className="stat-label">{t('groupPageUi.roles')}</span><strong className="stat-value">{group.roles?.length || 0}</strong><span className="stat-description">{t('groupPageUi.groupRoles')}</span></div></div>}
    {tab === 'members' && <section className="dashboard-panel"><h2>{t('groupPageUi.members')}</h2>{(group.members || []).map(m => <div className="group-edit-item" key={m.memberId}><span>{m.memberName}</span></div>)}</section>}
    {tab === 'points' && <section className="dashboard-panel"><h2>{t('groupPageUi.groupBalance')}</h2>{(group.balances || []).map(b => <div className="balance-row" key={b.pointTypeId}><strong>{t(`pointType.${b.code}`, { defaultValue: b.name || b.code })}</strong><span>{currency(b.amount, b.code, b.pointTypeId, b.name)}</span></div>)}</section>}
    {tab === 'tasks' && <section className="dashboard-panel"><h2>{t('groupPageUi.groupTasks')}</h2><div className="empty-state compact">{t('groupPageUi.taskFilterPending')}</div></section>}
  </>
}
