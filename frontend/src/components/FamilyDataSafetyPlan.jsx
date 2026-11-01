import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { listProtectiveBackups, getProtectiveBackup } from '../api/backupApi'
import { useTranslation } from 'react-i18next'

// Architecture 77d8d2d4: approved product matrix, NOT an executable reset/restore plan.
const dictionary = {
  uk: ['Дані сім’ї: очищення та резервні копії','Очищення','Відновлення з файлу','Повне','Вибіркове','Видалити','Залишити','Категорія','Попередній план. Виконання заблоковано до перевіреної резервної копії, узгодження залежностей і підтвердження Backend.','Завантаження та імпорт зашифрованих файлів стануть доступними після впровадження перевірених API.','Тимчасові копії на сервері зберігаються до 30 днів; завантажений вами файл сервер автоматично видалити не може.','Незмінно захищені: облікові записи, сім’я, доступ власника, інші сім’ї, глобальний каталог і журнал безпеки.','Вибір категорій є лише локальним переглядом. Жодних змін у базі даних.','Відновлення вимагає перевірки цілісності файлу, сумісності та конфліктів.'],
  ru: ['Данные семьи: очистка и резервные копии','Очистка','Восстановление из файла','Полная','Выборочная','Удалить','Сохранить','Категория','Предварительный план. Выполнение заблокировано до подтверждённой резервной копии, проверки зависимостей и одобрения Backend.','Загрузка и импорт зашифрованных файлов станут доступны после готовности проверенных API.','Временные копии на сервере хранятся до 30 дней; скачанный файл сервер автоматически удалить не может.','Всегда защищены: учётные записи, семья, доступ владельца, другие семьи, глобальный каталог и журнал безопасности.','Выбор категорий — только локальный просмотр. База данных не изменяется.','Восстановление требует проверки целостности, совместимости и конфликтов.'],
  de: ['Familiendaten: Zurücksetzen und Sicherung','Zurücksetzen','Aus Datei wiederherstellen','Vollständig','Selektiv','Löschen','Behalten','Kategorie','Nur Vorplanung. Ausführung bleibt gesperrt bis zur verifizierten Sicherung, Abhängigkeitsprüfung und Backend-Freigabe.','Download und Upload verschlüsselter Dateien erst nach Freigabe geprüfter APIs.','Serverkopien werden bis zu 30 Tage aufbewahrt; heruntergeladene Dateien können nicht vom Server gelöscht werden.','Immer geschützt: Konten, Familie, Eigentümerzugang, andere Familien, globaler Katalog und Sicherheitsprotokoll.','Die Auswahl ist lediglich eine lokale Vorschau. Keine Datenbankänderung.','Wiederherstellung erfordert Integritäts-, Kompatibilitäts- und Konfliktprüfung.'],
  en: ['Family data: reset and backups','Reset','Restore from file','Full','Selective','Delete','Retain','Category','Planning preview only. Execution is blocked pending a verified backup, dependency checks and Backend approval.','Encrypted file download and upload become available only after verified APIs are released.','Server-side temporary copies expire after 30 days; downloaded files cannot be remotely deleted.','Always protected: accounts, family identity, owner access, other families, global catalog and security audit.','Category choices are local-only preview. No database changes.','Restore requires file integrity, schema compatibility and conflict checks.'],
}
const entries = [
  ['tasks',true,['Завдання, розклади, виконання та історія','Задачи, расписания, выполнения и история','Aufgaben, Zeitpläne und Verlauf','Tasks, schedules, executions and history']],
  ['rewards',true,['Нагороди, запити та покупки','Награды, запросы и покупки','Belohnungen, Anfragen und Käufe','Rewards, requests and purchases']],
  ['economy',true,['Баланси, бали, репутація та економічна історія','Балансы, баллы, репутация и история','Guthaben, Punkte, Reputation und Verlauf','Balances, points, reputation and history']],
  ['groups',true,['Групи учасників та папки завдань','Группы участников и папки задач','Mitgliedergruppen und Aufgabenordner','Member groups and task folders']],
  ['templates',true,['Приватні шаблони','Частные шаблоны','Private Vorlagen','Private templates']],
  ['media',true,['Медіафайли сім’ї','Медиафайлы семьи','Familienmedien','Family-uploaded media']],
  ['members',false,['Учасники, крім власника','Участники, кроме владельца','Mitglieder außer Eigentümer','Non-owner family participants']],
  ['currencies',false,['Валюти та економічні налаштування','Валюты и настройки экономики','Währungen und Wirtschaftseinstellungen','Currencies and economy configuration']],
  ['settings',false,['Налаштування сім’ї','Настройки семьи','Familieneinstellungen','Family settings']],
]
const indexes = {uk:0,ru:1,de:2,en:3}
export default function FamilyDataSafetyPlan() {
  const { i18n } = useTranslation()
  const language = i18n.language.split('-')[0]
  const w = dictionary[language] || dictionary.en
  const index = indexes[language] ?? 3
  const { currentUser, getAccessToken } = useAuth()
  const workspaceId = currentUser?.workspaceId
  const accountScope = currentUser?.accountId ?? currentUser?.userId ?? currentUser?.id ?? currentUser?.username ?? null
  const canManage = currentUser?.permissions?.includes('MANAGE_WORKSPACE') || currentUser?.permissions?.includes('ADMIN_OVERRIDE')
  const [backups,setBackups] = useState([])
  const [details,setDetails] = useState(null)
  const [detailError,setDetailError] = useState(false)
  const [detailBusy,setDetailBusy] = useState(false)
  const [selectedSnapshot,setSelectedSnapshot] = useState(null)
  const [backupError,setBackupError] = useState(false)
  const [backupLoading,setBackupLoading] = useState(false)
  const [reload,setReload] = useState(0)
  useEffect(() => {
    setBackups([]); setDetails(null); setSelectedSnapshot(null); setBackupError(false); setBackupLoading(false); setDetailBusy(false); setDetailError(false)
    if (!workspaceId || !canManage) return
    let alive = true
    setBackupLoading(true); setBackupError(false); setBackups([]); setDetails(null); setSelectedSnapshot(null); setDetailError(false)
    listProtectiveBackups(getAccessToken(), workspaceId).then(result => {
      if (alive) setBackups(result)
    }).catch(() => { if (alive) setBackupError(true) })
      .finally(() => { if (alive) setBackupLoading(false) })
    return () => { alive = false }
  // AuthProvider supplies a new getAccessToken function on render; do not refetch forever.
  }, [workspaceId, accountScope, canManage, reload])
  useEffect(() => {
    if (!canManage || !workspaceId || !selectedSnapshot) return
    let active = true
    setDetailBusy(true); setDetailError(false); setDetails(null)
    getProtectiveBackup(getAccessToken(),workspaceId,selectedSnapshot)
      .then(result => { if (active) setDetails(result) })
      .catch(() => { if (active) setDetailError(true) })
      .finally(() => { if (active) setDetailBusy(false) })
    return () => { active = false }
  }, [canManage,workspaceId,accountScope,selectedSnapshot])
  const [tab,setTab] = useState('reset')
  const [mode,setMode] = useState('selective')
  const [chosen,setChosen] = useState(() => entries.filter(x=>x[1]).map(x=>x[0]))
  // Selection is only a provisional local proposal: never carry it between families.
  useEffect(() => {
    setTab('reset'); setMode('selective')
    setChosen(entries.filter(x => x[1]).map(x => x[0]))
  }, [workspaceId, accountScope])
  const [restoreMode,setRestoreMode] = useState('selective')
  const [restoreChosen,setRestoreChosen] = useState([])
  const [showSafetyDetails,setShowSafetyDetails] = useState(false)
  const resetCount = mode === 'full' ? entries.length : chosen.length
  const restoreCount = restoreMode === 'full' ? entries.length : restoreChosen.length
  // No upload endpoint currently exists. Categories below are planning-only;
  // archive manifest and compatibility MUST come from validated server preview.
  useEffect(() => { setRestoreChosen([]); setRestoreMode('selective') }, [workspaceId, accountScope])
  if (!canManage || !workspaceId) return null
  return <section className="settings-section" aria-label="Family data safety planning">
    <h2>{w[0]}</h2>
    <div style={{display:'flex',gap:12,flexWrap:'wrap'}}>
      <button type="button" className="secondary-button" aria-pressed={tab==='reset'} onClick={()=>setTab('reset')}>{w[1]}</button>
      <button type="button" className="secondary-button" aria-pressed={tab==='restore'} onClick={()=>setTab('restore')}>{w[2]}</button>
    </div>
    {tab==='reset' ? <>
      <p>{w[8]}</p>
      <fieldset><legend>{w[3]} / {w[4]}</legend>
        <label><input type="radio" name="safety-reset-mode" checked={mode==='full'} onChange={()=>setMode('full')}/>{w[3]}</label>
        <label><input type="radio" name="safety-reset-mode" checked={mode==='selective'} onChange={()=>setMode('selective')}/>{w[4]}</label>
      </fieldset>
      <div style={{maxHeight:340,overflowY:'auto'}}>
        {entries.map(([code,initial,label])=><label key={code} style={{display:'flex',gap:10,padding:'5px 0',alignItems:'center'}}>
          <input type="checkbox" checked={mode==='full'||chosen.includes(code)} disabled={mode==='full'} onChange={()=>setChosen(v=>v.includes(code)?v.filter(c=>c!==code):[...v,code])}/>
          <span>{label[index]} — {(mode==='full'||chosen.includes(code))?w[5]:w[6]}</span>
        </label>)}
      </div>
      <p role="status">{({uk:'У попередньому плані вибрано',ru:'В предварительном плане выбрано',de:'In der Vorplanung ausgewählt',en:'Selected in planning preview'})[language]}: {resetCount} / {entries.length}. {({uk:'Це не серверний план і не дозвіл на видалення.',ru:'Это не серверный план и не разрешение на удаление.',de:'Dies ist weder ein Serverplan noch eine Löschfreigabe.',en:'This is not a server-approved plan or deletion authorization.'})[language]}</p>
      <p>{w[11]}</p><p>{w[12]}</p>
    </> : <>
      <p>{w[9]}</p><p>{w[13]}</p>
      <fieldset><legend>{({uk:'Режим відновлення — лише план',ru:'Режим восстановления — только план',de:'Wiederherstellungsmodus — nur Planung',en:'Restore mode — planning only'})[language]}</legend>
        <label><input type="radio" name="restore-plan-mode" checked={restoreMode==='full'} onChange={()=>setRestoreMode('full')}/>{w[3]}</label>
        <label><input type="radio" name="restore-plan-mode" checked={restoreMode==='selective'} onChange={()=>setRestoreMode('selective')}/>{w[4]}</label>
      </fieldset>
      <p>{({uk:'Ці категорії не підтверджені складом архіву. Після завантаження файлу Backend має перевірити фактичний вміст, сумісність, залежності й конфлікти.',ru:'Категории не подтверждены содержимым архива. Backend должен проверить состав, совместимость, зависимости и конфликты.',de:'Kategorien sind nicht aus einer Archivdatei verifiziert. Backend muss Inhalt, Kompatibilität, Abhängigkeiten und Konflikte prüfen.',en:'Categories are not verified against an archive. Backend must validate actual contents, compatibility, dependencies and conflicts.'})[language]}</p>
      <div style={{maxHeight:280,overflowY:'auto'}}>
        {entries.map(([code,initial,label])=><label key={code} style={{display:'flex',gap:10,padding:'5px 0'}}>
          <input type="checkbox" checked={restoreMode==='full' || restoreChosen.includes(code)} disabled={restoreMode==='full'} onChange={()=>setRestoreChosen(v=>v.includes(code)?v.filter(c=>c!==code):[...v,code])}/>
          <span>{label[index]} — {(restoreMode==='full' || restoreChosen.includes(code))?({uk:'Планується відновити',ru:'Планируется восстановить',de:'Zur Wiederherstellung vorgesehen',en:'Planned for restore'})[language]:w[6]}</span>
        </label>)}
      </div>
      <p>{({uk:'Конфлікти, що блокують застосування: стан невідомий до серверного аналізу файлу. Заміна чи об’єднання даних не дозволяються за замовчуванням.',ru:'Блокирующие конфликты неизвестны до проверки файла сервером. Замена или объединение по умолчанию запрещены.',de:'Blockierende Konflikte sind bis zur serverseitigen Archivprüfung unbekannt. Kein implizites Ersetzen oder Zusammenführen.',en:'Blocking conflicts are unknown until server-side archive inspection. Replacement or merge is forbidden by default.'})[language]}</p>
      <p role="status">{({uk:'Заплановано категорій',ru:'Запланировано категорий',de:'Vorgesehene Kategorien',en:'Planned categories'})[language]}: {restoreCount} / {entries.length}</p>
      <p>{w[11]}</p>
    </>}
    <div>
      <h3>{({uk:'Серверні резервні копії',ru:'Резервные копии на сервере',de:'Server-Sicherungen',en:'Server backup records'})[language] || 'Server backup records'}</h3>
      <p>{({uk:'Це лише реєстр станів. Запис REQUESTED не означає наявність файлу або можливість відновлення.',ru:'Это только реестр статусов. REQUESTED не означает, что файл существует или доступен для восстановления.',de:'Nur Statusregister. REQUESTED bedeutet nicht, dass die Datei existiert oder wiederherstellbar ist.',en:'Metadata only. REQUESTED does not mean the file exists or can be restored.'})[language]}</p>
      <button type="button" className="secondary-button" disabled={backupLoading} onClick={()=>setReload(v=>v+1)}>{({uk:'Оновити список',ru:'Обновить список',de:'Liste aktualisieren',en:'Refresh list'})[language]}</button>
      {backupLoading && <p role="status">{({uk:'Завантаження…',ru:'Загрузка…',de:'Laden…',en:'Loading…'})[language]}</p>}
      {backupError && <p role="alert">{({uk:'Не вдалося отримати реєстр резервних копій.',ru:'Не удалось загрузить реестр резервных копий.',de:'Sicherungsliste konnte nicht geladen werden.',en:'Could not load backup records.'})[language]}</p>}
      {!backupLoading && !backupError && !backups.length && <p>{({uk:'Записів немає.',ru:'Записей нет.',de:'Keine Einträge.',en:'No records.'})[language]}</p>}
      {backups.map(item => <div key={item.snapshotId} style={{padding:'8px 0',borderBottom:'1px solid var(--border-color, #7775)'}}>
        <strong>{item.state === 'VERIFIED' && item.restoreVerified && item.executable ? ({uk:'Перевірено',ru:'Проверено',de:'Verifiziert',en:'Verified'})[language] : item.state === 'EXPIRED' ? ({uk:'Термін минув',ru:'Срок истёк',de:'Abgelaufen',en:'Expired'})[language] : ({uk:'Не підтверджено',ru:'Не подтверждено',de:'Nicht bestätigt',en:'Unverified'})[language]}</strong>
        <span> · {item.resetMode} · {item.snapshotId.slice(0,8)}</span>
        <div>{item.createdAt ? new Date(item.createdAt).toLocaleString(language) : '—'} / {item.expiresAt ? new Date(item.expiresAt).toLocaleString(language) : '—'}</div>
        <p style={{margin:'4px 0'}}>{({uk:'Метадані: наявність файлу та можливість імпорту не підтверджені.',ru:'Метаданные: наличие файла и возможность импорта не подтверждены.',de:'Nur Metadaten: Datei und Importfähigkeit sind nicht bestätigt.',en:'Metadata only: file availability and import capability are not confirmed.'})[language]}</p>
        <small>{item.state} · {item.selectedCategories?.length || 0} {({uk:'категорій',ru:'категорий',de:'Kategorien',en:'categories'})[language]}</small>
        <div><button type="button" className="secondary-button" aria-expanded={selectedSnapshot === item.snapshotId}
          onClick={()=>setSelectedSnapshot(v=>v===item.snapshotId?null:item.snapshotId)}>
          {({uk:'Переглянути запис',ru:'Просмотреть запись',de:'Eintrag ansehen',en:'View metadata'})[language]}
        </button></div>
        {selectedSnapshot === item.snapshotId && <div role="region" style={{padding:'8px 12px'}}>
          {detailBusy && <p role="status">{({uk:'Завантаження…',ru:'Загрузка…',de:'Laden…',en:'Loading…'})[language]}</p>}
          {detailError && <p role="alert">{({uk:'Деталі недоступні.',ru:'Детали недоступны.',de:'Details nicht verfügbar.',en:'Details unavailable.'})[language]}</p>}
          {details && <div>
            <p>{({uk:'Статус',ru:'Статус',de:'Status',en:'Status'})[language]}: {details.state}</p>
            <p>{({uk:'Створено',ru:'Создано',de:'Erstellt',en:'Created'})[language]}: {details.createdAt ? new Date(details.createdAt).toLocaleString(language) : '—'}</p>
            <p>{({uk:'Термін серверного зберігання до',ru:'Срок хранения на сервере до',de:'Server-Aufbewahrung bis',en:'Server retention until'})[language]}: {details.expiresAt ? new Date(details.expiresAt).toLocaleString(language) : '—'}</p>
            <p>{({uk:'Підтверджено відновлення',ru:'Восстановление подтверждено',de:'Wiederherstellung bestätigt',en:'Restore verified'})[language]}: {details.restoreVerified && details.state==='VERIFIED' ? ({uk:'Так (лише статус реєстру)',ru:'Да (только статус реестра)',de:'Ja (nur Registerstatus)',en:'Yes (registry status only)'})[language] : ({uk:'Ні',ru:'Нет',de:'Nein',en:'No'})[language]}</p>
            <ul>{details.selectedCategories.map(code=><li key={code}>{code}</li>)}</ul>
            <p>{({uk:'Цей запис не підтверджує можливість відновлення.',ru:'Запись не подтверждает возможность восстановления.',de:'Dieser Eintrag bestätigt keine Wiederherstellbarkeit.',en:'This record does not prove recoverability.'})[language]}</p>
          </div>}
        </div>}
      </div>)}
    </div>
    <p>{w[10]}</p>
  </section>
}
