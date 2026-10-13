import { useState } from 'react'
import { Link } from 'react-router-dom'
import LanguageSwitcher from '../components/LanguageSwitcher'
import './RegisterFamilyPage.css'

const SAMPLE = {
  familyName: 'Демонстраційна сім’я',
  ownerName: 'Тестовий дорослий',
  members: [
    { id: 1, name: 'Учасник 8 років', type: 'CHILD', age: '8' },
    { id: 2, name: 'Учасник 4 роки', type: 'CHILD', age: '4' },
  ],
  pointsPerTask: 20,
  dailyLimit: 90,
  starterTasks: true,
}
const EXAMPLE_TASKS = [
  { name: 'Прибрати свою кімнату', points: 20 },
  { name: 'Зробити домашнє завдання', points: 20 },
  { name: 'Допомогти вдома', points: 10 },
]

export default function RegisterFamilyPage() {
  // Preview-only: no API requests, JWT, storage, or mutations to a real family.
  const [step, setStep] = useState(0)
  const [draft, setDraft] = useState(() => ({
    familyName: '', ownerName: '', members: [],
    pointsPerTask: 20, dailyLimit: 90, starterTasks: true,
  }))
  const [error, setError] = useState('')
  const [finished, setFinished] = useState(false)

  const change = (name, value) => {
    setDraft((previous) => ({ ...previous, [name]: value }))
    setError('')
    setFinished(false)
  }
  const patchMember = (id, field, value) => {
    change('members', draft.members.map((m) => m.id === id ? { ...m, [field]: value } : m))
  }
  const addMember = () => change('members', [...draft.members, {
    id: Math.max(0, ...draft.members.map((m) => m.id)) + 1,
    name: '', type: 'CHILD', age: '',
  }])
  const next = () => {
    if (step === 0 && (!draft.familyName.trim() || !draft.ownerName.trim())) {
      setError('Заповніть назву сім’ї та ім’я адміністратора.')
      return
    }
    if (step === 1 && draft.members.some((member) => !member.name.trim() ||
      (member.type === 'CHILD' && (!member.age || Number(member.age) < 0 || Number(member.age) > 17)))) {
      setError('Перевірте імена та вік дітей (0–17 років).')
      return
    }
    setError('')
    setStep((previous) => Math.min(previous + 1, 3))
  }
  const steps = ['Сім’я', 'Учасники', 'Економіка', 'Підсумок']
  return (
    <div className="family-wizard-page">
      <div className="family-wizard">
        <div className="family-wizard-top">
          <div className="family-wizard-heading">
            <span className="family-wizard-logo">FP</span>
            <div><h1>FamilyPoints</h1><p>Майстер створення сім’ї</p></div>
          </div>
          <LanguageSwitcher />
        </div>
        <div className="family-wizard-notice" role="status">
          <strong>Демонстраційний режим</strong>
          <span>Дані залишаються лише на цій сторінці. Акаунти й сім’ї не створюються.</span>
          <button type="button" className="family-wizard-secondary"
            onClick={() => { setDraft({ ...SAMPLE, members: SAMPLE.members.map((m) => ({ ...m })) }); setStep(0); setFinished(false); setError('') }}>
            Заповнити приклад
          </button>
        </div>
        <nav className="family-wizard-steps" aria-label="Етапи налаштування">
          {steps.map((label, index) => (
            <button type="button" key={label} aria-current={index === step ? 'step' : undefined}
              className={index === step ? 'selected' : ''} onClick={() => { setStep(index); setError('') }}>
              <span>{index + 1}</span>{label}
            </button>
          ))}
        </nav>
        <div className="family-wizard-main">
          {step === 0 && <section>
            <h2>Розкажіть про сім’ю</h2>
            <p className="family-wizard-hint">Спочатку створюється сім’я та ваш профіль адміністратора.</p>
            <label>Назва сім’ї<input maxLength={100} value={draft.familyName}
              placeholder="Наприклад: Родина" onChange={(e) => change('familyName', e.target.value)} /></label>
            <label>Ім’я адміністратора<input maxLength={100} value={draft.ownerName}
              placeholder="Ваше ім’я" onChange={(e) => change('ownerName', e.target.value)} /></label>
            <p className="family-wizard-hint">Авторизація, пароль і Google Sign-In підключатимуться окремо після DEV-перевірок.</p>
          </section>}
          {step === 1 && <section>
            <h2>Склад сім’ї</h2>
            <p className="family-wizard-hint">Додайте тестових дорослих або дітей. Можна пропустити цей крок.</p>
            {draft.members.map((member) => <div className="family-wizard-member" key={member.id}>
              <label>Ім’я<input maxLength={100} value={member.name} onChange={(e) => patchMember(member.id, 'name', e.target.value)} /></label>
              <label>Тип<select value={member.type} onChange={(e) => patchMember(member.id, 'type', e.target.value)}>
                <option value="CHILD">Дитина</option><option value="PARENT">Дорослий</option><option value="OTHER">Інший</option>
              </select></label>
              {member.type === 'CHILD' && <label>Вік<input type="number" min="0" max="17"
                value={member.age} onChange={(e) => patchMember(member.id, 'age', e.target.value)} /></label>}
              <button type="button" className="family-wizard-secondary" onClick={() =>
                change('members', draft.members.filter((m) => m.id !== member.id))}>Прибрати</button>
            </div>)}
            <button type="button" className="family-wizard-secondary" onClick={addMember}>+ Додати учасника</button>
          </section>}
          {step === 2 && <section>
            <h2>Стартові налаштування</h2>
            <p className="family-wizard-hint">У прикладі використовується локальна економіка сім’ї — POINTS.</p>
            <label>Орієнтовна винагорода за завдання: {draft.pointsPerTask} балів
              <input type="range" min="5" max="100" step="5" value={draft.pointsPerTask}
                onChange={(e) => change('pointsPerTask', Number(e.target.value))} /></label>
            <label>Орієнтовний ліміт екранного часу: {draft.dailyLimit} хвилин
              <input type="range" min="0" max="180" step="15" value={draft.dailyLimit}
                onChange={(e) => change('dailyLimit', Number(e.target.value))} /></label>
            <label className="family-wizard-checkbox"><input type="checkbox" checked={draft.starterTasks}
              onChange={(e) => change('starterTasks', e.target.checked)} />
              Показати приклади початкових завдань</label>
            {draft.starterTasks && <div className="family-wizard-task-list">
              {EXAMPLE_TASKS.map((task) => <div key={task.name}>
                <span>{task.name}</span><strong>+{Math.round(task.points * draft.pointsPerTask / 20)} балів</strong>
              </div>)}
            </div>}
          </section>}
          {step === 3 && <section>
            <h2>Попередній перегляд</h2>
            <p className="family-wizard-hint">Так може виглядати нова сім’я після майбутньої реєстрації.</p>
            <div className="family-wizard-summary">
              <div><span>Сім’я</span><strong>{draft.familyName || 'Не вказано'}</strong></div>
              <div><span>Адміністратор</span><strong>{draft.ownerName || 'Не вказано'}</strong></div>
              <div><span>Додаткові учасники</span><strong>{draft.members.length}</strong></div>
              <div><span>Балова система</span><strong>POINTS</strong></div>
              <div><span>Екранний час</span><strong>{draft.dailyLimit} хв/день (приклад)</strong></div>
              <div><span>Шаблони завдань</span><strong>{draft.starterTasks ? 'Увімкнено' : 'Пропущено'}</strong></div>
            </div>
            {draft.members.length > 0 && <div className="family-wizard-roster">
              {draft.members.map((member) => <span key={member.id}>{member.name || 'Без імені'} · {member.type === 'CHILD' ? 'Дитина' : member.type === 'PARENT' ? 'Дорослий' : 'Інший'}</span>)}
            </div>}
            {finished ? <div className="family-wizard-done" role="status">
              Демопроходження завершено. Дані не відправлялися на сервер і не зберігалися.
            </div> : <p className="family-wizard-hint">Це лише попередній перегляд. Натискання нижче не створить акаунт.</p>}
          </section>}
        </div>
        {error && <div className="family-wizard-error" role="alert">{error}</div>}
        <div className="family-wizard-actions">
          <button type="button" className="family-wizard-secondary" disabled={step === 0}
            onClick={() => { setStep((previous) => previous - 1); setError('') }}>Назад</button>
          {step < 3 ? <button type="button" onClick={next}>Далі</button> :
            <button type="button" onClick={() => setFinished(true)}>Завершити демо</button>}
        </div>
        <p className="family-wizard-footer"><Link to="/login">Повернутися до входу</Link></p>
      </div>
    </div>
  )
}
