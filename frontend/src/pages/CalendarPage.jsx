import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getCalendar, getCalendarTimeZone, setCalendarTimeZone } from '../api/calendarApi'
import './CalendarPage.css'

const translations={
 uk:{title:'Календар',today:'Сьогодні',day:'День',week:'Тиждень',month:'Місяць',tasks:'Завдання',rewards:'Винагороди',all:'Усі',noEvents:'На цей день подій немає',timezone:'Часовий пояс',save:'Зберегти',deadline:'Дедлайн',window:'Вікно отримання',planned:'Заплановано',loading:'Оновлення',untimed:'Без точного часу',notStart:'Час означає дедлайн, а не початок завдання',settings:'Налаштування часу',retry:'Повторити'},
 de:{title:'Kalender',today:'Heute',day:'Tag',week:'Woche',month:'Monat',tasks:'Aufgaben',rewards:'Belohnungen',all:'Alle',noEvents:'Keine Ereignisse an diesem Tag',timezone:'Zeitzone',save:'Speichern',deadline:'Frist',window:'Belohnungsfenster',planned:'Geplant',loading:'Aktualisieren',untimed:'Ohne Uhrzeit',notStart:'Aufgabenzeit ist die Frist, nicht die Startzeit',settings:'Zeiteinstellungen',retry:'Erneut laden'},
 en:{title:'Calendar',today:'Today',day:'Day',week:'Week',month:'Month',tasks:'Tasks',rewards:'Rewards',all:'All',noEvents:'No events for this day',timezone:'Time zone',save:'Save',deadline:'Deadline',window:'Reward window',planned:'Planned',loading:'Updating',untimed:'No exact time',notStart:'Task times are deadlines, not start times',settings:'Time settings',retry:'Retry'},
 ru:{title:'Календарь',today:'Сегодня',day:'День',week:'Неделя',month:'Месяц',tasks:'Задания',rewards:'Награды',all:'Все',noEvents:'На этот день событий нет',timezone:'Часовой пояс',save:'Сохранить',deadline:'Срок',window:'Окно получения',planned:'Запланировано',loading:'Обновление',untimed:'Без точного времени',notStart:'Время задания — срок, а не начало',settings:'Настройки времени',retry:'Повторить'},
}
const timezoneChoices = [
  ['Europe/Berlin','Berlin / Deutschland'],
  ['Europe/Kyiv','Київ / Україна'],
  ['Europe/Warsaw','Warszawa / Polska'],
  ['Europe/Prague','Praha / Česko'],
  ['Europe/Vienna','Wien / Österreich'],
  ['Europe/Paris','Paris / France'],
  ['Europe/London','London / United Kingdom'],
  ['Europe/Rome','Roma / Italia'],
  ['Europe/Madrid','Madrid / España'],
  ['Europe/Amsterdam','Amsterdam / Nederland'],
  ['Europe/Zurich','Zürich / Schweiz'],
  ['Europe/Helsinki','Helsinki / Suomi'],
  ['Europe/Bucharest','București / România'],
  ['Europe/Istanbul','İstanbul / Türkiye'],
  ['America/New_York','New York / USA'],
  ['America/Chicago','Chicago / USA'],
  ['America/Los_Angeles','Los Angeles / USA'],
  ['Asia/Tokyo','Tokyo / 日本'],
  ['UTC','UTC'],
]
const iso=d=>[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-')
const fromISO=s=>new Date(s+'T12:00:00')
const add=(s,n)=>{const d=fromISO(s);d.setDate(d.getDate()+n);return iso(d)}
const monday=s=>{const d=fromISO(s);d.setDate(d.getDate()-(d.getDay()+6)%7);return iso(d)}
const firstMonth=s=>s.slice(0,7)+'-01'
const lastMonth=s=>{const d=fromISO(firstMonth(s));d.setMonth(d.getMonth()+1);d.setDate(0);return iso(d)}
const dateTime=(day,hour)=>new Date(day+'T'+hour+':00')
const daysBetween=(start,end)=>Math.round((fromISO(end)-fromISO(start))/86400000)+1
const dateLabel=(value,lang,options)=>new Intl.DateTimeFormat(lang,options).format(fromISO(value))
const clock=value=>{if(!value)return null;const m=String(value).match(/(?:T|^)(\d{2}):(\d{2})/);return m?m[1]+':'+m[2]:null}
function eventTime(e){
  if(e.kind==='REWARD_WINDOW')return clock(e.startsAt)
  return clock(e.scheduledTime)||clock(e.dueAt)
}
function EventCard({event,t}){
  const reward=event.kind==='REWARD_WINDOW'
  const time=eventTime(event)
  const content=<><span className="fp-cal-event-dot" aria-hidden="true"/><span className="fp-cal-event-text"><strong>{event.title}</strong><small>{reward?t.window:t.deadline}{event.status?' · '+event.status:''}</small></span>{time&&<time>{time}</time>}</>
  const destination=reward&&event.rewardId?'/rewards':!reward&&event.taskDefinitionId?'/tasks/'+event.taskDefinitionId:null
  return destination?<Link className={'fp-cal-event '+(reward?'reward':'task')} to={destination}>{content}</Link>:<div className={'fp-cal-event '+(reward?'reward':'task')}>{content}</div>
}
export default function CalendarPage(){
  const {i18n}=useTranslation()
  const lang=i18n.language.split('-')[0]
  const t=translations[lang]||translations.en
  const {currentUser,getAccessToken}=useAuth()
  const workspaceId=currentUser.workspaceId
  const canManage=currentUser.permissions?.some(p=>['MANAGE_WORKSPACE','ADMIN_OVERRIDE'].includes(p))
  const [focus,setFocus]=useState(()=>iso(new Date()))
  const [view,setView]=useState('week')
  const [type,setType]=useState('ALL')
  const [events,setEvents]=useState([])
  const [loading,setLoading]=useState(true)
  const [error,setError]=useState('')
  const [zone,setZone]=useState('')
  const [zoneBusy,setZoneBusy]=useState(false)
  const [showSettings,setShowSettings]=useState(false)
  const [refresh,setRefresh]=useState(0)
  const interval=useMemo(()=>{
    if(view==='day')return {from:focus,to:focus}
    if(view==='week'){const from=monday(focus);return {from,to:add(from,6)}}
    const from=monday(firstMonth(focus))
    const end=lastMonth(focus)
    return {from,to:add(end,6-(fromISO(end).getDay()+6)%7)}
  },[focus,view])
  useEffect(()=>{let active=true;setZone('')
    getCalendarTimeZone(getAccessToken(),workspaceId).then(r=>{if(active)setZone(typeof r==='string'?r:r.timeZone||r.workspaceTimeZone||'UTC')}).catch(()=>{})
    return()=>{active=false}
  },[workspaceId,getAccessToken])
  useEffect(()=>{
    let active=true;setLoading(true);setError('')
    if(daysBetween(interval.from,interval.to)>62){setError('Range exceeds 62 days');setLoading(false);return}
    getCalendar(getAccessToken(),workspaceId,{...interval,type}).then(data=>{
      if(active){setEvents(Array.isArray(data)?data:[]);setLoading(false)}
    }).catch(e=>{if(active){setError(e.message);setLoading(false)}})
    return()=>{active=false}
  },[workspaceId,interval,type,getAccessToken,refresh])
  const grouped=useMemo(()=>{
    const map=new Map()
    for(const e of events){if(!map.has(e.date))map.set(e.date,[]);map.get(e.date).push(e)}
    return map
  },[events])
  const onDay=day=>{setFocus(day);setView('day')}
  const go=delta=>{
    if(view==='day')setFocus(add(focus,delta))
    if(view==='week')setFocus(add(focus,delta*7))
    if(view==='month'){const d=fromISO(focus);d.setDate(1);d.setMonth(d.getMonth()+delta);setFocus(iso(d))}
  }
  const periodLabel=view==='day'?dateLabel(focus,lang,{weekday:'long',day:'numeric',month:'long',year:'numeric'}):
    view==='week'?dateLabel(interval.from,lang,{day:'numeric',month:'short'})+' — '+dateLabel(interval.to,lang,{day:'numeric',month:'short',year:'numeric'}):
    dateLabel(focus,lang,{month:'long',year:'numeric'})
  const dates=Array.from({length:daysBetween(interval.from,interval.to)},(_,i)=>add(interval.from,i))
  const saveZone=async e=>{e.preventDefault();setZoneBusy(true);setError('')
    try{await setCalendarTimeZone(getAccessToken(),workspaceId,zone);setRefresh(x=>x+1)}
    catch(err){setError(err.message)}finally{setZoneBusy(false)}
  }
  const dayEvents=grouped.get(focus)||[]
  return <main className="fp-calendar">
    <header className="fp-cal-heading"><div><h1>{t.title}</h1><p>{t.notStart}</p></div>
      <button className="secondary-button" type="button" onClick={()=>setShowSettings(s=>!s)} aria-expanded={showSettings}>{t.settings}</button>
    </header>
    {showSettings&&<form className="fp-cal-settings" onSubmit={saveZone}>
      <label>{t.timezone}
        <select value={zone} disabled={!canManage||zoneBusy||!zone} onChange={e=>setZone(e.target.value)}>
          {zone&&!timezoneChoices.some(([id])=>id===zone)&&<option value={zone}>{zone}</option>}
          {!zone&&<option value="">—</option>}
          {timezoneChoices.map(([id,label])=><option key={id} value={id}>{label} ({id})</option>)}
        </select>
      </label>
      {canManage&&<button type="submit" disabled={zoneBusy||!zone.trim()} className="secondary-button">{t.save}</button>}
    </form>}
    <section className="fp-cal-surface">
      <div className="fp-cal-toolbar">
        <div className="fp-cal-controls"><button type="button" className="secondary-button" onClick={()=>setFocus(iso(new Date()))}>{t.today}</button><button type="button" className="fp-cal-arrow" aria-label="Previous" onClick={()=>go(-1)}>‹</button><button type="button" className="fp-cal-arrow" aria-label="Next" onClick={()=>go(1)}>›</button><strong className="fp-cal-period">{periodLabel}</strong></div>
        <div className="fp-cal-controls"><select aria-label={t.all} value={type} onChange={e=>setType(e.target.value)}><option value="ALL">{t.all}</option><option value="TASK">{t.tasks}</option><option value="REWARD">{t.rewards}</option></select><div className="fp-cal-segments">{['day','week','month'].map(x=><button type="button" key={x} className={view===x?'selected':''} onClick={()=>setView(x)}>{t[x]}</button>)}</div></div>
      </div>
      {error&&<div role="alert" className="fp-cal-error">{error}<button className="secondary-button" onClick={()=>setRefresh(x=>x+1)}>{t.retry}</button></div>}
      <div className={'fp-cal-content'+(loading?' is-loading':'')} aria-busy={loading}>
        {view==='month'&&<div className="fp-cal-month">
          {Array.from({length:7},(_,i)=><div key={i} className="fp-cal-weekday">{dateLabel(add(monday(focus),i),lang,{weekday:'short'})}</div>)}
          {dates.map(day=><button type="button" key={day} className={'fp-cal-cell'+(day===iso(new Date())?' today':'')+(day.slice(0,7)!==focus.slice(0,7)?' outside':'')} onClick={()=>onDay(day)}>
            <span className="fp-cal-date">{Number(day.slice(-2))}</span><span className="fp-cal-cell-events">{(grouped.get(day)||[]).slice(0,3).map((e,i)=><span className={'fp-cal-mini '+(e.kind==='REWARD_WINDOW'?'reward':'task')} key={e.key||i}>{eventTime(e)||'•'} {e.title}</span>)}{(grouped.get(day)||[]).length>3&&<small>+{grouped.get(day).length-3}</small>}</span>
          </button>)}
        </div>}
        {view==='week'&&<div className="fp-cal-week">
          {dates.map(day=><section key={day} className={'fp-cal-day-column'+(day===iso(new Date())?' today':'')}>
            <button className="fp-cal-day-title" onClick={()=>onDay(day)}><small>{dateLabel(day,lang,{weekday:'short'})}</small><strong>{Number(day.slice(-2))}</strong></button>
            <div className="fp-cal-day-items">{(grouped.get(day)||[]).map((e,i)=><EventCard key={e.key||i} event={e} t={t}/>)}</div>
          </section>)}
        </div>}
        {view==='day'&&<div className="fp-cal-timeline">
          <div className="fp-cal-timeline-title">{dateLabel(focus,lang,{weekday:'long',day:'numeric',month:'long'})}</div>
          {Array.from({length:24},(_,hour)=>{
            const hourly=dayEvents.filter(e=>eventTime(e)&&Number(eventTime(e).slice(0,2))===hour)
            return <div className="fp-cal-hour" key={hour}><time>{String(hour).padStart(2,'0')}:00</time><div className="fp-cal-hour-content">{hourly.map((e,i)=><EventCard key={e.key||i} event={e} t={t}/>)}</div></div>
          })}
          {dayEvents.some(e=>!eventTime(e))&&<div className="fp-cal-untimed"><strong>{t.untimed}</strong>{dayEvents.filter(e=>!eventTime(e)).map((e,i)=><EventCard key={e.key||i} event={e} t={t}/>)}</div>}
        </div>}
        {!loading&&!error&&events.length===0&&<p className="fp-cal-empty">{t.noEvents}</p>}
        {loading&&<span className="fp-cal-loading" role="status">{t.loading}…</span>}
      </div>
    </section>
  </main>
}
