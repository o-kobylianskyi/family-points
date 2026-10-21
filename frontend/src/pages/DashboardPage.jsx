import { useCurrencyQuantity } from '../utils/useCurrencyQuantity'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getMyReputation, getMyReputationHistory } from '../api/reputationApi'
import { getMemberBalance } from '../api/pointsApi'
import { getMemberTasks, getOpenTasks } from '../api/taskApi'
import { useTranslation } from 'react-i18next'

function today() { const d=new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` }
export default function DashboardPage() {
  const { currentUser, getAccessToken } = useAuth(); const { t } = useTranslation()
  const currency = useCurrencyQuantity(getAccessToken(), currentUser.workspaceId)
  const [reputationHistory,setReputationHistory]=useState([]), [reputation,setReputation]=useState(null), [balance,setBalance]=useState(0), [tasks,setTasks]=useState([]), [open,setOpen]=useState([]), [error,setError]=useState(''), [loading,setLoading]=useState(true)
  useEffect(() => { (async()=>{try{const token=getAccessToken(), f=currentUser.workspaceId, m=currentUser.memberId, date=today(); const [b,ts,os,rep,history]=await Promise.all([getMemberBalance(token,f,m),getMemberTasks(token,f,m,date),getOpenTasks(token,f,date,m),getMyReputation(token),getMyReputationHistory(token)]); setBalance(b.balance); setReputation(rep.balance); setReputationHistory(history); setTasks(ts); setOpen(os)}catch(e){setError(e.message)}finally{setLoading(false)}})() },[currentUser.workspaceId,currentUser.memberId,getAccessToken])
  if(loading)return <div className="page-loading">{t('common.loading')}</div>; if(error)return <div className="page-error">{error}</div>
  const active=tasks.filter(x=>!['COMPLETED','EXCUSED'].includes(x.status))
  return <>
    <div className="dashboard-welcome"><div><h1>{t('dashboard.welcome',{name:currentUser.memberName})}</h1><p>{t('dashboardLive.description')}</p></div></div>
    <div className="dashboard-stats"><div className="stat-card"><span className="stat-label">{t('dashboardLive.myBalance')}</span><strong className="stat-value">{currency(balance, "POINTS")}</strong></div><div className="stat-card"><span className="stat-label">{t('dashboardLive.myReputation')}</span><strong className="stat-value">{reputation ?? "—"}</strong><span className="stat-description">{t('dashboardLive.reputationNote')}</span></div><div className="stat-card"><span className="stat-label">{t('dashboardLive.myTasks')}</span><strong className="stat-value">{active.length}</strong><span className="stat-description">{t('dashboardLive.activeToday')}</span></div><div className="stat-card"><span className="stat-label">{t('dashboardLive.available')}</span><strong className="stat-value">{open.length}</strong><span className="stat-description">{t('dashboardLive.openTasks')}</span></div></div>
    <section className="dashboard-panel">
      <div className="panel-header"><div><h2>{t('dashboardLive.reputationHistory')}</h2><p>{t('dashboardLive.lastChanges')}</p></div></div>
      {reputationHistory.length === 0 ? <div className="empty-state compact">{t('dashboardLive.noReputation')}</div> :
        <div className="task-list">
          {reputationHistory.slice(0, 5).map((entry) => (
            <div className="task-card" key={entry.id}>
              <strong>{entry.amount > 0 ? '+' : ''}{entry.amount}</strong>{' · '}
              {entry.description || entry.sourceType}
              <span> · {new Date(entry.createdAt).toLocaleString()}</span>
            </div>
          ))}
        </div>}
    </section>
    <div className="dashboard-grid"><section className="dashboard-panel"><div className="panel-header"><div><h2>{t('dashboardLive.today')}</h2><p>{t('dashboardLive.currentTasks')}</p></div><Link to="/tasks" className="secondary-button">{t('dashboardLive.allTasks')}</Link></div>{active.length===0?<div className="empty-state compact">{t('dashboardLive.noTasks')}</div>:<div className="task-list">{active.slice(0,5).map(x=><article className="task-card" key={x.id}><div className="task-card-main"><h3>{x.title}</h3><span>{t(`taskStatus.${x.status}`, { defaultValue: x.status })}</span></div></article>)}</div>}</section><section className="dashboard-panel"><div className="panel-header"><div><h2>{t('dashboardLive.quickActions')}</h2><p>{t('dashboardLive.mainSections')}</p></div></div><div className="dashboard-quick-actions"><Link to="/tasks" className="primary-button">{t('navigation.tasks')}</Link><Link to="/points" className="secondary-button">{t('navigation.points')}</Link><Link to="/rewards" className="secondary-button">{t('navigation.rewards')}</Link></div></section></div>
  </>
}
