import { useEffect, useRef, useState } from 'react'
import { getRewardPurchaseHistory } from '../api/rewardApi'
import { getWorkspaceMembers } from '../api/workspaceApi'
import './RewardPurchaseHistory.css'

const wording = {
  uk: {heading:'Історія покупок нагород', scope:'Показано покупки, а не підтвердження вручення чи використання.',all:'Усі учасники',member:'Учасник',status:'Статус',any:'Усі статуси',from:'Від',to:'До',apply:'Застосувати',reset:'Скинути',loading:'Завантаження історії…',empty:'Покупок за вибраними умовами немає.',more:'Показати ще',retry:'Повторити завантаження',error:'Не вдалося завантажити історію. Перевірте з’єднання.',purchased:'Куплено',resolved:'Оновлено',duration:'Тривалість',minutes:'хв',pending:'Очікує схвалення',ready:'Готова',redeemed:'Погашена',cancelled:'Скасована',unknown:'Статус сервера'},
  ru: {heading:'История покупок наград',scope:'Показаны покупки, а не подтверждение вручения или использования.',all:'Все участники',member:'Участник',status:'Статус',any:'Все статусы',from:'От',to:'До',apply:'Применить',reset:'Сбросить',loading:'Загрузка истории…',empty:'Покупок по заданным условиям нет.',more:'Показать ещё',retry:'Повторить загрузку',error:'Не удалось загрузить историю. Проверьте соединение.',purchased:'Куплено',resolved:'Обновлено',duration:'Длительность',minutes:'мин',pending:'Ожидает одобрения',ready:'Готова',redeemed:'Погашена',cancelled:'Отменена',unknown:'Статус сервера'},
  de: {heading:'Belohnungskäufe',scope:'Käufe sind kein Nachweis für Übergabe oder Nutzung.',all:'Alle Mitglieder',member:'Mitglied',status:'Status',any:'Alle Status',from:'Von',to:'Bis',apply:'Anwenden',reset:'Zurücksetzen',loading:'Verlauf wird geladen…',empty:'Keine Käufe für diese Filter.',more:'Mehr laden',retry:'Erneut laden',error:'Verlauf konnte nicht geladen werden. Verbindung prüfen.',purchased:'Gekauft',resolved:'Aktualisiert',duration:'Dauer',minutes:'Min.',pending:'Genehmigung ausstehend',ready:'Bereit',redeemed:'Eingelöst',cancelled:'Storniert',unknown:'Serverstatus'},
  en: {heading:'Reward purchase history',scope:'Purchase records do not confirm delivery or use.',all:'All members',member:'Member',status:'Status',any:'All statuses',from:'From',to:'To',apply:'Apply',reset:'Reset',loading:'Loading history…',empty:'No purchases match these filters.',more:'Load more',retry:'Retry',error:'Could not load history. Check the connection.',purchased:'Purchased',resolved:'Updated',duration:'Duration',minutes:'min',pending:'Pending approval',ready:'Ready',redeemed:'Redeemed',cancelled:'Cancelled',unknown:'Server status'}
}
const statuses = ['PENDING_APPROVAL', 'READY', 'REDEEMED', 'CANCELLED']
const statusLabels = {PENDING_APPROVAL:'pending', READY:'ready', REDEEMED:'redeemed', CANCELLED:'cancelled'}
const dateTime = (value, language) => {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : new Intl.DateTimeFormat(language, {dateStyle:'medium',timeStyle:'short'}).format(date)
}
const validPage = result => result && Array.isArray(result.items) &&
  typeof result.hasMore === 'boolean' &&
  (result.nextCursor == null || typeof result.nextCursor === 'string') &&
  (!result.hasMore || !!result.nextCursor)

export default function RewardPurchaseHistory({workspaceId, memberId, accountId, canManage, getAccessToken, language = 'en'}) {
  const words = wording[language] || wording.en
  const session = [accountId, workspaceId, memberId, canManage].join(':')
  const sessionRef = useRef(session)
  sessionRef.current = session
  const requestId = useRef(0)
  const [filters, setFilters] = useState({memberId:'',status:'',from:'',to:''})
  const [applied, setApplied] = useState({memberId:'',status:'',from:'',to:''})
  const [members, setMembers] = useState([])
  const [items, setItems] = useState([])
  const [cursor, setCursor] = useState(null)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [generation, setGeneration] = useState(0)
  const busy = useRef(false)
  const identityKey = [session, applied.memberId, applied.status, applied.from, applied.to].join('|')
  const identityRef = useRef(identityKey)
  identityRef.current = identityKey

  useEffect(() => {
    requestId.current += 1
    busy.current = false
    setFilters({memberId:'',status:'',from:'',to:''})
    setApplied({memberId:'',status:'',from:'',to:''})
    setMembers([]); setItems([]); setCursor(null); setHasMore(false); setError(''); setLoading(false)
    if (!canManage) return
    const observedSession = session
    getWorkspaceMembers(getAccessToken(), workspaceId)
      .then(data => {
        if (sessionRef.current === observedSession && Array.isArray(data)) setMembers(data)
      })
      .catch(() => { if (sessionRef.current === observedSession) setMembers([]) })
  }, [session])

  useEffect(() => {
    requestId.current += 1
    busy.current = false
    setItems([]); setCursor(null); setHasMore(false); setError('')
    const observedKey = identityKey
    const id = ++requestId.current
    setLoading(true); busy.current = true
    const options = { limit:25 }
    if (canManage && applied.memberId) options.memberId = applied.memberId
    if (applied.status) options.status = applied.status
    if (applied.from) options.from = applied.from + 'T00:00:00'
    if (applied.to) options.to = applied.to + 'T23:59:59'
    getRewardPurchaseHistory(getAccessToken(), workspaceId, options)
      .then(result => {
        if (id !== requestId.current || identityRef.current !== observedKey) return
        if (!validPage(result)) throw new Error('INVALID_PURCHASE_HISTORY_RESPONSE')
        setItems(result.items); setCursor(result.nextCursor); setHasMore(result.hasMore)
      })
      .catch(() => { if (id === requestId.current && identityRef.current === observedKey) setError(words.error) })
      .finally(() => {
        if (id === requestId.current && identityRef.current === observedKey) { setLoading(false); busy.current = false }
      })
  }, [identityKey, generation])

  const loadMore = async () => {
    if (busy.current || !hasMore || !cursor) return
    busy.current = true; setLoading(true); setError('')
    const observedKey = identityKey, id = ++requestId.current
    const options = {limit:25,cursor}
    if (canManage && applied.memberId) options.memberId = applied.memberId
    if (applied.status) options.status = applied.status
    if (applied.from) options.from = applied.from + 'T00:00:00'
    if (applied.to) options.to = applied.to + 'T23:59:59'
    try {
      const result = await getRewardPurchaseHistory(getAccessToken(), workspaceId, options)
      if (id !== requestId.current || identityRef.current !== observedKey) return
      if (!validPage(result)) throw new Error('INVALID_PURCHASE_HISTORY_RESPONSE')
      setItems(current => {
        const seen = new Set(current.map(item => String(item.id)))
        return [...current, ...result.items.filter(item => !seen.has(String(item.id)))]
      })
      setCursor(result.nextCursor); setHasMore(result.hasMore)
    } catch {
      if (id === requestId.current && identityRef.current === observedKey) setError(words.error)
    } finally {
      if (id === requestId.current && identityRef.current === observedKey) {busy.current = false;setLoading(false)}
    }
  }
  const apply = event => {
    event.preventDefault()
    if (filters.from && filters.to && filters.from > filters.to) return
    setApplied({...filters, memberId:canManage ? filters.memberId : ''})
    setGeneration(value => value + 1)
  }
  const reset = () => {
    const fresh = {memberId:'',status:'',from:'',to:''}
    setFilters(fresh);setApplied(fresh);setGeneration(value=>value+1)
  }
  return <section className="reward-history-panel">
    <h2>{words.heading}</h2>
    <p>{words.scope}</p>
    <form className="reward-history-filters" onSubmit={apply}>
      {canManage && <label>{words.member}
        <select value={filters.memberId} onChange={event=>setFilters(previous=>({...previous,memberId:event.target.value}))}>
          <option value="">{words.all}</option>
          {members.map(member=><option key={member.id} value={member.id}>{member.name || member.displayName || member.id}</option>)}
        </select>
      </label>}
      <label>{words.status}
        <select value={filters.status} onChange={event=>setFilters(previous=>({...previous,status:event.target.value}))}>
          <option value="">{words.any}</option>
          {statuses.map(status=><option key={status} value={status}>{words[statusLabels[status]]}</option>)}
        </select>
      </label>
      <label>{words.from}<input type="date" value={filters.from} onChange={event=>setFilters(previous=>({...previous,from:event.target.value}))}/></label>
      <label>{words.to}<input type="date" min={filters.from||undefined} value={filters.to} onChange={event=>setFilters(previous=>({...previous,to:event.target.value}))}/></label>
      <button type="submit" disabled={loading || !!(filters.from && filters.to && filters.from>filters.to)}>{words.apply}</button>
      <button type="button" disabled={loading} onClick={reset}>{words.reset}</button>
    </form>
    {error && <p className="page-error" role="alert">{error} <button type="button" onClick={()=>setGeneration(value=>value+1)}>{words.retry}</button></p>}
    {loading && <p role="status">{words.loading}</p>}
    {!loading && !error && items.length===0 && <p className="empty-state">{words.empty}</p>}
    <div className="reward-history-list">
      {items.map(purchase=><article className="reward-history-item" key={purchase.id}>
        <div><strong>{purchase.rewardTitle}</strong><span>{words[statusLabels[purchase.status]] || `${words.unknown}: ${purchase.status || '—'}`}</span></div>
        <div><span>{purchase.memberName || ''}</span><span>{purchase.priceAmount ?? '—'} {purchase.pointTypeCode || ''}</span></div>
        <div><span>{words.purchased}: {dateTime(purchase.purchasedAt,language)}</span>
          {purchase.resolvedAt && <span>{words.resolved}: {dateTime(purchase.resolvedAt,language)}</span>}
          {purchase.durationMinutes != null && <span>{words.duration}: {purchase.durationMinutes} {words.minutes}</span>}
        </div>
      </article>)}
    </div>
    {hasMore && <button type="button" className="secondary-button" disabled={loading} onClick={loadMore}>{words.more}</button>}
  </section>
}
