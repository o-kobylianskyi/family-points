import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import LanguageSwitcher from '../components/LanguageSwitcher'
import './RegisterFamilyPage.css'
import '../components/AndroidDownloadLink.css'

export default function RegisterFamilyPage() {
  const { t } = useTranslation()
  const SAMPLE = {
    familyName: t('familyDemoUi.sampleFamily'),
    ownerName: t('familyDemoUi.sampleOwner'),
    members: [
      { id: 1, name: t('familyDemoUi.sampleChild8'), type: 'CHILD', age: '8' },
      { id: 2, name: t('familyDemoUi.sampleChild4'), type: 'CHILD', age: '4' },
    ], pointsPerTask: 20, dailyLimit: 90, starterTasks: true,
  }
  const EXAMPLE_TASKS = [
    { name: t('familyDemoUi.sampleTaskClean'), points: 20 },
    { name: t('familyDemoUi.sampleTaskHomework'), points: 20 },
    { name: t('familyDemoUi.sampleTaskHelp'), points: 10 },
  ]
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
      setError(t('familyDemoUi.errorFamily'))
      return
    }
    if (step === 1 && draft.members.some((member) => !member.name.trim() ||
      (member.type === 'CHILD' && (!member.age || Number(member.age) < 0 || Number(member.age) > 17)))) {
      setError(t('familyDemoUi.errorMembers'))
      return
    }
    setError('')
    setStep((previous) => Math.min(previous + 1, 3))
  }
  const steps = [t('familyDemoUi.stepFamily'), t('familyDemoUi.stepMembers'), t('familyDemoUi.stepEconomy'), t('familyDemoUi.stepReview')]
  return (
    <div className="family-wizard-page">
      <div className="family-wizard">
        <div className="family-wizard-top">
          <div className="family-wizard-heading">
            <span className="family-wizard-logo">FP</span>
            <div><h1>FamilyPoints</h1><p>{t('familyDemoUi.wizard')}</p></div>
          </div>
          <LanguageSwitcher />
        </div>
        <div className="family-wizard-notice" role="status">
          <strong>{t('familyDemoUi.demo')}</strong>
          <span>{t('familyDemoUi.demoNotice')}</span>
          <button type="button" className="family-wizard-secondary"
            onClick={() => { setDraft({ ...SAMPLE, members: SAMPLE.members.map((m) => ({ ...m })) }); setStep(0); setFinished(false); setError('') }}>
            {t('familyDemoUi.fillSample')}
          </button>
        </div>
        <nav className="family-wizard-steps" aria-label={t("familyDemoUi.steps")}>
          {steps.map((label, index) => (
            <button type="button" key={label} aria-current={index === step ? 'step' : undefined}
              className={index === step ? 'selected' : ''} onClick={() => { setStep(index); setError('') }}>
              <span>{index + 1}</span>{label}
            </button>
          ))}
        </nav>
        <div className="family-wizard-main">
          {step === 0 && <section>
            <h2>{t('familyDemoUi.aboutFamily')}</h2>
            <p className="family-wizard-hint">{t('familyDemoUi.familyHelp')}</p>
            <label>{t('familyDemoUi.familyName')}<input maxLength={100} value={draft.familyName}
              placeholder={t("familyDemoUi.familyPlaceholder")} onChange={(e) => change('familyName', e.target.value)} /></label>
            <label>{t('familyDemoUi.owner')}<input maxLength={100} value={draft.ownerName}
              placeholder={t("familyDemoUi.yourName")} onChange={(e) => change('ownerName', e.target.value)} /></label>
            <p className="family-wizard-hint">{t('familyDemoUi.authNotice')}</p>
          </section>}
          {step === 1 && <section>
            <h2>{t('familyDemoUi.composition')}</h2>
            <p className="family-wizard-hint">{t('familyDemoUi.compositionHelp')}</p>
            {draft.members.map((member) => <div className="family-wizard-member" key={member.id}>
              <label>{t('familyDemoUi.name')}<input maxLength={100} value={member.name} onChange={(e) => patchMember(member.id, 'name', e.target.value)} /></label>
              <label>{t('familyDemoUi.type')}<select value={member.type} onChange={(e) => patchMember(member.id, 'type', e.target.value)}>
                <option value="CHILD">{t('familyDemoUi.child')}</option><option value="PARENT">{t('familyDemoUi.parent')}</option><option value="OTHER">{t('familyDemoUi.other')}</option>
              </select></label>
              {member.type === 'CHILD' && <label>{t('familyDemoUi.age')}<input type="number" min="0" max="17"
                value={member.age} onChange={(e) => patchMember(member.id, 'age', e.target.value)} /></label>}
              <button type="button" className="family-wizard-secondary" onClick={() =>
                change('members', draft.members.filter((m) => m.id !== member.id))}>{t('familyDemoUi.remove')}</button>
            </div>)}
            <button type="button" className="family-wizard-secondary" onClick={addMember}>{t('familyDemoUi.addMember')}</button>
          </section>}
          {step === 2 && <section>
            <h2>{t('familyDemoUi.starter')}</h2>
            <p className="family-wizard-hint">{t('familyDemoUi.starterHelp')}</p>
            <label>{t('familyDemoUi.rewardPerTask', { value: draft.pointsPerTask })}
              <input type="range" min="5" max="100" step="5" value={draft.pointsPerTask}
                onChange={(e) => change('pointsPerTask', Number(e.target.value))} /></label>
            <label>{t('familyDemoUi.screenLimit', { value: draft.dailyLimit })}
              <input type="range" min="0" max="180" step="15" value={draft.dailyLimit}
                onChange={(e) => change('dailyLimit', Number(e.target.value))} /></label>
            <label className="family-wizard-checkbox"><input type="checkbox" checked={draft.starterTasks}
              onChange={(e) => change('starterTasks', e.target.checked)} />
              {t('familyDemoUi.showStarter')}</label>
            {draft.starterTasks && <div className="family-wizard-task-list">
              {EXAMPLE_TASKS.map((task) => <div key={task.name}>
                <span>{task.name}</span><strong>+{t('familyDemoUi.points', { value: Math.round(task.points * draft.pointsPerTask / 20) })}</strong>
              </div>)}
            </div>}
          </section>}
          {step === 3 && <section>
            <h2>{t('familyDemoUi.preview')}</h2>
            <p className="family-wizard-hint">{t('familyDemoUi.previewHelp')}</p>
            <div className="family-wizard-summary">
              <div><span>{t('familyDemoUi.stepFamily')}</span><strong>{draft.familyName || t('familyDemoUi.notSet')}</strong></div>
              <div><span>{t('familyDemoUi.admin')}</span><strong>{draft.ownerName || t('familyDemoUi.notSet')}</strong></div>
              <div><span>{t('familyDemoUi.extraMembers')}</span><strong>{draft.members.length}</strong></div>
              <div><span>{t('familyDemoUi.pointsSystem')}</span><strong>POINTS</strong></div>
              <div><span>{t('familyDemoUi.screenTime')}</span><strong>{t('familyDemoUi.minutesPerDay', { value: draft.dailyLimit })}</strong></div>
              <div><span>{t('familyDemoUi.taskTemplates')}</span><strong>{draft.starterTasks ? t('familyDemoUi.enabled') : t('familyDemoUi.skipped')}</strong></div>
            </div>
            {draft.members.length > 0 && <div className="family-wizard-roster">
              {draft.members.map((member) => <span key={member.id}>{member.name || t('familyDemoUi.unnamed')} · {member.type === 'CHILD' ? t('familyDemoUi.child') : member.type === 'PARENT' ? t('familyDemoUi.parent') : t('familyDemoUi.other')}</span>)}
            </div>}
            {finished ? <div className="family-wizard-done" role="status">
              {t('familyDemoUi.finished')}
            </div> : <p className="family-wizard-hint">{t('familyDemoUi.demoHelp')}</p>}
          </section>}
        </div>
        {error && <div className="family-wizard-error" role="alert">{error}</div>}
        <div className="family-wizard-actions">
          <button type="button" className="family-wizard-secondary" disabled={step === 0}
            onClick={() => { setStep((previous) => previous - 1); setError('') }}>{t('familyDemoUi.back')}</button>
          {step < 3 ? <button type="button" onClick={next}>{t('familyDemoUi.next')}</button> :
            <button type="button" onClick={() => setFinished(true)}>{t('familyDemoUi.finishDemo')}</button>}
        </div>
        <p className="family-wizard-footer"><Link to="/login">{t('familyDemoUi.login')}</Link></p>
        <Link className="android-download-cta" to="/download">📱 {t('androidDownload.button')}</Link>
      </div>
    </div>
  )
}
