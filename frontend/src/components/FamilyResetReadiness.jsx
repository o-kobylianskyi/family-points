import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getResetPreview, planReset } from '../api/resetApi'

const categories = {
  PRIVATE_TEMPLATES: ['Приватні шаблони','Частные шаблоны','Private Vorlagen','Private templates'],
  TASK_DEFINITIONS: ['Завдання','Задачи','Aufgaben','Task definitions'],
  REWARD_DEFINITIONS: ['Нагороди','Награды','Belohnungen','Reward definitions'],
  TASK_EXECUTIONS: ['Виконання завдань','Выполнения задач','Aufgabenausführungen','Task executions'],
  REWARD_REQUESTS: ['Запити нагород','Запросы наград','Belohnungsanfragen','Reward requests'],
  REWARD_PURCHASES: ['Отримані нагороди','Полученные награды','Belohnungskäufe','Reward purchases'],
  POINT_LEDGER: ['Історія балів','История баллов','Punkteverlauf','Points ledger'],
  REPUTATION_LEDGER: ['Історія репутації','История репутации','Reputationsverlauf','Reputation ledger'],
  MEMBER_GROUPS: ['Групи учасників','Группы участников','Mitgliedergruppen','Member groups'],
  WORK_NODES: ['Робочі вузли','Рабочие узлы','Aufgabenstruktur','Work nodes'],
  TASK_SCHEDULE_SLOTS: ['Розклад завдань','Расписание задач','Aufgabenzeitplan','Task schedule slots'],
  MEDIA_ASSETS: ['Завантажені зображення','Загруженные изображения','Hochgeladene Bilder','Media assets'],
  MEDIA_BINDINGS: ['Прив’язки зображень','Привязки изображений','Bildzuordnungen','Media bindings']
}
const ui = {
  uk: ['Вибіркове очищення сім’ї','Попередній перегляд даних','Вибрано','Завантаження…','Перевірити залежності','План перевірено: очищення ще недоступне','Захищено: облікові записи, сім’я, учасники та інші сім’ї.','Сервер поки забороняє виконання. Жодні дані не видаляються.','Не вдалося завантажити дані.','Немає доступу до перегляду.','Завантажити знову','Виберіть категорії для попередньої перевірки.','Залежності та блокувальні умови:','Записів'],
  ru: ['Выборочная очистка семьи','Предпросмотр данных','Выбрано','Загрузка…','Проверить зависимости','План проверен: очистка пока недоступна','Защищено: учётные записи, семья, участники и другие семьи.','Сервер пока запрещает выполнение. Данные не удаляются.','Не удалось загрузить данные.','Нет доступа к просмотру.','Обновить','Выберите категории для предварительной проверки.','Зависимости и ограничения:','Записей'],
  de: ['Selektives Zurücksetzen der Familie','Datenvorschau','Ausgewählt','Wird geladen…','Abhängigkeiten prüfen','Plan geprüft: Zurücksetzen noch nicht verfügbar','Geschützt: Konto, Familie, Mitglieder und andere Familien.','Der Server erlaubt noch keine Ausführung. Es werden keine Daten gelöscht.','Daten konnten nicht geladen werden.','Keine Berechtigung für die Vorschau.','Erneut laden','Kategorien für die Vorschau auswählen.','Abhängigkeiten und Sperren:','Datensätze'],
  en: ['Selective family reset','Data preview','Selected','Loading…','Check dependencies','Plan checked: reset remains unavailable','Protected: account, family, members and other families.','Server execution is still disabled. No data is deleted.','Could not load data.','Preview access denied.','Reload','Select categories for preliminary review.','Dependencies and blockers:','Records']
}
const localeIndex = {uk:0,ru:1,de:2,en:3}
export default function FamilyResetReadiness() {
  const { i18n } = useTranslation()
  const { currentUser, getAccessToken } = useAuth()
  const language = i18n.language.split('-')[0]
  const words = ui[language] || ui.en
  const workspaceId = currentUser?.workspaceId
  const [preview,setPreview] = useState(null)
  const [selected,setSelected] = useState([])
  const [plan,setPlan] = useState(null)
  const [error,setError] = useState(null)
  const [busy,setBusy] = useState(false)
  const generation = useRef(0)
  const workspaceRef = useRef(workspaceId)
  if (workspaceRef.current !== workspaceId) { workspaceRef.current = workspaceId; generation.current += 1 }

  const load = async (id, requestId) => {
    setBusy(true);setError(null);setPreview(null);setPlan(null);setSelected([])
    try {
      const result = await getResetPreview(getAccessToken(), id)
      if (generation.current === requestId && workspaceRef.current === id) setPreview(result)
    } catch(e) {
      if (generation.current === requestId && workspaceRef.current === id) setError(e)
    } finally {
      if (generation.current === requestId && workspaceRef.current === id) setBusy(false)
    }
  }
  useEffect(() => {
    if (!workspaceId) return
    const requestId = ++generation.current
    load(workspaceId, requestId)
    return () => { generation.current += 1 }
  }, [workspaceId, getAccessToken])

  const check = async () => {
    const id = workspaceId, requestId = ++generation.current
    setBusy(true);setPlan(null);setError(null)
    try {
      const response = await planReset(getAccessToken(), id, selected)
      if (generation.current === requestId && workspaceRef.current === id) setPlan(response)
    } catch (e) {
      if (generation.current === requestId && workspaceRef.current === id) setError(e)
    } finally {
      if (generation.current === requestId && workspaceRef.current === id) setBusy(false)
    }
  }
  const keys = Object.keys(preview?.categories || {})
  return <section className="settings-section family-reset-readiness" aria-labelledby="family-reset-heading">
    <h2 id="family-reset-heading">{words[0]}</h2>
    <p>{words[6]}</p>
    <p role="status">{words[7]}</p>
    <div className="modal-actions"><button type="button" className="secondary-button" disabled={busy} onClick={()=>load(workspaceId, ++generation.current)}>{busy?words[3]:words[10]}</button></div>
    {error && <p className="error-message" role="alert">{error.status===403?words[9]:words[8]}</p>}
    {preview && <><h3>{words[1]}</h3>
      <p>{words[11]}</p>
      <div className="family-reset-category-grid">
        {keys.map(code => {
          const record = preview.categories[code]
          return <label key={code} className="family-reset-category">
            <input type="checkbox" disabled={busy} checked={selected.includes(code)} onChange={e=>{
              setPlan(null)
              setSelected(old=>e.target.checked?[...old,code]:old.filter(item=>item!==code))
            }}/>
            <span><strong>{categories[code]?.[localeIndex[language]??3]||code}</strong>
              <small>{words[13]}: {record.recordCount}</small></span>
          </label>
        })}
      </div>
      <p>{words[2]}: {selected.length}</p>
      <button type="button" className="secondary-button" disabled={busy || selected.length===0} onClick={check}>{busy?words[3]:words[4]}</button>
      {plan && <div role="status"><p>{words[5]}</p>
        {!!plan.blockers?.length && <><strong>{words[12]}</strong><ul>{plan.blockers.map((blocker,i)=><li key={i}>{blocker}</li>)}</ul></>}
      </div>}
    </>}
  </section>
}
