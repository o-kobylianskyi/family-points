import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import SoftFamilyIcon from '../components/SoftFamilyIcon'
import { useAuth } from '../context/AuthContext'
import { useCurrencyQuantity } from '../utils/useCurrencyQuantity'
import { getMyReputation, getMyReputationHistory } from '../api/reputationApi'
import { getMemberBalance } from '../api/pointsApi'
import { getMemberTasks, getOpenTasks } from '../api/taskApi'

function localToday() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function StatCard({ icon, tone, label, value, note, destination }) {
  return <Link to={destination} className={`fp-stat fp-stat--${tone}`}>
    <span className="fp-stat-icon"><img src={`/assets/soft-family/reference/icon-${icon}.webp`} alt="" onError={event => { event.currentTarget.hidden = true }} /><SoftFamilyIcon name={icon} size={56} className="fp-stat-svg-fallback" /></span>
    <span className="fp-stat-copy">
      <span className="fp-stat-label">{label}</span>
      <strong className="fp-stat-value">{value}</strong>
      {note && <span className="fp-stat-note">{note}</span>}
    </span>
    <span className="fp-stat-arrow" aria-hidden="true">›</span>
  </Link>
}

export default function DashboardPage() {
  const { currentUser, getAccessToken } = useAuth()
  const { t } = useTranslation()
  const currency = useCurrencyQuantity(getAccessToken(), currentUser.workspaceId)
  const [data, setData] = useState({ balance: 0, reputation: null, history: [], tasks: [], open: [] })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    ;(async () => {
      try {
        const token = getAccessToken()
        const family = currentUser.workspaceId
        const member = currentUser.memberId
        const date = localToday()
        const [balance, tasks, open, reputation, history] = await Promise.all([
          getMemberBalance(token, family, member),
          getMemberTasks(token, family, member, date),
          getOpenTasks(token, family, date, member),
          getMyReputation(token),
          getMyReputationHistory(token),
        ])
        if (!cancelled) setData({
          balance: balance.balance, tasks, open,
          reputation: reputation.balance, history,
        })
      } catch (e) {
        if (!cancelled) setError(e.message)
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => { cancelled = true }
  }, [currentUser.workspaceId, currentUser.memberId, getAccessToken])

  if (loading) return <div className="page-loading">{t('common.loading')}</div>
  if (error) return <div className="page-error">{error}</div>
  const active = data.tasks.filter(task => !['COMPLETED', 'EXCUSED'].includes(task.status))
  const history = data.history.slice(0, 5)

  return <div className="fp-dashboard">
    <section className="fp-hero">
      <div className="fp-hero-content">
        <h1>{t('dashboard.welcome', { name: currentUser.memberName })} <span aria-hidden="true">👋</span></h1>
        <p>{t('dashboardLive.description')}</p>
        <span className="fp-hero-motto">{t('dashboardLive.familyMotto')} <SoftFamilyIcon name="heart" size={22} /></span>
      </div>
      <img className="fp-hero-art" src="/assets/soft-family/reference/hero-family.webp" alt=""
        onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = '/assets/soft-family/family-illustration.svg' }} />
      <span className="fp-hero-calligraphy" aria-hidden="true">Малі справи<br/>— великі<br/>можливості ♡</span>
    </section>
    <div className="fp-stats">
      <StatCard icon="coins" tone="gold" label={t('dashboardLive.myBalance')} value={currency(data.balance, 'POINTS')} destination="/points" />
      <StatCard icon="star" tone="sage" label={t('dashboardLive.myReputation')} value={data.reputation ?? '—'} note={t('dashboardLive.reputationNote')} destination="/points" />
      <StatCard icon="checklist" tone="teal" label={t('dashboardLive.myTasks')} value={active.length} note={t('dashboardLive.activeToday')} destination="/tasks" />
      <StatCard icon="gift" tone="soft" label={t('dashboardLive.available')} value={data.open.length} note={t('dashboardLive.openTasks')} destination="/tasks" />
    </div>

    <div className="fp-dashboard-columns">
      <section className="fp-panel fp-tasks-panel">
        <header className="fp-panel-header">
          <h2>{t('dashboardLive.today')}</h2>
          <Link to="/tasks">{t('dashboardLive.allTasks')} <span aria-hidden="true">›</span></Link>
        </header>
        {active.length === 0 ? <div className="fp-empty">
          <span className="fp-empty-icon"><img src="/assets/soft-family/reference/icon-checklist.webp" alt="" onError={event => { event.currentTarget.hidden = true }} /><SoftFamilyIcon name="checklist" size={55} /></span>
          <strong>{t('dashboardLive.noTasks')}</strong>
        </div> : <div className="fp-task-rows">
          {active.slice(0, 5).map(task => <div className="fp-task-row" key={task.id}>
            <span className="fp-task-icon"><SoftFamilyIcon name="check" size={26} /></span>
            <span className="fp-task-main"><strong>{task.title}</strong>
              <small>{t(`taskStatus.${task.status}`, { defaultValue: task.status })}</small>
            </span>
            <span className="fp-task-badge"><SoftFamilyIcon name="star" size={19} /></span>
          </div>)}
        </div>}
      </section>
      <section className="fp-panel fp-history-panel">
        <header className="fp-panel-header"><div>
          <h2>{t('dashboardLive.reputationHistory')}</h2>
          <small>{t('dashboardLive.lastChanges')}</small>
        </div></header>
        {history.length === 0 ? <div className="fp-empty">
          <span className="fp-empty-icon"><img src="/assets/soft-family/reference/icon-star.webp" alt="" onError={event => { event.currentTarget.hidden = true }} /><SoftFamilyIcon name="star" size={52} /></span>
          <strong>{t('dashboardLive.noReputation')}</strong>
        </div> : <div className="fp-history-rows">
          {history.map((entry, index) => <div className="fp-history-row" key={entry.id ?? index}>
            <span className={`fp-history-symbol ${entry.amount >= 0 ? 'positive' : 'negative'}`} aria-hidden="true">{entry.amount >= 0 ? '↑' : '↓'}</span>
            <span className="fp-history-copy">
              <strong>{entry.amount > 0 ? '+' : ''}{entry.amount}</strong>
              <small>{entry.description || entry.sourceType}</small>
            </span>
            <time>{new Date(entry.createdAt).toLocaleDateString()}</time>
          </div>)}
        </div>}
      </section>
    </div>

    <section className="fp-panel fp-actions-panel">
      <header className="fp-panel-header"><h2>{t('dashboardLive.quickActions')}</h2></header>
      <div className="fp-actions">
        <Link to="/tasks"><img src="/assets/soft-family/reference/quick-plus.webp" alt="" onError={event => { event.currentTarget.hidden = true }} /><SoftFamilyIcon name="plus" size={36} />{t('navigation.tasks')}</Link>
        <Link to="/members"><img src="/assets/soft-family/reference/quick-family.webp" alt="" onError={event => { event.currentTarget.hidden = true }} /><SoftFamilyIcon name="family" size={36} />{t('workspaceUi.members')}</Link>
        <Link to="/rewards"><img src="/assets/soft-family/reference/quick-gift.webp" alt="" onError={event => { event.currentTarget.hidden = true }} /><SoftFamilyIcon name="gift" size={36} />{t('navigation.rewards')}</Link>
        <Link to="/points"><img src="/assets/soft-family/reference/quick-points.webp" alt="" onError={event => { event.currentTarget.hidden = true }} /><SoftFamilyIcon name="coins" size={36} />{t('navigation.points')}</Link>
      </div>
    </section>
  </div>
}
