import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import './FamilyResetReadiness.css'
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

const serverMessages = {
  "TASK_DEFINITIONS requires TASK_EXECUTIONS and TASK_SCHEDULE_SLOTS": [
    "Для очищення завдань також виберіть їх виконання та розклад.",
    "Для очистки задач также выберите выполнения и расписание.",
    "Aufgaben erfordern auch Ausführungen und Zeitpläne.",
    "Task definitions require executions and schedule slots."
  ],
  "REWARD_DEFINITIONS requires REWARD_REQUESTS and REWARD_PURCHASES": [
    "Для очищення нагород також виберіть запити й отримання нагород.",
    "Для очистки наград также выберите запросы и получения наград.",
    "Belohnungen erfordern auch Anfragen und Käufe.",
    "Reward definitions require requests and purchases."
  ],
  "MEDIA_ASSETS requires MEDIA_BINDINGS": [
    "Для очищення зображень також виберіть прив’язки зображень.",
    "Для очистки изображений также выберите привязки изображений.",
    "Medien erfordern auch Bildzuordnungen.",
    "Media assets require media bindings."
  ],
  "Group and work hierarchy references must be reviewed before reset": [
    "Перед очищенням груп потрібно перевірити пов’язані робочі вузли.",
    "Перед очисткой групп нужно проверить связанные рабочие узлы.",
    "Vor dem Zurücksetzen von Gruppen muss die Aufgabenstruktur geprüft werden.",
    "Group and work hierarchy references must be reviewed."
  ],
  "Execution is disabled until dependent FK graph and tenant-safe deletion order are approved": [
    "Очищення заблоковано до перевірки залежностей бази даних і безпечного порядку видалення.",
    "Очистка заблокирована до проверки связей базы данных и безопасного порядка удаления.",
    "Zurücksetzen gesperrt, bis Datenbankabhängigkeiten und sichere Löschreihenfolge genehmigt sind.",
    "Execution blocked until database dependencies and safe deletion order are approved."
  ],
  "Audit, confirmation and idempotency records are not implemented": [
    "Безпечне підтвердження й перевірки журналу операцій ще не завершено.",
    "Безопасное подтверждение и проверки журнала операций ещё не завершены.",
    "Bestätigung, Audit und wiederholungssichere Verarbeitung sind noch nicht fertig.",
    "Audit, confirmation and idempotency safeguards are incomplete."
  ],
  "Offline operation generation fencing and ledger reconciliation are not implemented": [
    "Захист від старих офлайн-операцій і узгодження історії балів ще не завершено.",
    "Защита от старых офлайн-операций и согласование истории баллов ещё не завершены.",
    "Schutz vor alten Offline-Vorgängen und Abgleich des Punktekontos fehlen noch.",
    "Offline replay fencing and ledger reconciliation are incomplete."
  ],
  "Activation request references need coordinated review": [
    "Потрібно врахувати посилання запитів активації.",
    "Необходимо учесть ссылки запросов активации.",
    "Aktivierungsreferenzen müssen geprüft werden.",
    "Activation request references require review."
  ],
  "Executions, schedules, approvals, history and ledger dependencies not yet resolved": [
    "Залежності виконань, розкладів, погоджень та історії ще не узгоджені.",
    "Зависимости выполнений, расписаний, согласований и истории ещё не согласованы.",
    "Abhängigkeiten von Ausführungen, Zeitplänen und Verlauf sind ungeklärt.",
    "Executions, schedules, approvals and ledger dependencies remain unresolved."
  ],
  "Purchases, requests, time sessions and point ledger dependencies not yet resolved": [
    "Покупки, запити, часові сесії та історія балів залежать від нагород.",
    "Покупки, запросы, временные сессии и история баллов зависят от наград.",
    "Käufe, Anfragen, Zeitsitzungen und Punkteverlauf hängen davon ab.",
    "Purchases, requests, timed sessions and ledger dependencies remain unresolved."
  ],
  "Task audit, participants and rewards must be accounted for": [
    "Слід врахувати історію завдань, учасників і нагороди.",
    "Нужно учесть историю задач, участников и награды.",
    "Aufgabenverlauf, Teilnehmer und Belohnungen berücksichtigen.",
    "Task history, participants and rewards must be accounted for."
  ],
  "Approvals, obligations and purchases must be reviewed": [
    "Потрібна перевірка погоджень, зобов’язань і покупок.",
    "Нужно проверить согласования, обязательства и покупки.",
    "Freigaben, Verpflichtungen und Käufe müssen geprüft werden.",
    "Approvals, obligations and purchases must be reviewed."
  ],
  "Historical spending and obligation dependencies are protected": [
    "Історичні витрати та зобов’язання захищені.",
    "Исторические расходы и обязательства защищены.",
    "Historische Ausgaben und Verpflichtungen sind geschützt.",
    "Historical spending and obligations are protected."
  ],
  "Immutable point transaction history is protected": [
    "Незмінна історія операцій з балами захищена.",
    "Неизменяемая история баллов защищена.",
    "Unveränderlicher Punkteverlauf ist geschützt.",
    "Immutable point transaction history is protected."
  ],
  "Immutable reputation transaction history is protected": [
    "Незмінна історія репутації захищена.",
    "Неизменяемая история репутации защищена.",
    "Unveränderlicher Reputationsverlauf ist geschützt.",
    "Immutable reputation history is protected."
  ],
  "Memberships, nested groups, permissions and group balances require coordinated cleanup": [
    "Учасники, вкладені групи, права та баланси потребують узгодженого очищення.",
    "Участники, вложенные группы, права и балансы требуют согласованной очистки.",
    "Mitglieder, Untergruppen, Rechte und Salden müssen abgestimmt werden.",
    "Memberships, nested groups, permissions and balances require coordinated cleanup."
  ],
  "Task hierarchy and descendant references must be preserved until dependency plan approved": [
    "Ієрархію завдань і зв’язки потрібно зберегти до погодження плану.",
    "Иерархию задач и связи нужно сохранить до утверждения плана.",
    "Aufgabenhierarchie und Verweise müssen bis zur Freigabe erhalten bleiben.",
    "Task hierarchy and references must be preserved until approval."
  ],
  "Scheduled occurrences and task history depend on these slots": [
    "Від цих слотів залежать заплановані виконання та історія завдань.",
    "От этих слотов зависят запланированные выполнения и история задач.",
    "Geplante Ausführungen und Verlauf hängen von diesen Zeitfenstern ab.",
    "Scheduled occurrences and task history depend on these slots."
  ],
  "Media blobs and bindings require coordinated removal; global catalog images unaffected": [
    "Файли та прив’язки слід очищати узгоджено; глобальні іконки збережуться.",
    "Файлы и привязки нужно очищать согласованно; глобальные иконки сохранятся.",
    "Dateien und Bindungen müssen gemeinsam bereinigt werden; globale Bilder bleiben.",
    "Media files and bindings require coordinated removal; global images remain."
  ],
  "Member/group/task image references must be reconciled": [
    "Прив’язки зображень учасників, груп і завдань потребують перевірки.",
    "Привязки изображений участников, групп и задач требуют проверки.",
    "Bildverweise von Mitgliedern, Gruppen und Aufgaben müssen geprüft werden.",
    "Member, group and task image references must be reconciled."
  ]
}
const localizedServerMessage = (message, language) => serverMessages[message]?.[localeIndex[language] ?? 3] || ({uk:'Додаткова умова безпеки сервера. Очищення недоступне.',ru:'Дополнительное ограничение безопасности сервера. Очистка недоступна.',de:'Zusätzliche Sicherheitsbeschränkung des Servers. Zurücksetzen nicht verfügbar.',en:'Additional server safety restriction. Reset unavailable.'})[language] || 'Reset unavailable.'

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
  const selectionRevision = useRef(0)
  const workspaceRef = useRef(workspaceId)
  if (workspaceRef.current !== workspaceId) { workspaceRef.current = workspaceId; generation.current += 1 }

  const load = async (id, requestId) => {
    selectionRevision.current += 1
    setBusy(true);setError(null);setPreview(null);setPlan(null);setSelected([])
    try {
      const result = await getResetPreview(getAccessToken(), id)
      if (generation.current === requestId && workspaceRef.current === id) {
        if (String(result?.workspaceId) !== String(id) || result?.executionEnabled !== false) throw new Error('RESET_PREVIEW_INVALID')
        setPreview(result)
      }
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
    const id = workspaceId, requestId = ++generation.current, selectionId = selectionRevision.current
    const codes = [...selected]
    setBusy(true);setPlan(null);setError(null)
    try {
      const response = await planReset(getAccessToken(), id, codes)
      if (generation.current === requestId && workspaceRef.current === id && selectionRevision.current === selectionId) {
        if (String(response?.workspaceId) !== String(id) || response?.executable !== false || !Array.isArray(response?.blockers)) throw new Error('RESET_PLAN_INVALID')
        setPlan(response)
      }
    } catch (e) {
      if (generation.current === requestId && workspaceRef.current === id && selectionRevision.current === selectionId) setError(e)
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
              selectionRevision.current += 1
              setPlan(null)
              setSelected(old=>e.target.checked?[...old,code]:old.filter(item=>item!==code))
            }}/>
            <span><strong>{categories[code]?.[localeIndex[language]??3]||code}</strong>
              <small>{words[13]}: {record.recordCount}</small>
              {record.warning && <small>{localizedServerMessage(record.warning, language)}</small>}</span>
          </label>
        })}
      </div>
      <p>{words[2]}: {selected.length}</p>
      <button type="button" className="secondary-button" disabled={busy || selected.length===0} onClick={check}>{busy?words[3]:words[4]}</button>
      {plan && <div role="status"><p>{words[5]}</p>
        {!!plan.blockers?.length && <><strong>{words[12]}</strong><ul>{plan.blockers.map((blocker,i)=><li key={i}>{localizedServerMessage(blocker, language)}</li>)}</ul></>}
      </div>}
    </>}
  </section>
}
