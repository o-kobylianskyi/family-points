import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getMemberBalance } from '../api/pointsApi'
import { getMemberTasks, getOpenTasks } from '../api/taskApi'
import { useTranslation } from 'react-i18next'

function today() { const d=new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` }
export default function DashboardPage() {
  const { currentUser, getAccessToken } = useAuth(); const { t } = useTranslation()
  const [balance,setBalance]=useState(0), [tasks,setTasks]=useState([]), [open,setOpen]=useState([]), [error,setError]=useState(''), [loading,setLoading]=useState(true)
  useEffect(() => { (async()=>{try{const token=getAccessToken(), f=currentUser.workspaceId, m=currentUser.memberId, date=today(); const [b,ts,os]=await Promise.all([getMemberBalance(token,f,m),getMemberTasks(token,f,m,date),getOpenTasks(token,f,date,m)]); setBalance(b.balance); setTasks(ts); setOpen(os)}catch(e){setError(e.message)}finally{setLoading(false)}})() },[currentUser.workspaceId,currentUser.memberId,getAccessToken])
  if(loading)return <div className="page-loading">{t('common.loading')}</div>; if(error)return <div className="page-error">{error}</div>
  const active=tasks.filter(x=>!['COMPLETED','EXCUSED'].includes(x.status))
  return <>
    <div className="dashboard-welcome"><div><h1>{t('dashboard.welcome',{name:currentUser.memberName})}</h1><p>Ваш робочий простір на сьогодні</p></div></div>
    <div className="dashboard-stats"><div className="stat-card"><span className="stat-label">Мій баланс</span><strong className="stat-value">{balance}</strong><span className="stat-description">{t('common.points')}</span></div><div className="stat-card"><span className="stat-label">Мої завдання</span><strong className="stat-value">{active.length}</strong><span className="stat-description">активних сьогодні</span></div><div className="stat-card"><span className="stat-label">Доступно взяти</span><strong className="stat-value">{open.length}</strong><span className="stat-description">відкритих завдань</span></div></div>
    <div className="dashboard-grid"><section className="dashboard-panel"><div className="panel-header"><div><h2>Сьогодні</h2><p>Ваші актуальні завдання</p></div><Link to="/tasks" className="secondary-button">Усі завдання</Link></div>{active.length===0?<div className="empty-state compact">На сьогодні активних завдань немає.</div>:<div className="task-list">{active.slice(0,5).map(x=><article className="task-card" key={x.id}><div className="task-card-main"><h3>{x.title}</h3><span>{x.status}</span></div></article>)}</div>}</section><section className="dashboard-panel"><div className="panel-header"><div><h2>Швидкі дії</h2><p>Основні розділи</p></div></div><div className="dashboard-quick-actions"><Link to="/tasks" className="primary-button">Завдання</Link><Link to="/points" className="secondary-button">Бали</Link><Link to="/rewards" className="secondary-button">Нагороди</Link></div></section></div>
  </>
}
