import { useMemo } from 'react'

const config = {
  TASK: [['title','text'],['description','text'],['mandatory','checkbox'],['recurrenceType','select:ONCE,DAILY,WEEKLY,MONTHLY,CUSTOM'],['recurrenceDayOfWeek','number'],['recurrenceDayOfMonth','number'],['dueTime','time'],['startDate','date'],['assignedMemberId','number'],['rewardAmount','number'],['penaltyAmount','number'],['rewardPointTypeId','number'],['penaltyPointTypeId','number']],
  REWARD: [['title','text'],['description','text'],['rewardKind','select:STANDARD,TIME_BASED'],['defaultDurationMinutes','number'],['priceAmount','number'],['pointTypeId','number'],['requiresApproval','checkbox'],['acquisitionMode','select:DIRECT,REQUEST,DIRECT_OR_REQUEST'],['minimumReputation','number'],['cooldownSeconds','number']],
}
const labels = {
  uk: {title:'Назва',description:'Опис',mandatory:'Обов’язкове',recurrenceType:'Повторення',recurrenceDayOfWeek:'День тижня',recurrenceDayOfMonth:'День місяця',dueTime:'Час',startDate:'Дата початку',assignedMemberId:'ID учасника',rewardAmount:'Винагорода',penaltyAmount:'Штраф',rewardPointTypeId:'ID валюти винагороди',penaltyPointTypeId:'ID валюти штрафу',rewardKind:'Тип нагороди',defaultDurationMinutes:'Тривалість, хв',priceAmount:'Ціна',pointTypeId:'ID валюти',requiresApproval:'Потрібне погодження',acquisitionMode:'Спосіб отримання',minimumReputation:'Мінімальна репутація',cooldownSeconds:'Пауза, с'},
  en: {title:'Title',description:'Description',mandatory:'Mandatory',recurrenceType:'Recurrence',recurrenceDayOfWeek:'Weekday',recurrenceDayOfMonth:'Month day',dueTime:'Due time',startDate:'Start date',assignedMemberId:'Member ID',rewardAmount:'Reward amount',penaltyAmount:'Penalty amount',rewardPointTypeId:'Reward currency ID',penaltyPointTypeId:'Penalty currency ID',rewardKind:'Reward kind',defaultDurationMinutes:'Duration, min',priceAmount:'Price',pointTypeId:'Currency ID',requiresApproval:'Approval required',acquisitionMode:'Acquisition',minimumReputation:'Minimum reputation',cooldownSeconds:'Cooldown, sec'},
  ru: {title:'Название',description:'Описание',mandatory:'Обязательно',recurrenceType:'Повторение',recurrenceDayOfWeek:'День недели',recurrenceDayOfMonth:'День месяца',dueTime:'Время',startDate:'Дата начала',assignedMemberId:'ID участника',rewardAmount:'Награда',penaltyAmount:'Штраф',rewardPointTypeId:'ID валюты награды',penaltyPointTypeId:'ID валюты штрафа',rewardKind:'Тип награды',defaultDurationMinutes:'Длительность, мин',priceAmount:'Цена',pointTypeId:'ID валюты',requiresApproval:'Нужно одобрение',acquisitionMode:'Способ получения',minimumReputation:'Минимальная репутация',cooldownSeconds:'Пауза, с'},
  de: {title:'Titel',description:'Beschreibung',mandatory:'Pflicht',recurrenceType:'Wiederholung',recurrenceDayOfWeek:'Wochentag',recurrenceDayOfMonth:'Monatstag',dueTime:'Zeit',startDate:'Startdatum',assignedMemberId:'Mitglied-ID',rewardAmount:'Belohnung',penaltyAmount:'Strafe',rewardPointTypeId:'Belohnungswährung-ID',penaltyPointTypeId:'Strafwährung-ID',rewardKind:'Belohnungsart',defaultDurationMinutes:'Dauer, Min.',priceAmount:'Preis',pointTypeId:'Währung-ID',requiresApproval:'Genehmigung',acquisitionMode:'Einlöseart',minimumReputation:'Mindest-Reputation',cooldownSeconds:'Wartezeit, Sek.'},
}
export default function TemplateDraftFieldsEditor({kind, value, onChange, language='uk', disabled=false}) {
  const draft = useMemo(() => { try {const v=JSON.parse(value); return v && typeof v==='object' && !Array.isArray(v)?v:null} catch {return null}}, [value])
  if (!draft || !config[kind]) return null
  const names=labels[language] || labels.en
  const set = (key,next) => onChange(JSON.stringify({...draft,[key]:next},null,2))
  return <div className="currency-forms-fields">
    {config[kind].filter(([key])=>Object.hasOwn(draft,key)).map(([key,type])=><label key={key}>
      <span>{names[key]||key}</span>
      {type==='checkbox' ? <input type="checkbox" checked={draft[key]===true} disabled={disabled} onChange={e=>set(key,e.target.checked)}/> :
        type.startsWith('select:') ? <select disabled={disabled} value={draft[key]??''} onChange={e=>set(key,e.target.value||null)}>
          <option value="">—</option>{type.slice(7).split(',').map(item=><option key={item} value={item}>{item.replaceAll('_',' ')}</option>)}
          {draft[key] && !type.slice(7).split(',').includes(draft[key]) && <option value={draft[key]}>{draft[key]}</option>}
        </select> :
        <input type={type} disabled={disabled} value={draft[key]??''} min={type==='number' && /(Minutes|Seconds|TypeId|MemberId)$/.test(key)?0:undefined}
          onChange={e=>{const raw=e.target.value;if(type!=='number')return set(key,raw||null);if(!raw)return set(key,null);const number=Number(raw);if(Number.isFinite(number)&&(!/(Minutes|Seconds|TypeId|MemberId)$/.test(key)||number>=0))set(key,number)}}/>}
    </label>)}
  </div>
}
