import { useMemo } from 'react'

const fields = {
  TASK: [
    ['description', 'text'], ['mandatory', 'boolean'], ['recurrenceType', 'text'],
    ['dueTime', 'time'], ['rewardCurrencyCode', 'text'], ['rewardAmount', 'number'],
    ['penaltyCurrencyCode', 'text'], ['penaltyAmount', 'number'],
    ['rewardReputation', 'number'], ['penaltyReputation', 'number'],
  ],
  REWARD: [
    ['description', 'text'], ['currencyCode', 'text'], ['priceAmount', 'number'],
    ['rewardKind', 'text'], ['durationMinutes', 'number'], ['acquisitionMode', 'text'],
    ['requiresApproval', 'boolean'], ['minimumReputation', 'number'], ['cooldownSeconds', 'number'],
    ['memberTotalLimit', 'number'], ['memberDailyLimit', 'number'], ['memberWeeklyLimit', 'number'],
    ['workspaceTotalLimit', 'number'], ['workspaceDailyLimit', 'number'], ['workspaceWeeklyLimit', 'number'],
    ['extraRequestAllowed', 'boolean'], ['availabilityMode', 'text'],
  ],
}
const names = {
  uk: { description:'Опис',mandatory:'Обов’язкове',recurrenceType:'Повторення',dueTime:'Час виконання',rewardCurrencyCode:'Валюта нагороди',rewardAmount:'Нарахування',penaltyCurrencyCode:'Валюта штрафу',penaltyAmount:'Штраф',rewardReputation:'Репутація (+)',penaltyReputation:'Репутація (−)',currencyCode:'Валюта',priceAmount:'Ціна',rewardKind:'Тип нагороди',durationMinutes:'Тривалість, хв',acquisitionMode:'Спосіб отримання',requiresApproval:'Потрібне погодження',minimumReputation:'Мінімальна репутація',cooldownSeconds:'Пауза, с',memberTotalLimit:'Ліміт учасника',memberDailyLimit:'На день (учасник)',memberWeeklyLimit:'На тиждень (учасник)',workspaceTotalLimit:'Ліміт сім’ї',workspaceDailyLimit:'На день (сім’я)',workspaceWeeklyLimit:'На тиждень (сім’я)',extraRequestAllowed:'Додатковий запит',availabilityMode:'Доступність' },
  en: {description:'Description',mandatory:'Required',recurrenceType:'Recurrence',dueTime:'Due time',rewardCurrencyCode:'Reward currency',rewardAmount:'Award amount',penaltyCurrencyCode:'Penalty currency',penaltyAmount:'Penalty amount',rewardReputation:'Reputation (+)',penaltyReputation:'Reputation (−)',currencyCode:'Currency',priceAmount:'Price',rewardKind:'Reward kind',durationMinutes:'Duration, min',acquisitionMode:'Acquisition mode',requiresApproval:'Approval required',minimumReputation:'Minimum reputation',cooldownSeconds:'Cooldown, s',memberTotalLimit:'Member total limit',memberDailyLimit:'Member daily limit',memberWeeklyLimit:'Member weekly limit',workspaceTotalLimit:'Workspace total limit',workspaceDailyLimit:'Workspace daily limit',workspaceWeeklyLimit:'Workspace weekly limit',extraRequestAllowed:'Allow extra requests',availabilityMode:'Availability'}
}
export default function TemplateFieldsEditor({ kind, definition, onChange, language='uk', disabled=false }) {
  const object = useMemo(() => { try { const value=JSON.parse(definition); return value && !Array.isArray(value) && typeof value==='object' ? value : null } catch {return null} },[definition])
  if (!object || !fields[kind]) return null
  const labels = names[language] || names.en
  const setField = (key,value) => onChange(JSON.stringify({ ...object, [key]: value }, null, 2))
  return <div className="currency-forms-fields">
    {fields[kind].filter(([key]) => Object.hasOwn(object,key)).map(([key,type]) =>
      <label key={key}><span>{labels[key] || key}</span>
        {type==='boolean' ? <input type="checkbox" checked={Boolean(object[key])} disabled={disabled} onChange={e=>setField(key,e.target.checked)}/>
          : <input type={type} step={type==='number'?'any':undefined} value={object[key] ?? ''} disabled={disabled}
            onChange={e=>setField(key,e.target.value===''?null:type==='number'?Number(e.target.value):e.target.value)}/>}
      </label>)}
  </div>
}
