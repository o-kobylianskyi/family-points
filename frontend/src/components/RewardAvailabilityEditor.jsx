import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getRewardAvailability, updateRewardAvailability } from '../api/rewardApi'

const textByLocale = {
  uk: {title:'Час отримання винагороди',mode:'Режим',always:'Завжди',once:'Одноразове вікно',weekly:'Щотижня',start:'Початок',end:'Кінець',day:'День тижня',from:'Від',to:'До',save:'Зберегти',saved:'Збережено',add:'Додати інтервал',remove:'Видалити',enabled:'Активний',effectiveFrom:'Діє від',effectiveTo:'Діє до',zone:'Часовий пояс',invalid:'Перевірте дати, час і перетини інтервалів',limit:'Максимум 56 інтервалів'},
  de: {title:'Belohnungszeitfenster',mode:'Modus',always:'Immer',once:'Einmalig',weekly:'Wöchentlich',start:'Beginn',end:'Ende',day:'Wochentag',from:'Von',to:'Bis',save:'Speichern',saved:'Gespeichert',add:'Zeitfenster hinzufügen',remove:'Entfernen',enabled:'Aktiv',effectiveFrom:'Gültig ab',effectiveTo:'Gültig bis',zone:'Zeitzone',invalid:'Datum, Uhrzeit und Überschneidungen prüfen',limit:'Maximal 56 Zeitfenster'},
  en: {title:'Reward availability',mode:'Mode',always:'Always',once:'One-time window',weekly:'Weekly',start:'Start',end:'End',day:'Weekday',from:'From',to:'To',save:'Save',saved:'Saved',add:'Add window',remove:'Remove',enabled:'Enabled',effectiveFrom:'Effective from',effectiveTo:'Effective to',zone:'Time zone',invalid:'Check dates, times and overlapping windows',limit:'Maximum 56 windows'},
  ru: {title:'Время получения награды',mode:'Режим',always:'Всегда',once:'Разовое окно',weekly:'Еженедельно',start:'Начало',end:'Конец',day:'День недели',from:'С',to:'По',save:'Сохранить',saved:'Сохранено',add:'Добавить интервал',remove:'Удалить',enabled:'Активен',effectiveFrom:'Действует с',effectiveTo:'Действует до',zone:'Часовой пояс',invalid:'Проверьте даты, время и пересечения',limit:'Максимум 56 интервалов'},
}
const days = { uk:['Пн','Вт','Ср','Чт','Пт','Сб','Нд'],de:['Mo','Di','Mi','Do','Fr','Sa','So'],en:['Mon','Tue','Wed','Thu','Fri','Sat','Sun'],ru:['Пн','Вт','Ср','Чт','Пт','Сб','Вс'] }
const isoInstant = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?(?:Z|[+-]\d{2}:\d{2})$/
const defaultWindow = () => ({windowId:'web-'+crypto.randomUUID(),dayOfWeek:1,localStartTime:'16:00',localEndTime:'18:00',effectiveFromDate:null,effectiveToDate:null,enabled:true})
const toForm = item => ({...item,localStartTime:item.localStartTime.slice(0,5),localEndTime:item.localEndTime.slice(0,5)})
const overlaps = (a,b) => a.dayOfWeek===b.dayOfWeek && a.enabled && b.enabled &&
  a.localStartTime<b.localEndTime && b.localStartTime<a.localEndTime &&
  (!a.effectiveToDate || !b.effectiveFromDate || a.effectiveToDate>=b.effectiveFromDate) &&
  (!b.effectiveToDate || !a.effectiveFromDate || b.effectiveToDate>=a.effectiveFromDate)

export default function RewardAvailabilityEditor({ rewardId }) {
  const {i18n}=useTranslation()
  const locale=i18n.language.split('-')[0]
  const t=textByLocale[locale]||textByLocale.en
  const dayNames=days[locale]||days.en
  const {currentUser,getAccessToken}=useAuth()
  const workspaceId=currentUser.workspaceId
  const lock=useRef(false)
  const [data,setData]=useState(null)
  const [mode,setMode]=useState('ALWAYS')
  const [start,setStart]=useState('')
  const [end,setEnd]=useState('')
  const [windows,setWindows]=useState([])
  const [error,setError]=useState('')
  const [notice,setNotice]=useState('')
  const [busy,setBusy]=useState(false)

  useEffect(()=>{
    let active=true
    setData(null);setError('');setNotice('')
    getRewardAvailability(getAccessToken(),workspaceId,rewardId).then(result=>{
      if(!active)return
      setData(result);setMode(result.mode||'ALWAYS')
      setStart(result.startsAt||'');setEnd(result.endsAt||'')
      setWindows((result.weeklyWindows||[]).map(toForm))
    }).catch(e=>{if(active)setError(e.message)})
    return ()=>{active=false}
  },[workspaceId,rewardId,getAccessToken])

  const patch=(id,values)=>setWindows(current=>current.map(w=>w.windowId===id?{...w,...values}:w))
  const save=async event=>{
    event.preventDefault()
    if(lock.current||!data)return
    let payload={mode,startsAt:null,endsAt:null,weeklyWindows:[]}
    if(mode==='ONE_TIME'){
      if(!isoInstant.test(start)||!isoInstant.test(end)||!Number.isFinite(Date.parse(start))||
        !Number.isFinite(Date.parse(end))||Date.parse(start)>=Date.parse(end)){
        setError(t.invalid);return
      }
      payload={...payload,startsAt:start,endsAt:end}
    }
    if(mode==='RECURRING_WEEKLY'){
      if(windows.length>56||windows.some(w=>!Number.isInteger(Number(w.dayOfWeek))||Number(w.dayOfWeek)<1||
        Number(w.dayOfWeek)>7||!/^([01]\d|2[0-3]):[0-5]\d$/.test(w.localStartTime)||
        !/^([01]\d|2[0-3]):[0-5]\d$/.test(w.localEndTime)||w.localStartTime>=w.localEndTime||
        (w.effectiveFromDate&&w.effectiveToDate&&w.effectiveFromDate>w.effectiveToDate))||
        windows.some((w,i)=>windows.slice(i+1).some(other=>overlaps(w,other)))){
        setError(t.invalid);return
      }
      payload.weeklyWindows=windows.map(w=>({
        windowId:w.windowId,dayOfWeek:Number(w.dayOfWeek),localStartTime:w.localStartTime+':00',
        localEndTime:w.localEndTime+':00',effectiveFromDate:w.effectiveFromDate||null,
        effectiveToDate:w.effectiveToDate||null,enabled:Boolean(w.enabled)
      }))
    }
    lock.current=true;setBusy(true);setError('');setNotice('')
    try{
      const saved=await updateRewardAvailability(getAccessToken(),workspaceId,rewardId,payload)
      setData(saved);setMode(saved.mode||mode);setStart(saved.startsAt||'');setEnd(saved.endsAt||'')
      setWindows((saved.weeklyWindows||[]).map(toForm));setNotice(t.saved)
    }catch(e){setError(e.message)}
    finally{lock.current=false;setBusy(false)}
  }

  return <section className="reward-admin-panel">
    <h3>{t.title}</h3>
    {data?.workspaceTimeZone&&<p>{t.zone}: {data.workspaceTimeZone}</p>}
    {error&&<p role="alert" className="error-message">{error}</p>}
    {notice&&<p role="status">{notice}</p>}
    <form className="reward-editor" onSubmit={save}>
      <label>{t.mode}<select disabled={busy||!data} value={mode} onChange={e=>setMode(e.target.value)}>
        <option value="ALWAYS">{t.always}</option><option value="ONE_TIME">{t.once}</option>
        <option value="RECURRING_WEEKLY">{t.weekly}</option></select></label>
      {mode==='ONE_TIME'&&<>
        <label>{t.start}<input type="text" required disabled={busy} value={start} onChange={e=>setStart(e.target.value)} placeholder="2026-11-05T18:00:00+01:00" /></label>
        <label>{t.end}<input type="text" required disabled={busy} value={end} onChange={e=>setEnd(e.target.value)} placeholder="2026-11-05T20:00:00+01:00" /></label>
      </>}
      {mode==='RECURRING_WEEKLY'&&<div style={{display:'grid',gap:12}}>
        {windows.map((w,i)=><fieldset key={w.windowId} disabled={busy} style={{display:'grid',gap:8,border:'1px solid var(--border, #aaa)',borderRadius:12,padding:12,minWidth:0}}>
          <legend>{i+1}</legend>
          <label>{t.day}<select value={w.dayOfWeek} onChange={e=>patch(w.windowId,{dayOfWeek:Number(e.target.value)})}>
            {dayNames.map((name,index)=><option value={index+1} key={name}>{name}</option>)}</select></label>
          <label>{t.from}<input type="time" required value={w.localStartTime} onChange={e=>patch(w.windowId,{localStartTime:e.target.value})}/></label>
          <label>{t.to}<input type="time" required value={w.localEndTime} onChange={e=>patch(w.windowId,{localEndTime:e.target.value})}/></label>
          <label>{t.effectiveFrom}<input type="date" value={w.effectiveFromDate||''} onChange={e=>patch(w.windowId,{effectiveFromDate:e.target.value||null})}/></label>
          <label>{t.effectiveTo}<input type="date" value={w.effectiveToDate||''} onChange={e=>patch(w.windowId,{effectiveToDate:e.target.value||null})}/></label>
          <label><input type="checkbox" checked={w.enabled} onChange={e=>patch(w.windowId,{enabled:e.target.checked})}/>{t.enabled}</label>
          <button type="button" className="secondary-button" onClick={()=>setWindows(list=>list.filter(item=>item.windowId!==w.windowId))}>{t.remove}</button>
        </fieldset>)}
        <button type="button" className="secondary-button" disabled={busy||windows.length>=56} onClick={()=>setWindows(list=>[...list,defaultWindow()])}>{t.add}</button>
        {windows.length>=56&&<small>{t.limit}</small>}
      </div>}
      <button type="submit" className="secondary-button" disabled={busy||!data}>{t.save}</button>
    </form>
  </section>
}
