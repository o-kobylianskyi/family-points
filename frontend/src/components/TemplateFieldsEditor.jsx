import { useMemo } from 'react'

const fields = {
  TASK: [
    ['description', 'text'], ['mandatory', 'boolean'], ['recurrenceType', 'text'],
    ['recurrenceDayOfWeek', 'number'], ['recurrenceDayOfMonth', 'number'], ['dueTime', 'time'], ['rewardCurrencyCode', 'text'], ['rewardAmount', 'number'],
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
    ['availabilityStartsAt', 'text'], ['availabilityEndsAt', 'text'],
  ],
}
const enumValues = {
  rewardKind: ['STANDARD', 'TIME_BASED'],
  acquisitionMode: ['DIRECT', 'REQUEST', 'DIRECT_OR_REQUEST'],
  recurrenceType: ['ONCE', 'DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM'],
  availabilityMode: ['ALWAYS', 'ONE_TIME', 'RECURRING_WEEKLY'],
}
const nonNegative = new Set([
  'priceAmount', 'durationMinutes', 'cooldownSeconds', 'minimumReputation',
  'memberTotalLimit', 'memberDailyLimit', 'memberWeeklyLimit',
  'workspaceTotalLimit', 'workspaceDailyLimit', 'workspaceWeeklyLimit',
])
const wholeNumbers = new Set([
  'durationMinutes', 'cooldownSeconds', 'memberTotalLimit', 'memberDailyLimit',
  'memberWeeklyLimit', 'workspaceTotalLimit', 'workspaceDailyLimit', 'workspaceWeeklyLimit',
])
const bounded = { recurrenceDayOfWeek: [1, 7], recurrenceDayOfMonth: [1, 31] }
const names = {
  ru: {description:'Описание',mandatory:'Обязательно',recurrenceType:'Повторение',dueTime:'Время выполнения',rewardCurrencyCode:'Валюта награды',rewardAmount:'Начисление',penaltyCurrencyCode:'Валюта штрафа',penaltyAmount:'Штраф',rewardReputation:'Репутация (+)',penaltyReputation:'Репутация (−)',currencyCode:'Валюта',priceAmount:'Цена',rewardKind:'Тип награды',durationMinutes:'Длительность, мин',acquisitionMode:'Способ получения',requiresApproval:'Нужно одобрение',minimumReputation:'Минимальная репутация',cooldownSeconds:'Пауза, с',memberTotalLimit:'Лимит участника',memberDailyLimit:'В день (участник)',memberWeeklyLimit:'В неделю (участник)',workspaceTotalLimit:'Лимит семьи',workspaceDailyLimit:'В день (семья)',workspaceWeeklyLimit:'В неделю (семья)',extraRequestAllowed:'Дополнительный запрос',availabilityMode:'Доступность',recurrenceDayOfWeek:'День недели (1–7)',recurrenceDayOfMonth:'День месяца (1–31)',availabilityStartsAt:'Доступна с (ISO)',availabilityEndsAt:'Доступна до (ISO)'},
  de: {description:'Beschreibung',mandatory:'Verpflichtend',recurrenceType:'Wiederholung',dueTime:'Fälligkeitszeit',rewardCurrencyCode:'Belohnungswährung',rewardAmount:'Gutschrift',penaltyCurrencyCode:'Strafwährung',penaltyAmount:'Strafbetrag',rewardReputation:'Reputation (+)',penaltyReputation:'Reputation (−)',currencyCode:'Währung',priceAmount:'Preis',rewardKind:'Belohnungsart',durationMinutes:'Dauer, Min.',acquisitionMode:'Einlöseart',requiresApproval:'Genehmigung erforderlich',minimumReputation:'Mindestreputation',cooldownSeconds:'Wartezeit, Sek.',memberTotalLimit:'Limit pro Mitglied',memberDailyLimit:'Tageslimit pro Mitglied',memberWeeklyLimit:'Wochenlimit pro Mitglied',workspaceTotalLimit:'Limit pro Familie',workspaceDailyLimit:'Tageslimit pro Familie',workspaceWeeklyLimit:'Wochenlimit pro Familie',extraRequestAllowed:'Zusätzliche Anfrage erlaubt',availabilityMode:'Verfügbarkeit',recurrenceDayOfWeek:'Wochentag (1–7)',recurrenceDayOfMonth:'Monatstag (1–31)',availabilityStartsAt:'Verfügbar ab (ISO)',availabilityEndsAt:'Verfügbar bis (ISO)'},
  uk: { description:'Опис',mandatory:'Обов’язкове',recurrenceType:'Повторення',dueTime:'Час виконання',rewardCurrencyCode:'Валюта нагороди',rewardAmount:'Нарахування',penaltyCurrencyCode:'Валюта штрафу',penaltyAmount:'Штраф',rewardReputation:'Репутація (+)',penaltyReputation:'Репутація (−)',currencyCode:'Валюта',priceAmount:'Ціна',rewardKind:'Тип нагороди',durationMinutes:'Тривалість, хв',acquisitionMode:'Спосіб отримання',requiresApproval:'Потрібне погодження',minimumReputation:'Мінімальна репутація',cooldownSeconds:'Пауза, с',memberTotalLimit:'Ліміт учасника',memberDailyLimit:'На день (учасник)',memberWeeklyLimit:'На тиждень (учасник)',workspaceTotalLimit:'Ліміт сім’ї',workspaceDailyLimit:'На день (сім’я)',workspaceWeeklyLimit:'На тиждень (сім’я)',extraRequestAllowed:'Додатковий запит',availabilityMode:'Доступність',recurrenceDayOfWeek:'День тижня (1–7)',recurrenceDayOfMonth:'День місяця (1–31)',availabilityStartsAt:'Доступна з (ISO)',availabilityEndsAt:'Доступна до (ISO)' },
  en: {description:'Description',mandatory:'Required',recurrenceType:'Recurrence',dueTime:'Due time',rewardCurrencyCode:'Reward currency',rewardAmount:'Award amount',penaltyCurrencyCode:'Penalty currency',penaltyAmount:'Penalty amount',rewardReputation:'Reputation (+)',penaltyReputation:'Reputation (−)',currencyCode:'Currency',priceAmount:'Price',rewardKind:'Reward kind',durationMinutes:'Duration, min',acquisitionMode:'Acquisition mode',requiresApproval:'Approval required',minimumReputation:'Minimum reputation',cooldownSeconds:'Cooldown, s',memberTotalLimit:'Member total limit',memberDailyLimit:'Member daily limit',memberWeeklyLimit:'Member weekly limit',workspaceTotalLimit:'Workspace total limit',workspaceDailyLimit:'Workspace daily limit',workspaceWeeklyLimit:'Workspace weekly limit',extraRequestAllowed:'Allow extra requests',availabilityMode:'Availability',recurrenceDayOfWeek:'Day of week (1–7)',recurrenceDayOfMonth:'Day of month (1–31)',availabilityStartsAt:'Available from (ISO)',availabilityEndsAt:'Available until (ISO)'}
}
export default function TemplateFieldsEditor({ kind, definition, onChange, language='uk', disabled=false }) {
  const object = useMemo(() => { try { const value=JSON.parse(definition); return value && !Array.isArray(value) && typeof value==='object' ? value : null } catch {return null} },[definition])
  const safeText = value => typeof value === 'string' ? value : value == null ? '' : typeof value === 'object' ? (value[language] || value.uk || value.en || Object.values(value).find(v => typeof v === 'string') || '') : String(value)
  if (!object || !fields[kind]) return null
  const labels = names[language] || names.en
  const setField = (key,value) => onChange(JSON.stringify({ ...object, [key]: value }, null, 2))
  return <div className="currency-forms-fields">
    {fields[kind].filter(([key]) => Object.hasOwn(object,key)).map(([key,type]) =>
      <label key={key}><span>{labels[key] || key}</span>
        {type === 'boolean'
          ? <input type="checkbox" checked={object[key] === true} disabled={disabled} onChange={e => setField(key, e.target.checked)}/>
          : enumValues[key] && (object[key] == null || enumValues[key].includes(object[key]))
            ? <select value={type === 'number' ? (object[key] ?? '') : safeText(object[key])} disabled={disabled} onChange={e => setField(key, e.target.value || null)}>
                {object[key] == null && <option value="">—</option>}
                {enumValues[key].map(value => <option key={value} value={value}>{value.replaceAll('_', ' ')}</option>)}
              </select>
            : <input type={type} step={type === 'number' ? wholeNumbers.has(key) ? 1 : 'any' : undefined}
                min={type === 'number' ? (bounded[key]?.[0] ?? (nonNegative.has(key) ? 0 : undefined)) : undefined}
                max={type === 'number' ? bounded[key]?.[1] : undefined}
                value={object[key] ?? ''} disabled={disabled}
                onChange={e => {
                  if (type !== 'number') return setField(key, typeof object[key] === 'object' && object[key] !== null ? {...object[key], [language]: e.target.value} : (e.target.value || null))
                  if (e.target.value === '') return setField(key, null)
                  const value = Number(e.target.value)
                  if (!Number.isFinite(value) || nonNegative.has(key) && value < 0 || bounded[key] && (value < bounded[key][0] || value > bounded[key][1] || !Number.isInteger(value)) || wholeNumbers.has(key) && !Number.isInteger(value)) return
                  setField(key, value)
                }}/>}
      </label>)}
  </div>
}
