import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getCalendar, getCalendarTimeZone, setCalendarTimeZone } from '../api/calendarApi'

const copy = {
  uk: { title:'Календар',from:'Від',to:'До',type:'Тип',all:'Усі',tasks:'Завдання',rewards:'Винагороди',load:'Оновити',empty:'Немає подій',zone:'Часовий пояс сім’ї',save:'Зберегти',deadline:'Дедлайн',window:'Час отримання винагороди',permission:'Змінювати часовий пояс може лише адміністратор' },
  de: { title:'Kalender',from:'Von',to:'Bis',type:'Typ',all:'Alle',tasks:'Aufgaben',rewards:'Belohnungen',load:'Aktualisieren',empty:'Keine Ereignisse',zone:'Familienzeitzone',save:'Speichern',deadline:'Frist',window:'Belohnungszeitfenster',permission:'Nur Administratoren dürfen die Zeitzone ändern' },
  en: { title:'Calendar',from:'From',to:'To',type:'Type',all:'All',tasks:'Tasks',rewards:'Rewards',load:'Refresh',empty:'No events',zone:'Family time zone',save:'Save',deadline:'Deadline',window:'Reward availability window',permission:'Only administrators can update the time zone' },
  ru: { title:'Календарь',from:'От',to:'До',type:'Тип',all:'Все',tasks:'Задания',rewards:'Награды',load:'Обновить',empty:'Нет событий',zone:'Часовой пояс семьи',save:'Сохранить',deadline:'Срок',window:'Время получения награды',permission:'Только администратор может изменить часовой пояс' },
}
const dateValue = (date) => [date.getFullYear(),String(date.getMonth()+1).padStart(2,'0'),String(date.getDate()).padStart(2,'0')].join('-')
export default function CalendarPage() {
  const { i18n } = useTranslation()
  const t = copy[i18n.language.split('-')[0]] || copy.en
  const { currentUser, getAccessToken } = useAuth()
  const [from,setFrom] = useState(()=>dateValue(new Date()))
  const [to,setTo] = useState(()=>{const d=new Date();d.setDate(d.getDate()+13);return dateValue(d)})
  const [type,setType] = useState('ALL')
  const [events,setEvents] = useState([])
  const [zone,setZone] = useState('')
  const [error,setError] = useState('')
  const [busy,setBusy] = useState(false)
  const [reload,setReload] = useState(0)
  const workspaceId=currentUser?.workspaceId
  const canEdit=currentUser?.permissions?.some(p=>['MANAGE_WORKSPACE','ADMIN_OVERRIDE'].includes(p))
  useEffect(()=>{
    if (!workspaceId) return
    let alive=true
    getCalendarTimeZone(getAccessToken(),workspaceId).then(value=>{
      if(alive) setZone(typeof value==='string'?value:value.timeZone || value.workspaceTimeZone || 'UTC')
    }).catch(()=>{})
    return ()=>{alive=false}
  },[workspaceId,getAccessToken])
  useEffect(()=>{
    if(!workspaceId) return
    let alive=true
    if(!from||!to||from>to||Math.round((new Date(to)-new Date(from))/86400000)>=62){
      setError('Select a valid date range of up to 62 days')
      setEvents([])
      return
    }
    getCalendar(getAccessToken(),workspaceId,{from,to,type}).then(data=>{
      if(!alive)return
      setEvents(Array.isArray(data)?data:[])
      setError('')
    }).catch(e=>{if(alive){setEvents([]);setError(e.message)}})
    return ()=>{alive=false}
  },[workspaceId,from,to,type,reload,getAccessToken])
  const saveZone=async()=>{
    setBusy(true);setError('')
    try{await setCalendarTimeZone(getAccessToken(),workspaceId,zone);setReload(v=>v+1)}
    catch(e){setError(e.message)}
    finally{setBusy(false)}
  }
  return <main className="calendar-page">
    <h1>{t.title}</h1>
    <div className="settings-section" style={{display:'flex',gap:12,flexWrap:'wrap',alignItems:'end'}}>
      <label>{t.from}<input type="date" value={from} onChange={e=>setFrom(e.target.value)} /></label>
      <label>{t.to}<input type="date" min={from} value={to} onChange={e=>setTo(e.target.value)} /></label>
      <label>{t.type}<select value={type} onChange={e=>setType(e.target.value)}><option value="ALL">{t.all}</option><option value="TASK">{t.tasks}</option><option value="REWARD">{t.rewards}</option></select></label>
      <button className="secondary-button" onClick={()=>setReload(v=>v+1)}>{t.load}</button>
    </div>
    {error && <p role="alert" className="error-message">{error}</p>}
    <section className="settings-section">
      <h2>{t.zone}</h2>
      <label>{t.zone}<input value={zone} onChange={e=>setZone(e.target.value)} disabled={!canEdit||busy}/></label>
      {canEdit?<button className="secondary-button" disabled={busy||!zone.trim()} onClick={saveZone}>{t.save}</button>:<small>{t.permission}</small>}
    </section>
    <section className="settings-section" aria-live="polite">
      {events.length===0?<p>{t.empty}</p>:<div style={{display:'grid',gap:10}}>
        {events.map((event,index)=><article className="account-card" key={event.key||index} style={{overflowWrap:'anywhere'}}>
          <small>{event.date} · {event.kind==='REWARD_WINDOW'?t.window:t.deadline} {event.scheduledTime||''}</small>
          <strong>{event.title}</strong><span>{event.status}</span>
        </article>)}
      </div>}
    </section>
  </main>
}
