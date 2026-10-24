import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getRewardAvailability, updateRewardAvailability } from '../api/rewardApi'

const words={
 uk:{title:'Час отримання винагороди',mode:'Режим',always:'Завжди',once:'Одноразове вікно',weekly:'Щотижня',start:'Початок',end:'Кінець',day:'День тижня (1–7)',from:'Час від',to:'Час до',save:'Зберегти час',saved:'Збережено',error:'Помилка'},
 de:{title:'Belohnungszeitfenster',mode:'Modus',always:'Immer',once:'Einmalig',weekly:'Wöchentlich',start:'Beginn',end:'Ende',day:'Wochentag (1–7)',from:'Von',to:'Bis',save:'Zeitfenster speichern',saved:'Gespeichert',error:'Fehler'},
 en:{title:'Reward availability',mode:'Mode',always:'Always',once:'One-time window',weekly:'Weekly',start:'Start',end:'End',day:'Weekday (1–7)',from:'From',to:'To',save:'Save availability',saved:'Saved',error:'Error'},
 ru:{title:'Время получения награды',mode:'Режим',always:'Всегда',once:'Разовое окно',weekly:'Еженедельно',start:'Начало',end:'Конец',day:'День недели (1–7)',from:'С',to:'По',save:'Сохранить расписание',saved:'Сохранено',error:'Ошибка'}
}
const localDatetime=value=>value?.replace(/([+-]\d\d:\d\d|Z)$/,'').slice(0,16)||''
export default function RewardAvailabilityEditor({ rewardId }) {
  const { i18n }=useTranslation()
  const t=words[i18n.language.split('-')[0]]||words.en
  const {currentUser,getAccessToken}=useAuth()
  const workspaceId=currentUser.workspaceId
  const [data,setData]=useState(null)
  const [mode,setMode]=useState('ALWAYS')
  const [start,setStart]=useState('')
  const [end,setEnd]=useState('')
  const [day,setDay]=useState(1)
  const [from,setFrom]=useState('16:00')
  const [to,setTo]=useState('18:00')
  const [error,setError]=useState('')
  const [notice,setNotice]=useState('')
  const [busy,setBusy]=useState(false)
  useEffect(()=>{
    let active=true
    getRewardAvailability(getAccessToken(),workspaceId,rewardId).then(result=>{
      if(!active)return
      setData(result);setMode(result.mode||'ALWAYS')
      setStart(localDatetime(result.startsAt));setEnd(localDatetime(result.endsAt))
      const first=result.weeklyWindows?.[0]
      if(first){setDay(first.dayOfWeek);setFrom(first.localStartTime?.slice(0,5)||'16:00');setTo(first.localEndTime?.slice(0,5)||'18:00')}
    }).catch(e=>{if(active)setError(e.message)})
    return ()=>{active=false}
  },[getAccessToken,workspaceId,rewardId])
  const save=async e=>{
    e.preventDefault();if(busy)return
    setBusy(true);setError('');setNotice('')
    try{
      let payload={mode,startsAt:null,endsAt:null,weeklyWindows:[]}
      if(mode==='ONE_TIME') {
        if(!start||!end) throw new Error('Invalid date/time range')
        if (!/^\d{4}-\d\d-\d\dT\d\d:\d\d(?::\d\d)?(?:Z|[+-]\d\d:\d\d)$/.test(start) ||
            !/^\d{4}-\d\d-\d\dT\d\d:\d\d(?::\d\d)?(?:Z|[+-]\d\d:\d\d)$/.test(end) ||
            !Number.isFinite(Date.parse(start)) || !Number.isFinite(Date.parse(end)) || Date.parse(start)>=Date.parse(end)) throw new Error('Use ISO date-time with UTC offset, e.g. 2026-11-05T18:00:00+01:00')
        payload={...payload,startsAt:start,endsAt:end}
      }
      if(mode==='RECURRING_WEEKLY'){
        if(from>=to)throw new Error('Start must be before end')
        payload.weeklyWindows=[{windowId:data?.weeklyWindows?.[0]?.windowId||'web-1',dayOfWeek:Number(day),localStartTime:`${from}:00`,localEndTime:`${to}:00`,effectiveFromDate:data?.weeklyWindows?.[0]?.effectiveFromDate||null,effectiveToDate:data?.weeklyWindows?.[0]?.effectiveToDate||null,enabled:true},...(data?.weeklyWindows||[]).slice(1)]
      }
      const saved=await updateRewardAvailability(getAccessToken(),workspaceId,rewardId,payload)
      setData(saved);setNotice(t.saved)
    }catch(e){setError(e.message)}finally{setBusy(false)}
  }
  return <section className="reward-admin-panel">
    <h3>{t.title}</h3>
    {error&&<p className="error-message" role="alert">{error}</p>}
    {notice&&<p role="status">{notice}</p>}
    <form onSubmit={save} className="reward-editor">
      <label>{t.mode}<select value={mode} onChange={e=>setMode(e.target.value)}><option value="ALWAYS">{t.always}</option><option value="ONE_TIME">{t.once}</option><option value="RECURRING_WEEKLY">{t.weekly}</option></select></label>
      {mode==='ONE_TIME'&&<><label>{t.start}<input type="text" placeholder="2026-11-05T18:00:00+01:00" value={start} onChange={e=>setStart(e.target.value)} required/></label><label>{t.end}<input type="text" placeholder="2026-11-05T20:00:00+01:00" value={end} onChange={e=>setEnd(e.target.value)} required/></label></>}
      {mode==='RECURRING_WEEKLY'&&<><label>{t.day}<input type="number" min="1" max="7" value={day} onChange={e=>setDay(e.target.value)}/></label><label>{t.from}<input type="time" value={from} onChange={e=>setFrom(e.target.value)}/></label><label>{t.to}<input type="time" value={to} onChange={e=>setTo(e.target.value)}/></label></>}
      <button className="secondary-button" type="submit" disabled={busy||!data}>{t.save}</button>
    </form>
  </section>
}
